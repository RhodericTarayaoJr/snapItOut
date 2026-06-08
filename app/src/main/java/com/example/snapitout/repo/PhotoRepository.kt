package com.example.snapitout.repo

import android.content.Context
import com.example.snapitout.cloud.CloudinaryUploader
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume

class PhotoRepository private constructor(context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val http = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------- WRITE SIDE ----------

    /** Call right after a file has been saved locally. Uploads in the background (non-blocking). */
    fun enqueueSave(file: File, userId: String) {
        scope.launch {
            try {
                uploadAndRecord(file, userId)
            } catch (_: Exception) {
                // ignored on purpose — syncWithCloud() will retry on next AlbumActivity open
            }
        }
    }

    /** Call when a local image is deleted, so it won't sync back down from other devices. */
    fun enqueueDelete(userId: String, fileName: String) {
        scope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("photos").document(sanitize(fileName))
                    .delete()
            } catch (_: Exception) { }
        }
    }

    private suspend fun uploadAndRecord(file: File, userId: String) {
        if (!file.exists()) return
        val url = CloudinaryUploader.upload(file)
        val data = hashMapOf(
            "userId" to userId,
            "fileName" to file.name,
            "cloudUrl" to url,
            "createdAt" to file.lastModified()
        )
        firestore.collection("users").document(userId)
            .collection("photos").document(sanitize(file.name))
            .set(data)
    }

    // ---------- SYNC (call on AlbumActivity load) ----------

    /**
     * Two-way sync:
     *  - uploads any local file not yet on the cloud  (retry / safety net)
     *  - downloads any cloud file missing locally       (cross-device)
     * Returns true if anything new was downloaded, so the gallery can refresh.
     */
    suspend fun syncWithCloud(userId: String, albumFolder: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!albumFolder.exists()) albumFolder.mkdirs()

            val localFiles = albumFolder.listFiles()
                ?.filter { it.isFile }
                ?.associateBy { it.name }
                ?: emptyMap()

            val cloud = fetchCloudPhotos(userId) // fileName -> url

            // upload local files the cloud doesn't have yet
            localFiles.values.forEach { f ->
                if (!cloud.containsKey(f.name)) {
                    try { uploadAndRecord(f, userId) } catch (_: Exception) { }
                }
            }

            // download cloud files we don't have locally
            var downloadedSomething = false
            cloud.forEach { (name, url) ->
                if (!localFiles.containsKey(name)) {
                    try {
                        downloadTo(url, File(albumFolder, name))
                        downloadedSomething = true
                    } catch (_: Exception) { }
                }
            }
            downloadedSomething
        }

    private suspend fun fetchCloudPhotos(userId: String): Map<String, String> =
        suspendCancellableCoroutine { cont ->
            firestore.collection("users").document(userId)
                .collection("photos")
                .get()
                .addOnSuccessListener { snap ->
                    val map = snap.documents.mapNotNull { d ->
                        val name = d.getString("fileName")
                        val url = d.getString("cloudUrl")
                        if (name != null && url != null) name to url else null
                    }.toMap()
                    cont.resume(map)
                }
                .addOnFailureListener { cont.resume(emptyMap()) }
        }

    private fun downloadTo(url: String, target: File) {
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download failed: ${resp.code}")
            resp.body?.byteStream()?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        }
    }

    private fun sanitize(name: String): String = name.replace("/", "_")

    companion object {
        @Volatile private var INSTANCE: PhotoRepository? = null
        fun get(context: Context): PhotoRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhotoRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
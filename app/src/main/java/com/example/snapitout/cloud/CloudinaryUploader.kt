package com.example.snapitout.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

object CloudinaryUploader {

    // 🔧 SET THESE TWO VALUES FROM YOUR CLOUDINARY DASHBOARD
    private const val CLOUD_NAME = "dmkjuoff8"
    private const val UPLOAD_PRESET = "snapItOut"

    private val client = OkHttpClient()

    /** Uploads a local image file to Cloudinary and returns its secure HTTPS URL. */
    suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        val endpoint = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Cloudinary upload failed (${response.code}): $bodyText")
            }
            JSONObject(bodyText).getString("secure_url")
        }
    }
}
package com.example.snapitout.utils

import android.content.Context
import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow
import kotlin.math.sqrt

object CollageUtils {

    /** 🔥 PHOTO MOSAIC (FIXED — NO RIGHT/BOTTOM GAPS) */
    fun createPhotoMosaic(
        context: Context,
        mainImage: Bitmap,
        tiles: List<Bitmap>
    ): Bitmap? {

        if (tiles.isEmpty()) return null

        val width = 1080
        val height = 1080

        val scaledMain = Bitmap.createScaledBitmap(mainImage, width, height, true)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val gridSize = 50
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Precompute tile average colors
        val tileData = tiles.map { tile ->
            tile to getAverageColor(tile)
        }

        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {

                // ✅ FIX: perfect edge coverage (no leftover pixels)
                val left = (x * width) / gridSize
                val top = (y * height) / gridSize
                val right = ((x + 1) * width) / gridSize
                val bottom = ((y + 1) * height) / gridSize

                val cellWidth = right - left
                val cellHeight = bottom - top

                val targetColor = getAverageColorFromRegion(
                    scaledMain,
                    left,
                    top,
                    cellWidth,
                    cellHeight
                )

                val bestTile = tileData.minByOrNull {
                    colorDistance(it.second, targetColor)
                }!!.first

                val scaledTile = Bitmap.createScaledBitmap(
                    bestTile,
                    cellWidth,
                    cellHeight,
                    true
                )

                canvas.drawBitmap(
                    scaledTile,
                    left.toFloat(),
                    top.toFloat(),
                    paint
                )
            }
        }

        // subtle overlay for better blending
        val overlayPaint = Paint().apply {
            alpha = 80
        }

        canvas.drawBitmap(scaledMain, 0f, 0f, overlayPaint)

        return result
    }

    /** 🎯 Average color of full bitmap */
    private fun getAverageColor(bitmap: Bitmap): Int {
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0

        for (x in 0 until bitmap.width step 5) {
            for (y in 0 until bitmap.height step 5) {
                val pixel = bitmap.getPixel(x, y)
                r += Color.red(pixel)
                g += Color.green(pixel)
                b += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) return Color.BLACK

        return Color.rgb(
            (r / count).toInt(),
            (g / count).toInt(),
            (b / count).toInt()
        )
    }

    /** 🔥 Average color of region */
    private fun getAverageColorFromRegion(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): Int {

        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0

        val step = 3

        for (x in startX until (startX + width) step step) {
            for (y in startY until (startY + height) step step) {

                val px = x.coerceAtMost(bitmap.width - 1)
                val py = y.coerceAtMost(bitmap.height - 1)

                val pixel = bitmap.getPixel(px, py)

                r += Color.red(pixel)
                g += Color.green(pixel)
                b += Color.blue(pixel)
                count++
            }
        }

        if (count == 0) return Color.BLACK

        return Color.rgb(
            (r / count).toInt(),
            (g / count).toInt(),
            (b / count).toInt()
        )
    }

    /** 🎯 Color distance matching */
    private fun colorDistance(c1: Int, c2: Int): Double {
        val r = Color.red(c1) - Color.red(c2)
        val g = Color.green(c1) - Color.green(c2)
        val b = Color.blue(c1) - Color.blue(c2)

        return sqrt(
            r.toDouble().pow(2) +
                    g.toDouble().pow(2) +
                    b.toDouble().pow(2)
        )
    }

    /** 💾 Save bitmap */
    fun saveBitmapToAlbum(bitmap: Bitmap, folder: File): File? {
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, "SnapIt_Mosaic_${System.currentTimeMillis()}.jpg")

        return try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
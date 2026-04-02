package com.fiorenmas.cleansociallink.history.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Executors

object HistoryImageLoader {
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 5000
    private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    private const val MAX_BITMAP_DIMENSION_PX = 2048

    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cache = object : LruCache<String, Bitmap>(40) {}

    fun loadInto(imageView: ImageView, url: String) {
        imageView.tag = url

        val cached = cache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        imageView.setImageResource(android.R.drawable.ic_menu_report_image)
        executor.execute {
            val bitmap = downloadBitmap(url) ?: return@execute
            cache.put(url, bitmap)
            mainHandler.post {
                if (imageView.tag == url) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return HistoryNetworkClient.get(
            url = url,
            accept = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
            connectTimeoutMs = CONNECT_TIMEOUT_MS,
            readTimeoutMs = READ_TIMEOUT_MS
        ) { connection ->
            connection.inputStream.use { stream ->
                decodeBitmapSafely(stream)
            }
        }
    }

    private fun decodeBitmapSafely(stream: InputStream): Bitmap? {
        val bytes = readImageBytes(stream) ?: return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_BITMAP_DIMENSION_PX)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun readImageBytes(stream: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalBytes = 0

        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            totalBytes += read
            if (totalBytes > MAX_IMAGE_BYTES) {
                return null
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var currentWidth = width
        var currentHeight = height

        while (currentWidth > maxDimension || currentHeight > maxDimension) {
            sample *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sample.coerceAtLeast(1)
    }
}


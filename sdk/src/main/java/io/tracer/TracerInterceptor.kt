package io.tracer

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import okio.InflaterSource
import java.io.EOFException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater

class TracerInterceptor : Interceptor {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 1. Guard: Prevent infinite loops by ignoring our own reporting traffic
        if (request.header("X-Tracer-Internal") != null) {
            return chain.proceed(request)
        }

        android.util.Log.d("Tracer", "Intercepting request: ${request.url}")
        val startNs = System.nanoTime()

        val requestBodyString = try {
            val copy = request.newBuilder().build()
            val buffer = Buffer()
            copy.body?.writeTo(buffer)
            // Limit request logging too (optional but good practice)
            val byteCount = buffer.size.coerceAtMost(65536)
            if (isProbablyUtf8(buffer)) {
                buffer.readString(byteCount, Charset.forName("UTF-8"))
            } else {
                "[Binary Request Body]"
            }
        } catch (e: Exception) {
            ""
        }

        val requestHeaders = request.headers.toMap()

        val response: Response
        try {
            // Force Gzip/Deflate to ensure we can decode the response (we don't support Brotli yet)
            val newRequest = request.newBuilder()
                .header("Accept-Encoding", "gzip, deflate")
                .build()
            response = chain.proceed(newRequest)
        } catch (e: Exception) {
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBodyString = try {
            val contentType = response.header("Content-Type")?.lowercase(Locale.US) ?: ""
            val encoding = response.header("Content-Encoding")?.lowercase(Locale.US) ?: ""

            if (contentType.contains("image") || 
                contentType.contains("audio") || 
                contentType.contains("video") || 
                contentType.contains("application/octet-stream") ||
                contentType.contains("application/protobuf") ||
                contentType.contains("application/x-protobuf")) {
                "[Binary Data: $contentType]"
            } else {
                // Use peekBody to safely buffer up to 2MB without consuming the response
                val peekingBody = response.peekBody(2097152)
                val peekedSource = peekingBody.source()
                val resultBuffer = Buffer()

                when (encoding) {
                    "gzip" -> {
                        val gzipSource = GzipSource(peekedSource)
                        resultBuffer.writeAll(gzipSource)
                        gzipSource.close()
                    }
                    "deflate" -> {
                        val inflaterSource = InflaterSource(peekedSource, Inflater(true))
                        resultBuffer.writeAll(inflaterSource)
                        inflaterSource.close()
                    }
                    else -> {
                        resultBuffer.writeAll(peekedSource)
                    }
                }

                if (isProbablyUtf8(resultBuffer)) {
                    resultBuffer.readString(Charset.forName("UTF-8"))
                } else {
                    "[Binary or Encrypted Data]"
                }
            }
        } catch (e: OutOfMemoryError) {
            "(Response too large to display - OOM avoided)"
        } catch (e: Exception) {
            "(Body omitted: ${e.message})"
        }

        val responseHeaders = response.headers.toMap()

        val transaction = ApiTransaction(
            id = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
            timestamp = dateFormat.format(Date()),
            method = request.method,
            url = request.url.toString(),
            requestHeaders = requestHeaders,
            requestBody = requestBodyString,
            responseStatus = response.code,
            responseTime = "${tookMs}ms",
            responseHeaders = responseHeaders,
            responseBody = responseBodyString
        )

        Tracer.report(transaction)

        return response
    }

    private fun isProbablyUtf8(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = buffer.size.coerceAtMost(64)
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (e: EOFException) {
            return false
        }
    }
}

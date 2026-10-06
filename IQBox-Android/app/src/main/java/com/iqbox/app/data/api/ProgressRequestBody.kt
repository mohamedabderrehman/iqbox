package com.iqbox.app.data.api

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

/**
 * A RequestBody wrapper that reports real upload progress via a callback.
 * Reports: bytesWritten, totalBytes, speed (bytes/sec)
 */
class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, totalBytes: Long, speedBytesPerSec: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        val countingSink = CountingForwardingSink(sink, totalBytes, onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class CountingForwardingSink(
        delegate: Sink,
        private val totalBytes: Long,
        private val onProgress: (Long, Long, Long) -> Unit
    ) : ForwardingSink(delegate) {

        private var bytesWritten = 0L
        private var lastReportTime = System.currentTimeMillis()
        private var lastReportBytes = 0L
        private var currentSpeed = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            reportProgress()
        }

        private fun reportProgress() {
            val now = System.currentTimeMillis()
            val elapsed = now - lastReportTime

            if (elapsed >= 150 || bytesWritten >= totalBytes) {
                if (elapsed > 0) {
                    val bytesSinceLast = bytesWritten - lastReportBytes
                    currentSpeed = (bytesSinceLast * 1000) / elapsed
                    lastReportTime = now
                    lastReportBytes = bytesWritten
                }
                onProgress(bytesWritten, totalBytes, currentSpeed)
            }
        }
    }
}

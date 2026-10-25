package com.example.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Lets the player open WAV files whose header has a wrong "block size" (bytes per sample frame).
 *
 * Some apps write e.g. 2 instead of 4 for 16-bit stereo. The audio itself is fine and most players
 * ignore the value, but Media3 checks it strictly and refuses the file ("Expected block size: 4;
 * got: 2"). This data source reads the first bytes of a file when it is opened from the start and,
 * only for plain WAV (PCM, float, A-law, µ-law) with an inconsistent value, corrects that one
 * number before the player sees it. Every other file passes through untouched.
 */
@OptIn(UnstableApi::class)
class WavHeaderFixingDataSource(private val upstream: DataSource) : DataSource {

    class Factory(private val upstream: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = WavHeaderFixingDataSource(upstream.createDataSource())
    }

    private var head: ByteArray? = null
    private var headLength = 0
    private var headPosition = 0

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val length = upstream.open(dataSpec)
        head = null
        headLength = 0
        headPosition = 0
        // The header is only read when a file is opened from its first byte.
        if (dataSpec.position == 0L) {
            val buffer = ByteArray(HEADER_BYTES)
            var filled = 0
            while (filled < buffer.size) {
                val read = upstream.read(buffer, filled, buffer.size - filled)
                if (read == C.RESULT_END_OF_INPUT) break
                filled += read
            }
            fixWavBlockAlign(buffer, filled)
            head = buffer
            headLength = filled
        }
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val bytes = head
        if (bytes != null && headPosition < headLength) {
            val count = minOf(length, headLength - headPosition)
            System.arraycopy(bytes, headPosition, buffer, offset, count)
            headPosition += count
            return count
        }
        if (bytes != null && headLength < HEADER_BYTES) {
            // The whole file fitted in the header buffer: nothing more to read.
            return C.RESULT_END_OF_INPUT
        }
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        head = null
        upstream.close()
    }

    companion object {
        private const val HEADER_BYTES = 8 * 1024

        /** WAV format types whose block size must equal channels × bytes per sample. */
        private val PLAIN_FORMATS = setOf(1, 3, 6, 7)
        private const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE

        /** Corrects the fmt chunk's block size in place when it doesn't match the format. */
        internal fun fixWavBlockAlign(bytes: ByteArray, length: Int) {
            if (length < 12) return
            val riff = ascii(bytes, 0) == "RIFF" || ascii(bytes, 0) == "RF64"
            if (!riff || ascii(bytes, 8) != "WAVE") return
            var position = 12L
            while (position + 8 <= length) {
                val p = position.toInt()
                val size = le32(bytes, p + 4)
                if (ascii(bytes, p) == "fmt ") {
                    val body = p + 8
                    if (size < 16 || body + 16 > length) return
                    var type = le16(bytes, body)
                    if (type == WAVE_FORMAT_EXTENSIBLE && size >= 40 && body + 26 <= length) {
                        type = le16(bytes, body + 24)
                    }
                    val channels = le16(bytes, body + 2)
                    val bits = le16(bytes, body + 14)
                    if (type !in PLAIN_FORMATS || channels <= 0 || bits <= 0 || bits % 8 != 0) return
                    val expected = channels * bits / 8
                    if (le16(bytes, body + 12) != expected) {
                        bytes[body + 12] = (expected and 0xFF).toByte()
                        bytes[body + 13] = ((expected shr 8) and 0xFF).toByte()
                    }
                    return
                }
                // RIFF chunks are padded to an even size.
                position += 8 + size + (size and 1L)
            }
        }

        private fun ascii(bytes: ByteArray, at: Int): String =
            String(CharArray(4) { (bytes[at + it].toInt() and 0xFF).toChar() })

        private fun le16(bytes: ByteArray, at: Int): Int =
            (bytes[at].toInt() and 0xFF) or ((bytes[at + 1].toInt() and 0xFF) shl 8)

        private fun le32(bytes: ByteArray, at: Int): Long =
            (le16(bytes, at).toLong()) or (le16(bytes, at + 2).toLong() shl 16)
    }
}

package io.github.barqallayl.burkan.core.shell

import java.io.ByteArrayOutputStream
import java.io.IOException

/* What adbd sends back on a `shell,v2` stream, for tests that feed the decoder. */

fun packet(id: Int, text: String): ByteArray = packet(id, text.toByteArray())

fun packet(id: Int, data: ByteArray): ByteArray = ByteArrayOutputStream().apply {
    write(id)
    val length = data.size
    write(length and 0xFF)
    write(length shr 8 and 0xFF)
    write(length shr 16 and 0xFF)
    write(length shr 24 and 0xFF)
    write(data)
}.toByteArray()

fun exit(code: Int): ByteArray = packet(ShellProtocolDecoder.ID_EXIT, byteArrayOf(code.toByte()))

class RecordingOpener(private val stream: () -> ShellStream) : ShellStreamOpener {
    val destinations = mutableListOf<String>()
    override fun open(destination: String): ShellStream {
        destinations += destination
        return stream()
    }
}

/** Serves [bytes] [chunk] at a time; at the end returns -1, or throws as libadb does once the daemon closed it. */
class BytesStream(
    private val bytes: ByteArray,
    private val chunk: Int = 4096,
    private val throwAtEnd: Boolean = false,
) : ShellStream {
    private var position = 0
    override fun read(buffer: ByteArray): Int {
        if (position == bytes.size) {
            if (throwAtEnd) throw IOException("Stream closed.") else return -1
        }
        val count = minOf(chunk, buffer.size, bytes.size - position)
        bytes.copyInto(buffer, startIndex = position, endIndex = position + count)
        position += count
        return count
    }

    override fun close() = Unit
}

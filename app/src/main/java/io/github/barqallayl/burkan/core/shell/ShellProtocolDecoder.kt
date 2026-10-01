package io.github.barqallayl.burkan.core.shell

import java.io.ByteArrayOutputStream

/**
 * Decodes adb's shell protocol v2, which keeps stdout and stderr apart and ends with the exit code. Each packet is
 * one id byte, a four-byte little-endian length, then that many bytes of data.
 */
class ShellProtocolDecoder {

    private var pending = ByteArray(INITIAL_CAPACITY)
    private var pendingSize = 0
    private val stdout = ByteArrayOutputStream()
    private val stderr = ByteArrayOutputStream()

    /** Null until the exit packet has arrived. */
    var exitCode: Int? = null
        private set

    /** Feeds the next [count] bytes of [bytes], which may end anywhere inside a packet. */
    fun feed(bytes: ByteArray, count: Int = bytes.size) {
        ensureCapacity(pendingSize + count)
        bytes.copyInto(pending, destinationOffset = pendingSize, endIndex = count)
        pendingSize += count
        var offset = 0
        while (exitCode == null && pendingSize - offset >= HEADER_SIZE) {
            val length = littleEndianInt(offset + 1)
            if (pendingSize - offset - HEADER_SIZE < length) break
            val dataStart = offset + HEADER_SIZE
            when (pending[offset].toInt()) {
                ID_STDOUT -> stdout.write(pending, dataStart, length)
                ID_STDERR -> stderr.write(pending, dataStart, length)
                ID_EXIT -> exitCode = if (length > 0) pending[dataStart].toInt() and 0xFF else 0
                // Window-size and stdin packets only ever go to the device.
            }
            offset = dataStart + length
        }
        pending.copyInto(pending, destinationOffset = 0, startIndex = offset, endIndex = pendingSize)
        pendingSize -= offset
    }

    /** The finished result, or null while the exit packet is still to come. */
    fun result(): ShellResult? = exitCode?.let { code ->
        ShellResult(exitCode = code, stdout = stdout.toString(Charsets.UTF_8), stderr = stderr.toString(Charsets.UTF_8))
    }

    private fun littleEndianInt(at: Int): Int =
        (pending[at].toInt() and 0xFF) or
            ((pending[at + 1].toInt() and 0xFF) shl 8) or
            ((pending[at + 2].toInt() and 0xFF) shl 16) or
            ((pending[at + 3].toInt() and 0xFF) shl 24)

    private fun ensureCapacity(needed: Int) {
        if (needed > pending.size) pending = pending.copyOf(maxOf(needed, pending.size * 2))
    }

    companion object {
        const val ID_STDOUT = 1
        const val ID_STDERR = 2
        const val ID_EXIT = 3
        const val HEADER_SIZE = 5
        private const val INITIAL_CAPACITY = 64 * 1024
    }
}

package de.voicegym.voicegym.util.audio

import de.voicegym.voicegym.util.DecoderBufferListener
import de.voicegym.voicegym.util.RecordBufferListener
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.LinkedBlockingDeque

fun Byte.toPositiveInt() = toInt() and 0xFF

class PCMStorage(val sampleRate: Int) : RecordBufferListener, DecoderBufferListener, InputStream() {

    override fun read(): Int {
        var ret: Int = -1
        if (buffer == null) {
            if (getNextBuffer()) {
                ret = buffer!!.get().toPositiveInt()
            }
        } else if (!buffer!!.hasRemaining()) {
            if (getNextBuffer()) {
                ret = buffer!!.get().toPositiveInt()
            }
        } else {
            ret = buffer!!.get().toPositiveInt()
        }
        return ret
    }

    private fun getNextBuffer(): Boolean {
        if (inputDeque.isNotEmpty()) {
            buffer = ByteBuffer.allocate(2 * inputDeque.peekFirst().size).order(ByteOrder.LITTLE_ENDIAN)
            val array = inputDeque.pollFirst()
            usedDeque.addFirst(array)
            buffer?.asShortBuffer()?.put(array)
            return true
        } else {
            return false
        }
    }

    private val inputDeque = LinkedBlockingDeque<ShortArray>()

    private val usedDeque = LinkedBlockingDeque<ShortArray>()

    private var sealed: Boolean = false

    private var buffer: ByteBuffer? = null

    var size: Int = 0
        private set(value) {
            field = value
        }

    fun ready(): Boolean = sealed

    override fun onBufferReady(data: ShortArray) {
        if (data.isEmpty()) throw Error("No empty arrays allowed in storage")
        if (!sealed) {
            size += data.size
            inputDeque.addLast(data)
        }
    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = true

    override fun isDecoded() {
        stopListening()
    }

    fun stopListening() {
        if (!sealed) {
            sealed = true
        }
    }

    fun rewind() {
        buffer = null
        if (sealed) {
            while (usedDeque.isNotEmpty()) inputDeque.addFirst(usedDeque.pollFirst())
        }
    }

    fun asShortBuffer(): ShortBuffer {
        if (sealed) {
            val currentPosition = usedDeque.size
            rewind()
            val returnBuffer = ShortBuffer.allocate(size)
            while (inputDeque.isNotEmpty()) {
                val arr = inputDeque.pollFirst()
                returnBuffer.put(arr)
                usedDeque.addFirst(arr)
            }
            // okay buffer created let's get the deque and queue in the earlier state
            while (usedDeque.size > currentPosition) {
                inputDeque.addFirst(usedDeque.removeFirst())
            }
            return returnBuffer
        } else {
            throw Error("Cannot return ShortBuffer from unsealed PCMStorage")
        }
    }
}

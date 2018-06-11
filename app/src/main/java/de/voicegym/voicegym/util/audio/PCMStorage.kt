package de.voicegym.voicegym.util.audio

import de.voicegym.voicegym.util.RecordBufferListener
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingDeque

fun Byte.toPositiveInt() = toInt() and 0xFF

class PCMStorage(val sampleRate: Int) : RecordBufferListener, InputStream() {
    // TODO refactor class so it uses two deques and can be freely winded forward and back
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
        if (inputQueue.isNotEmpty()) {
            buffer = ByteBuffer.allocate(2 * inputQueue.peek().size).order(ByteOrder.LITTLE_ENDIAN)
            val array = inputQueue.poll()
            usedDeque.addLast(array)
            buffer?.asShortBuffer()?.put(array)
            return true
        } else {
            return false
        }
    }

    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()

    private val usedDeque = LinkedBlockingDeque<ShortArray>()

    private var sealed: Boolean = false

    private var buffer: ByteBuffer? = null

    var size: Int = 0

    fun ready(): Boolean = sealed

    override fun onBufferReady(data: ShortArray) {
        if (data.isEmpty()) throw Error("No empty arrays allowed in storage")
        if (!sealed) {
            size += data.size
            inputQueue.add(data)
        }
    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = true

    fun stopListening() {
        if (!sealed) {
            sealed = true
        }
    }

    fun rewind() {
        if (sealed) {
            while (inputQueue.isNotEmpty()) usedDeque.addLast(inputQueue.poll())
            while (usedDeque.isNotEmpty()) inputQueue.add(usedDeque.poll())
        }
    }

    fun asShortBuffer(): ShortBuffer {
        if (sealed) {
            val currentPosition = usedDeque.size
            rewind()
            val returnBuffer = ShortBuffer.allocate(size)
            while (inputQueue.isNotEmpty()) {
                val arr = inputQueue.poll()
                returnBuffer.put(arr)
                usedDeque.addLast(arr)
            }
            // okay buffer created let's get the deque and queue in the earlier state
            val tempDeque = LinkedBlockingDeque<ShortArray>()
            while (usedDeque.size > currentPosition) {
                tempDeque.add(usedDeque.removeLast())
            }
            while (tempDeque.isNotEmpty()) inputQueue.add(tempDeque.poll())
            return returnBuffer
        } else {
            throw Error("Cannot return ShortBuffer from unsealed PCMStorage")
        }
    }
}

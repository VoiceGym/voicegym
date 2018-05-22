package de.voicegym.voicegym.audioHelper

import java.io.InputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

fun Byte.toUnsignedInt() = toInt() and 0xFF

class PCMStorage(val sampleRate: Int) : RecordBufferListener, InputStream() {

    override fun read(): Int {
        var ret = 0
        if (!sealed || buffer == null) return -1
        try {
            ret = buffer!!.get().toUnsignedInt()
        } catch (e: BufferUnderflowException) {
            ret = -1
        }
        return ret
    }

    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    private var readArray: ShortArray? = null
    private var readPosition: Int = 0
    private var sealed: Boolean = false

    private var buffer: ByteBuffer? = null

    private fun countStoredSamples(): Int {
        var size = 0
        inputQueue.iterator().forEach { size += it.size }
        return size
    }

    private fun collectDataStoredInBuffer(): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(countStoredSamples() * 2)
        while (hasData()) {
            byteBuffer.asShortBuffer().put(readShort())
        }
        return byteBuffer
    }

    private fun readShort(): Short {
        if (readArray == null) setToNextArray()
        val ret = readArray!![readPosition++]
        if (readPosition == readArray?.size) setToNextArray()
        return ret
    }

    private fun setToNextArray() {
        if (inputQueue.isNotEmpty()) {
            readArray = inputQueue.poll()
            readPosition = 0
        } else {
            throw Error("Cannot set to next array if there is none")
        }
    }

    fun ready(): Boolean = sealed

    private fun hasData(): Boolean {
        if (!sealed) return false
        if (readArray != null && readPosition < readArray!!.size - 1) return true
        if (inputQueue.isNotEmpty()) return true
        return false
    }

    override fun onBufferReady(data: ShortArray) {
        if (data.isEmpty()) throw Error("No empty arrays allowed in storage")
        if (!sealed) inputQueue.add(data)
    }

    override fun canHandleBufferSize(bufferSize: Int): Boolean = true

    fun stopListening() {
        if (!sealed) {
            sealed = true
            buffer = collectDataStoredInBuffer()
        } else {
            throw RuntimeException("PCMStorage already sealed")
        }
    }
}

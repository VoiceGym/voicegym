package de.voicegym.voicegym.audioHelper

import java.util.concurrent.ConcurrentLinkedQueue

class PCMStorage(val sampleRate: Int) : RecordBufferListener {

    private val inputQueue = ConcurrentLinkedQueue<ShortArray>()
    private var readArray: ShortArray? = null
    private var readPosition: Int = 0
    private var sealed: Boolean = false

    fun read(): Short {
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

    fun hasData(): Boolean {
        if (!sealed) return false
        if (readArray != null && readPosition < readArray!!.size) return true
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
        } else {
            throw RuntimeException("PCMStorage already sealed")
        }
    }
}

package de.voicegym.voicegym.util

interface RecordBufferListener {

    /**
     * TODO: Proper documentation
     */
    fun onBufferReady(data: ShortArray)

    /**
     * TODO: Proper documentation
     */
    fun canHandleBufferSize(bufferSize: Int): Boolean
}

package de.voicegym.voicegym.util

interface RecordBufferListener {
    /**
     * This function must be implemented to receive data from the RecordHelper
     */
    fun onBufferReady(data: ShortArray)

    /**
     * this function must be implemented in order to subscribe to the RecordHelper
     */
    fun canHandleBufferSize(bufferSize: Int): Boolean
}

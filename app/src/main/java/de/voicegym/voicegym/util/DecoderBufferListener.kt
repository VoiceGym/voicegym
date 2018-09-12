package de.voicegym.voicegym.util

interface DecoderBufferListener {
    /**
     * when the MP4Decoder fills up a buffer array of given size it will call this method
     * and provide a copy of the containing data
     */
    fun onBufferReady(data: ShortArray)

    /**
     * Tells the subscribing component that the decoding has been completed
     */
    fun isDecoded()
}

package de.voicegym.voicegym.util.audio

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer


class MP4Decoder(inputFilename: String) {

    private val extractor = MediaExtractor()
    private val decoder: MediaCodec
    private val inputFormat: MediaFormat

    private val inputBuffers: Array<ByteBuffer>
    private var outputBuffers: Array<ByteBuffer>

    private var isAtEOF: Boolean = false
    private var outputBufferIndex = -1

    val sampleRate: Int
        get() = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

    init {
        extractor.setDataSource(inputFilename)
        // check prerequisites
        if (extractor.trackCount != 1) throw Error("Multiple Audio Channels not supported yet")
        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME)
        if (!mime.startsWith("audio/")) throw Error("Given file is not an Audio file")

        // prepare for decoding first track
        extractor.selectTrack(0)
        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        inputFormat = format
        decoder.start()
        inputBuffers = decoder.inputBuffers
        outputBuffers = decoder.outputBuffers
        isAtEOF = false
    }

    private fun readData(info: BufferInfo): ByteBuffer? {

        while (true) {
            if (!isAtEOF) {
                val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val size = extractor.readSampleData(inputBuffers[inputBufferIndex], 0)
                    if (size < 0) {
                        // End Of File
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isAtEOF = true
                    } else {
                        decoder.queueInputBuffer(inputBufferIndex, 0, size, extractor.getSampleTime(), 0)
                        extractor.advance()
                    }
                }
            }

            if (outputBufferIndex >= 0) {
                outputBuffers[outputBufferIndex].position(0)
            }

            @SuppressLint("WrongConstant")
            outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000)
            if (outputBufferIndex >= 0) {
                // EOF
                if (info.flags != 0) {
                    decoder.stop()
                    decoder.release()
                    return null
                }
                Log.i("Buffer ", outputBufferIndex.toString())
                decoder.releaseOutputBuffer(outputBufferIndex, false)

                return outputBuffers[outputBufferIndex]


            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.outputBuffers
            }
        }
    }

    private fun read(): ShortArray? {
        val bufferInfo = BufferInfo()
        val buffer = readData(bufferInfo) ?: return null
        val samplesRead = bufferInfo.size / 2
        val shorts = ShortArray(samplesRead)
        for (i in 0 until samplesRead) {
            shorts[i] = buffer.asShortBuffer().get()
        }

        return shorts
    }

    companion object {
        fun getPCMStorage(fromAudioFile: String): PCMStorage {
            val mp4Decoder = MP4Decoder(fromAudioFile)
            val pcmStorage = PCMStorage(mp4Decoder.sampleRate)
            var notEOF = true
            while (notEOF) {
                val read = mp4Decoder.read()
                read?.let { pcmStorage.onBufferReady(read) }
                if (read == null) notEOF = false
            }
            return pcmStorage
        }
    }
}

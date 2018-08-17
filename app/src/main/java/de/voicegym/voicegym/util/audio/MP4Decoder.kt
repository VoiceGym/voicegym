package de.voicegym.voicegym.util.audio

import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Process
import android.util.Log
import de.voicegym.voicegym.util.DecoderBufferListener
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread


class MP4Decoder(private val bufferSizeForCallbacks: Int) {

    /**
     * The buffer array in which the decoded samples are stored before they are handed out to the subscribers
     */
    private var outputBufferArray: ShortArray = ShortArray(bufferSizeForCallbacks)

    /**
     * This points to the position in the outputBufferArray where new samples can be written
     */
    private var currentOutputBufferPosition: Int = 0

    /**
     * After decoding this function is called to write into the outputBufferArray,
     * and if the outputBufferArray is full this function hands out copies of it to the subscribers
     */
    private fun internalBufferReady(data: ShortArray) {

        if (data.size > bufferSizeForCallbacks) throw Error("Callback buffer must be larger than internal one for this algorithm")

        val lengthToCopy = if (data.size < (outputBufferArray.size - currentOutputBufferPosition)) {
            // Okay data fits into outputbuffer
            data.size
        } else {
            // Buffer will be filled up by the data
            outputBufferArray.size - currentOutputBufferPosition
        }

        if (lengthToCopy != 0) {
            System.arraycopy(data, 0, outputBufferArray, currentOutputBufferPosition, lengthToCopy)
        }

        if (lengthToCopy < data.size) {
            // Internal buffer has been filled, up can be handed out
            subscribers.forEach { it.onBufferReady(outputBufferArray.copyOf()) }
            // Copy remaining data into outputbuffer
            System.arraycopy(data, lengthToCopy, outputBufferArray, 0, data.size - lengthToCopy)
            currentOutputBufferPosition = data.size - lengthToCopy
        } else {
            currentOutputBufferPosition += lengthToCopy
        }

    }

    /**
     * list of the subscribers that are interested in receiving copies of the decoded samples
     */
    private val subscribers = ArrayList<DecoderBufferListener>()

    /**
     * add a new subscriber to our decoder
     */
    fun addBufferListener(listener: DecoderBufferListener) = subscribers.add(listener)

    /**
     * remove a subscriber
     */
    fun removeBufferListener(listener: DecoderBufferListener) = subscribers.remove(listener)

    /**
     * starts a decoding thread for the given file if we have any subscribers
     */
    fun startDecoding(inputFile: File) {
        if (subscribers.isNotEmpty()) {
            thread(start = true, priority = Process.THREAD_PRIORITY_AUDIO) {
                decode(inputFile)
            }
        } else throw Error("No callback subscribers, doesn't make sense to decode anything")
    }

    /**
     * in case for API levels 19 and 20 this is used to store the outputBufferArrays of the Codec
     */
    private var deprecatedOutputBuffers: Array<ByteBuffer>? = null

    private fun decode(inputFile: File) {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile.path)
        val trackNumber = findFirstAudioTrack(extractor)
        if (trackNumber == -1) {
            throw Error("No audio track in given filename")
        }

        val codec = getMediaDecoderForMediaExtractor(extractor, trackNumber)
        codec.start()

        // Output loop
        var endOfOutputStream = false
        var endOfInputFile = false
        val bufferInfo = MediaCodec.BufferInfo()
        var outputFormat: MediaFormat?
        deprecatedOutputBuffers = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) codec.outputBuffers else null

        while (!endOfOutputStream) {
            var inputBufferFull = false
            while (!endOfInputFile && !inputBufferFull) {
                val indexOfBuffer = codec.dequeueInputBuffer(1000)
                when {
                    indexOfBuffer >= 0                               -> {
                        val inputBuffer = when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> codec.getInputBuffer(indexOfBuffer)
                            else                                                  -> codec.inputBuffers[indexOfBuffer]
                        }

                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        val presentationTime = extractor.sampleTime
                        if (sampleSize <= 0 || !extractor.advance()) {
                            endOfInputFile = true
                        }
                        codec.queueInputBuffer(indexOfBuffer, 0, if (sampleSize > 0) sampleSize else 0, presentationTime, if (endOfInputFile) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    }

                    indexOfBuffer == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        inputBufferFull = true
                    }

                    else                                             -> throw Error("could not retrieve buffer")
                }
            }

            val bufferId = codec.dequeueOutputBuffer(bufferInfo, 60)
            when {
                bufferId >= 0                           -> {
                    internalBufferReady(retrieveSamplesForChannelAndQueuedInputBuffer(codec, trackNumber, bufferInfo, bufferId))
                }

                bufferId == INFO_OUTPUT_BUFFERS_CHANGED -> {
                    // can be ignored for api >= 21
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) deprecatedOutputBuffers = codec.outputBuffers
                }

                bufferId == INFO_OUTPUT_FORMAT_CHANGED  -> {
                    // new format
                    outputFormat = codec.outputFormat
                    Log.i("OutputFormat now: ", outputFormat.toString())
                }

                bufferId == INFO_TRY_AGAIN_LATER        -> {
                    // okay not possible right now
                }

                else                                    -> throw Error("Unknown Key")
            }
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM !== 0) {
                endOfOutputStream = true
                codec.stop()
                codec.release()
            }
        }
        extractor.release()

        /**
         * hand out the remaining samples in the outputBufferArray
         */
        // overwrite remaining old values from previous cycles with zeroes
        for (i in currentOutputBufferPosition until outputBufferArray.size) outputBufferArray[i] = 0
        // tell everyone were done
        subscribers.forEach {
            // hand out the last buffer
            it.onBufferReady(outputBufferArray.copyOf())
            // finish off
            it.isDecoded()
        }
    }

    private fun retrieveSamplesForChannelAndQueuedInputBuffer(codec: MediaCodec, audioChannel: Int, bufferInfo: MediaCodec.BufferInfo, bufferId: Int): ShortArray {
        if (bufferId < 0) {
            throw Error("need a bufferId")
        }
        val outputBuffer = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> codec.getOutputBuffer(bufferId)
            else                                                  -> deprecatedOutputBuffers!![bufferId]
        }

        val format = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> codec.getOutputFormat(bufferId)
            else                                                  -> codec.outputFormat
        }

        val samples = outputBuffer!!.order(ByteOrder.nativeOrder()).asShortBuffer()
        samples.rewind()
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        if (audioChannel < 0 || audioChannel >= numChannels) {
            throw Error("Requested a channel not available in codec.")
        }
        if (numChannels != 1) throw Error("So far only supporting 1 channel per Audio file")
        val res = ShortArray(bufferInfo.size / 2)
        samples.get(res)
        outputBuffer.clear()
        codec.releaseOutputBuffer(bufferId, false)
        return res

    }

    /**
     * selects the first audio track in a file, returns -1 if there ain't one
     */
    private fun findFirstAudioTrack(mediaExtractor: MediaExtractor): Int {
        var mime = ""
        for (i in 0..mediaExtractor.trackCount) {
            mime = mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    /**
     * returns a MediaCodec for the given channel, if that channel is an audio channel
     */
    private fun getMediaDecoderForMediaExtractor(mediaExtractor: MediaExtractor, audioChannel: Int): MediaCodec {
        var trackFormat: MediaFormat? = null
        // select the first audio track

        val inputFormat = mediaExtractor.getTrackFormat(audioChannel)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)
        if (mime.startsWith("audio/")) {
            mediaExtractor.selectTrack(audioChannel)
            trackFormat = inputFormat
            Log.i("InputFormat", inputFormat.toString())
        } else {
            throw Error("Selected channel was no audio track")
        }
        trackFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        val codec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME))
        codec.configure(trackFormat, null, null, 0)
        return codec
    }
}

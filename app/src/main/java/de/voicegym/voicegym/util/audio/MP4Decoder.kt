package de.voicegym.voicegym.util.audio

import android.media.MediaCodec
import android.media.MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
import android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
import android.media.MediaCodec.INFO_TRY_AGAIN_LATER
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MP4Decoder {

    companion object {

        private var deprecatedOutputBuffers: Array<ByteBuffer>? = null

        fun getPCMStorage(inputFile: File): PCMStorage {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.path)
            val trackNumber = findFirstAudioTrack(extractor)
            if (trackNumber == -1) {
                throw Error("No audio track in given filename")
            }
            val sampleRate = extractor.getTrackFormat(trackNumber).getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val storage = PCMStorage(sampleRate)

            val codec = getMediaDecoderForMediaExtractor(extractor, trackNumber)
            codec.start()

            // Output loop
            var endOfOutputStream = false
            var endOfInputFile = false
            val mapOfSamples = HashMap<Long, ShortArray>()
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
                        val samples = retrieveSamplesForChannelAndQueuedInputBuffer(codec, trackNumber, bufferInfo, bufferId)
                        bufferInfo.presentationTimeUs
                        mapOfSamples[bufferInfo.presentationTimeUs] = samples
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
                    //TODO release extractor
                }
            }

            for (entry in mapOfSamples.toSortedMap()) {
                storage.onBufferReady(entry.value)
            }
            storage.stopListening()
            storage.reverseForBugfix()
            return storage
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


        private fun findFirstAudioTrack(mediaExtractor: MediaExtractor): Int {
            var mime = ""
            for (i in 0..mediaExtractor.trackCount) {
                mime = mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("audio/")) {
                    return i;
                }
            }
            return -1;
        }

        private fun getMediaDecoderForMediaExtractor(mediaExtractor: MediaExtractor, audioChannel: Int): MediaCodec {
            var trackFormat: MediaFormat? = null
            // select the first audio track

            val inputFormat = mediaExtractor.getTrackFormat(audioChannel);
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
}

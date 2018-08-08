package de.voicegym.voicegym.util.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * @author Anna Stępień <anna.stepien@semantive.com>
 * @since 02.07.15
 *
 *
 * PCMEncoder allows encoding multiple input streams of PCM data into one, compressed audio file.
 */

class PCMEncoder
/**
 * Creates encoder with given params for output file
 *
 * @param bitrate
 * @param sampleRate
 * @param channelCount
 */
(private val bitrate: Int, private val sampleRate: Int, private val channelCount: Int) {

    private var mediaFormat: MediaFormat? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var codecInputBuffers: Array<ByteBuffer>? = null
    private var codecOutputBuffers: Array<ByteBuffer>? = null
    private var bufferInfo: MediaCodec.BufferInfo? = null
    private var audioTrackId: Int = 0
    private var totalBytesRead: Int = 0
    private var presentationTimeUs = 0L
    var outputPath: String? = null


    fun prepare() {
        if (outputPath == null) {
            throw IllegalStateException("The output path must be set first!")
        }
        try {
            mediaFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, sampleRate, channelCount)
            mediaFormat?.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            mediaFormat?.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            mediaFormat?.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            mediaFormat?.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)


            mediaCodec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
            mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()

            codecInputBuffers = mediaCodec?.inputBuffers
            codecOutputBuffers = mediaCodec?.outputBuffers

            bufferInfo = MediaCodec.BufferInfo()

            mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            totalBytesRead = 0
            presentationTimeUs = 0
        } catch (e: IOException) {
            Log.e(TAG, "Exception while initializing PCMEncoder", e)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping PCMEncoder")
        handleEndOfStream()
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaMuxer?.stop()
        mediaMuxer?.release()
    }

    private fun handleEndOfStream() {
        val inputBufferIndex = mediaCodec?.dequeueInputBuffer(CODEC_TIMEOUT.toLong())
        mediaCodec?.queueInputBuffer(inputBufferIndex!!, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        writeOutputs()
    }

    /**
     * Encodes input stream
     *
     * @param inputStream
     * @param sampleRate  sample rate of input stream
     * @throws IOException
     */
    fun encode(inputStream: InputStream, sampleRate: Int) {
        Log.d(TAG, "Starting encoding of InputStream")
        val tempBuffer = ByteArray(2 * sampleRate)
        var hasMoreData = true
        var stop = false

        while (!stop) {
            var inputBufferIndex = 0
            var currentBatchRead = 0
            while (inputBufferIndex != -1 && hasMoreData && currentBatchRead <= 50 * sampleRate) {
                inputBufferIndex = mediaCodec!!.dequeueInputBuffer(CODEC_TIMEOUT.toLong())

                if (inputBufferIndex >= 0) {
                    val buffer = codecInputBuffers!![inputBufferIndex]
                    buffer.clear()

                    val bytesRead = inputStream.read(tempBuffer, 0, buffer.limit())
                    if (bytesRead == -1) {
                        mediaCodec?.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, 0)
                        hasMoreData = false
                        stop = true
                    } else {
                        totalBytesRead += bytesRead
                        currentBatchRead += bytesRead
                        buffer.put(tempBuffer, 0, bytesRead)
                        mediaCodec?.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                        presentationTimeUs = 1000000L * (totalBytesRead / 2) / sampleRate
                    }
                }
            }

            writeOutputs()
        }

        inputStream.close()
        Log.d(TAG, "Finished encoding of InputStream")
    }

    private fun writeOutputs() {
        var outputBufferIndex = 0
        while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo!!, CODEC_TIMEOUT.toLong())
            if (outputBufferIndex >= 0) {
                val encodedData = codecOutputBuffers!![outputBufferIndex]
                encodedData.position(bufferInfo!!.offset)
                encodedData.limit(bufferInfo!!.offset + bufferInfo!!.size)

                if (bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && bufferInfo!!.size != 0) {
                    mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                } else {
                    mediaMuxer?.writeSampleData(audioTrackId, codecOutputBuffers!![outputBufferIndex], bufferInfo!!)
                    mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mediaFormat = mediaCodec?.outputFormat
                audioTrackId = mediaMuxer!!.addTrack(mediaFormat!!)
                mediaMuxer?.start()
            }
        }
    }

    companion object {
        private const val TAG = "PCMEncoder"
        private const val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
        private const val CODEC_TIMEOUT = 5000
    }
}

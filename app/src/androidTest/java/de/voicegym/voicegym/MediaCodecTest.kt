package de.voicegym.voicegym

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream


@RunWith(AndroidJUnit4::class)
@SmallTest
class MediaCodecTest {

    val LOGTAG = "MEDIATEST"

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        Assert.assertEquals("de.voicegym.voicegym", appContext.packageName)
    }

    @Test
    fun convertTest() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputFile: InputStream = context.assets.open("pfeifen.raw")
//        val inputFile = "/input.pcm"

        val outFile : String = File(appContext.filesDir, "pfeifen.mp4").absolutePath
        Log.d(LOGTAG, outFile)
        val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
        val CODEC_TIMEOUT = 5000L
        val sampleRate = 44100
        val channelCount = 1
        val bitrate = 64000

        var mediaFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, sampleRate, channelCount)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)

        val mediaCodec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()

        val codecInputBuffers = mediaCodec.inputBuffers
        val codecOutputBuffers = mediaCodec.outputBuffers

        val bufferInfo = MediaCodec.BufferInfo()

        val mux = MediaMuxer(outFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val tempBuffer = ByteArray(2 * sampleRate)
        var hasMoreData = true
        var stop = false
        var totalBytesRead = 0
        var presentationTimeUs = 0L

        while (!stop) {
            var inputBufferIndex = 0
            var currentBatchRead = 0
            while (inputBufferIndex != -1 && hasMoreData && currentBatchRead <= 50 * sampleRate) {
                inputBufferIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT)

                if (inputBufferIndex >= 0) {
                    val buffer = codecInputBuffers[inputBufferIndex]
                    buffer.clear()

                    val bytesRead = inputFile.read(tempBuffer, 0, buffer.limit())
                    if (bytesRead == -1) {
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs as Long, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        hasMoreData = false
                        stop = true
                    } else {
                        totalBytesRead += bytesRead
                        currentBatchRead += bytesRead
                        buffer.put(tempBuffer, 0, bytesRead)
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs as Long, 0)
                        presentationTimeUs = 1000000L * (totalBytesRead / 2L) / sampleRate
                    }
                }
            }

            var outputBufferIndex = 0
            var audioTrackId = 0
            while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT)
                if (outputBufferIndex >= 0) {
                    val encodedData = codecOutputBuffers[outputBufferIndex]
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && bufferInfo.size != 0) {
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    } else {
                        mux.writeSampleData(audioTrackId, codecOutputBuffers[outputBufferIndex], bufferInfo)
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mediaFormat = mediaCodec.outputFormat
                    audioTrackId = mux.addTrack(mediaFormat)
                    mux.start()
                }
            }
        }

        inputFile.close()
        mediaCodec.stop()
        mediaCodec.release()
        mux.stop()
        mux.release()

        Assert.assertEquals(1, 1)


    }

}


//    @Test
//    fun pcm2mp4() {
//        val filePath = Environment.getExternalStorageDirectory().getPath() + "/" + AUDIO_RECORDING_FILE_NAME
//        val inputFile = File(filePath)
//        val fis = FileInputStream(inputFile)
//
//        val outputFile = File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + COMPRESSED_AUDIO_FILE_NAME)
//        if (outputFile.exists())
//            outputFile.delete()
//
//        val mux = MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//
//        var outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE,
//                SAMPLING_RATE, 1)
//        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
//        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE)
//
//        val codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
//        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        codec.start()
//
//        val codecInputBuffers = codec.getInputBuffers() // Note: Array of buffers
//        val codecOutputBuffers = codec.getOutputBuffers()
//
//        val outBuffInfo = MediaCodec.BufferInfo()
//
//        val tempBuffer = ByteArray(BUFFER_SIZE)
//        var hasMoreData = true
//        var presentationTimeUs = 0
//        var audioTrackIdx = 0
//        var totalBytesRead = 0
//        var percentComplete = 0
//
//        do {
//            var inputBufIndex = 0
//            while (inputBufIndex != -1 && hasMoreData) {
//                inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS)
//
//                if (inputBufIndex >= 0) {
//                    val dstBuf = codecInputBuffers [inputBufIndex]
//                    dstBuf.clear()
//
//                    val bytesRead = fis.read(tempBuffer, 0, dstBuf.limit())
//                    if (bytesRead == -1) { // -1 implies EOS
//                        hasMoreData = false
//                        codec.queueInputBuffer(inputBufIndex, 0, 0, presentationTimeUs.toLong(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
//                    } else {
//                        totalBytesRead += bytesRead
//                        dstBuf.put(tempBuffer, 0, bytesRead)
//                        codec.queueInputBuffer(inputBufIndex, 0, bytesRead,  presentationTimeUs.toLong(), 0)
//                        presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE
//                    }
//                }
//            }
//
//            // Drain audio
//            var outputBufIndex = 0
//            while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
//
//                outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS)
//                if (outputBufIndex >= 0) {
//                    val encodedData = codecOutputBuffers[outputBufIndex]
//                    encodedData.position(outBuffInfo.offset)
//                    encodedData.limit(outBuffInfo.offset + outBuffInfo.size)
//
//                    if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
//                        codec.releaseOutputBuffer(outputBufIndex, false)
//                    } else {
//                        mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo)
//                        codec.releaseOutputBuffer(outputBufIndex, false)
//                    }
//                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    outputFormat = codec.getOutputFormat()
//                    Log.v(LOGTAG, "Output format changed - " + outputFormat)
//                    audioTrackIdx = mux.addTrack(outputFormat)
//                    mux.start()
//                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                    Log.e(LOGTAG, "Output buffers changed during encode!")
//                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                    // NO OP
//                } else {
//                    Log.e(LOGTAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex)
//                }
//            }
//            percentComplete = (int) Math . round (((float) totalBytesRead /(float) inputFile . length ()) * 100.0)
//            Log.v(LOGTAG, "Conversion % - " percentComplete)
//        } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM && !mStop)
//
//        fis.close()
//        mux.stop()
//        mux.release()
//        Log.v(LOGTAG, "Compression done ...")
//    } catch (FileNotFoundException e)
//    {
//        Log.e(LOGTAG, "File not found!", e)
//    } catch (IOException e)
//    {
//        Log.e(LOGTAG, "IO exception!", e)
//    }
//}

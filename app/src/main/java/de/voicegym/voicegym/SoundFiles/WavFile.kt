package de.voicegym.voicegym.SoundFiles

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*-------------------------------------------------------------------
// The canonical wave file would look like this
// Byte 0 1 2 3
// Char R I F F
riffChunkSize:Int          // 4 bytes at byte  4; littleEndian
// Byte  8  9 10 11
// Char  W  A  V  E

// Byte 12 13 14 15
// Char  f  m  t
var fmtChunkSize: Int          // 4 bytes at byte 16; littleEndian
var audioFormat:Short          // 2 bytes at byte 20; littleEndian
var numChannels:Short          // 2 bytes at byte 22; littleEndian
var sampleRate:Int             // 4 bytes at byte 24; littleEndian
var byteRate: Int              // 4 bytes at byte 28; littleEndian
var blockAlign: Short          // 2 bytes at byte 32; littleEndian
var bitPerSample: Short        // 2 bytes at byte 34; littleEndian

// Byte 36 37 38 39
// Char  d  a  t  a
var dataChunkSize: Int         // 4 byte at byte 40; littleEndian
// followed by the data in littleEndian Format
-------------------------------------------------------------------*/
class WavFile(private val stream: InputStream) {
    private val riffHeader = "RIFF"
    private val riffChunkSize: Int
    private val riffType = "WAVE"
    private val fmtHeader = "fmt "
    private val fmtChunkSize: Int
    private val audioFormat: Short
    private val numChannels: Short
    private val sampleRate: Int
    private val byteRate: Int
    private val blockAlign: Short
    private val bitPerSample: Short
    private val dataHeader = "data"
    private val dataChunkSize: Int
    private val outBuffer: ByteBuffer

    init {
        val data = ByteArray(44)
        stream.read(data)
        if (!checkStringAtPosition(riffHeader, 0, data)) {
            throw RuntimeException("Error File Format Not Supported")
        }
        if (!checkStringAtPosition(riffType + fmtHeader, 8, data)) {
            throw RuntimeException("Error File Format Not Supported")
        }
        if (!checkStringAtPosition(dataHeader, 36, data)) {
            throw RuntimeException("Error File Format Not Supported")
        }
        val byteBuffer = ByteBuffer.wrap(data)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        riffChunkSize = byteBuffer.getInt(4)
        fmtChunkSize = byteBuffer.getInt(16)
        audioFormat = byteBuffer.getShort(20)
        numChannels = byteBuffer.getShort(22)
        sampleRate = byteBuffer.getInt(24)
        byteRate = byteBuffer.getInt(28)
        blockAlign = byteBuffer.getShort(32)
        bitPerSample = byteBuffer.getShort(34)
        dataChunkSize = byteBuffer.getInt(40)
        if ((bitPerSample / 8) != byteRate / sampleRate) throw RuntimeException("Wav file corrupt")
        stream.mark(44)
        outBuffer = ByteBuffer.allocate(bitPerSample / 8)
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    constructor(file: File) : this(FileInputStream(file))


    private fun checkStringAtPosition(str: String, startPosition: Int, data: ByteArray) =
            str.toCharArray().filterIndexed { i, char ->
                char != data[startPosition + i].toChar()
            }.isEmpty()

    fun getNextPCMSample(): Short {
        if (bitPerSample == 16.toShort()) {
            outBuffer.put(0, stream.read().toByte())
            outBuffer.put(1, stream.read().toByte())
            return outBuffer.getShort(0)
        } else {
            throw RuntimeException("Cannot retrieve 16bit frame from not 16bit wave file")
        }
    }

    /**
     * @param t time in ms
     * @return a timeframe of length t (in ms), starting from last mark
     */
    fun getTimeFrame(t: Int): ShortArray = getPCMBlock(blocksize = t * sampleRate / 1000)

    /**
     * @param blocksize: number of pcm samples in block
     * @return the duration of the block of pcm samples in ms
     */
    fun getFrameLength(blocksize: Int) = 1000 * blocksize / sampleRate

    /**
     * Fetches a block of PCM Samples
     * Repeated application fetches the next block until the end of available data,
     * then it will return an ShortArray filled with 0.
     *
     * @param blocksize the number of samples to collect
     * @return ShortArray of length blocksize containing PCM Samples
     */
    fun getPCMBlock(blocksize: Int): ShortArray {
        val bytesPerSample = bitPerSample / 8;
        val bytesInBlock = blocksize * bytesPerSample
        val buffer = ByteBuffer
                .allocate(bytesInBlock)
                .order(ByteOrder.LITTLE_ENDIAN)
        val pcmArray = ShortArray(blocksize)

        for (i in 0 until bytesInBlock) {
            val data = stream.read()
            if (data != -1)
                buffer.put(i, data.toByte())
            else
                buffer.put(i, 0b0)
        }

        for (i in 0 until bytesInBlock step bytesPerSample) {
            pcmArray[i / bytesPerSample] = buffer.getShort(i)
        }

        return pcmArray
    }


    fun getNumberOfPCMSamples(): Int =
            this.dataChunkSize / (this.bitPerSample / 8)


    override fun toString(): String {
        return """WavFile(
                |  riffHeader='$riffHeader',
                |  riffChunkSize=$riffChunkSize,
                |  riffType='$riffType',
                |  fmtHeader='$fmtHeader',
                |  fmtChunkSize=$fmtChunkSize,
                |  audioFormat=$audioFormat,
                |  numChannels=$numChannels,
                |  sampleRate=$sampleRate,
                |  byteRate=$byteRate,
                |  blockAlign=$blockAlign,
                |  bitPerSample=$bitPerSample,
                |  dataChunkSize=$dataChunkSize,
                |  dataHeader='$dataHeader'
                |)""".trimMargin()
    }

}
package de.voicegym.voicegym.SoundFiles

import java.io.File
import java.io.FileInputStream
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
class WavFile {
    private val stream: FileInputStream
    private val riffHeader = "RIFF"
    val riffChunkSize: Int
    private val riffType = "WAVE"
    private val fmtHeader = "fmt "
    val fmtChunkSize: Int
    val audioFormat: Short
    val numChannels: Short
    val sampleRate: Int
    val byteRate: Int
    val blockAlign: Short
    val bitPerSample: Short
    private val dataHeader = "data"
    val dataChunkSize: Int
    private val outBuffer: ByteBuffer

    constructor(file: File) {
        stream = FileInputStream(file)
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

        val byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        riffChunkSize = byteBuffer.getInt(4) // 4 bytes at byte  4; littleEndian


        fmtChunkSize = byteBuffer.getInt(16) // 4 bytes at byte 16; littleEndian
        audioFormat = byteBuffer.getShort(20) // 2 bytes at byte 20; littleEndian
        numChannels = byteBuffer.getShort(22) // 2 bytes at byte 22; littleEndian
        sampleRate = byteBuffer.getInt(24) // 4 bytes at byte 24; littleEndian
        byteRate = byteBuffer.getInt(28) // 4 bytes at byte 28; littleEndian
        blockAlign = byteBuffer.getShort(32) // 2 bytes at byte 32; littleEndian
        bitPerSample = byteBuffer.getShort(34) // 2 bytes at byte 34; littleEndian
        dataChunkSize = byteBuffer.getInt(40) // 4 byte at byte 40; littleEndian
        stream.mark(44)
        outBuffer = ByteBuffer.allocate(bitPerSample / 8);
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    fun checkStringAtPosition(str: String, startPosition: Int, data: ByteArray) =
            str.toCharArray().filterIndexed { i, char ->
                !(char == data[startPosition + i].toChar())
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
     * returns a timeframe of length t (in ms)
     */
    fun getTimeFrame(t: Int): ShortArray {
        var array = ShortArray(sampleRate / (t / 1000))
        // TODO implement
        throw NotImplementedError()
        return array
    }

    fun getNumberOfPCMSamples(): Int {
        return this.dataChunkSize / (this.bitPerSample / 8)
    }

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
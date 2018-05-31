package de.voicegym.voicegym.util.audio

import android.os.Environment
import java.io.File
import java.io.InputStream

fun getDezibelFromAmplitude(amplitude: Double): Double = 20 * Math.log10(amplitude)


fun savePCMInputStreamOnSDCard(fileName: String, inputStream: InputStream, sampleRate: Int, bitRate: Int) {
    if (isExternalStorageWritable()) {
        val pcmEncoder = PCMEncoder(bitRate, sampleRate, 1)
        pcmEncoder.outputPath = getOutFile(fileName, "m4a").path
        pcmEncoder.prepare()
        pcmEncoder.encode(inputStream, sampleRate)
        pcmEncoder.stop()
    }
}

/**
 * Creates a file with the current date and the given extension. If the file already exists, it is overwritten.
 * @param fileExtension: the file extension used to create the file.
 *
 * @return: created an empty File
 */
private fun getOutFile(fileString: String, fileExtension: String): File {
    val outFile = File(getVoiceGymFolder(), "$fileString.$fileExtension")
    if (outFile.exists()) outFile.delete()
    outFile.createNewFile()
    if (!outFile.canWrite()) throw Error("Cannot write file")
    return outFile
}


/* Checks if external storage is available for read and write */
fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

/* Checks if external storage is available to at least read */
fun isExternalStorageReadable(): Boolean {
    return Environment.getExternalStorageState() in setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}

fun getVoiceGymFolder(): File? {
    // Get the directory for the user's public pictures directory.
    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VoiceGym")
    if (!file.exists()) {
        if (!file.mkdirs()) throw Error("Error creating VoiceGym folder")
    }
    return file
}

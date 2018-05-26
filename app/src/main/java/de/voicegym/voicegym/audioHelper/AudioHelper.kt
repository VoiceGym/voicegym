package de.voicegym.voicegym.audioHelper

import android.os.Environment
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getDezibelFromAmplitude(amplitude: Double): Double = 20 * Math.log10(amplitude)


fun savePCMInputStreamOnSDCard(pcmStorage: InputStream, sampleRate: Int, bitRate: Int) {
    if (isExternalStorageWritable()) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(Calendar.getInstance().time)
        val pcmEncoder = PCMEncoder(bitRate, sampleRate, 1)
        val outFile = File(getVoiceGymFolder(), timestamp + ".m4a")
        if (outFile.exists()) outFile.delete()
        outFile.createNewFile()
        if (!outFile.canWrite()) throw Error("Cannot write file")
        pcmEncoder.outputPath = outFile.path
        pcmEncoder.prepare()
        pcmEncoder.encode(pcmStorage, sampleRate)
        pcmEncoder.stop()
    }
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

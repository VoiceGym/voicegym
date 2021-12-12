package de.voicegym.voicegym.util.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioRecord
import android.media.AudioRecord.STATE_INITIALIZED
import android.media.AudioRecord.getMinBufferSize
import android.media.MediaRecorder.AudioSource.MIC
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.setThreadPriority
import de.voicegym.voicegym.menu.settings.SettingsBundle.audioFormat
import de.voicegym.voicegym.menu.settings.SettingsBundle.channelConfig
import de.voicegym.voicegym.menu.settings.SettingsBundle.sampleRate
import de.voicegym.voicegym.util.RecordBufferListener
import java.lang.Thread.sleep

class RecordHelper(private val preferredBufferSize: Int, private val appContext: Context) {


    private val bytesPerBufferSlot = when (audioFormat) {
        ENCODING_PCM_16BIT -> 2
        else -> throw Error("Unsupported AudioFormat")
    }

    // we are using the number of slots as a bufferSize, and recalculate it for the recordObject
    private val minimumBufferSize = getMinBufferSize(sampleRate, channelConfig, audioFormat) / bytesPerBufferSlot

    // preferredBufferSize is possible (only impossible if too small)
    private val bufferSize = if (preferredBufferSize > minimumBufferSize) preferredBufferSize else minimumBufferSize

    // private array that records the pcm samples, only copies of this shall be handed to the listeners
    private val audioBuffer = ShortArray(bufferSize)

    // list of all objects interested in obtaining copies of the audioBuffer
    private val listenerList = ArrayList<RecordBufferListener>()

    // the object that obtains pcm samples from the microphone
    private var recordObject: AudioRecord? = null

    // control variable to stop the background thread that collects samples from the microphone
    var shouldRecord: Boolean = true
        private set

    // Functions
    fun hasPreferredSize(): Boolean = bufferSize == preferredBufferSize

    /**
     * Add listeners that shall receive data from the recordThread
     */
    @Synchronized
    fun subscribeListener(listener: RecordBufferListener) {
        if (listener.canHandleBufferSize(bufferSize)) listenerList.add(listener)
    }

    @Synchronized
    fun unSubscribeListener(listener: RecordBufferListener) {
        if (listenerList.contains(listener)) listenerList.remove(listener)
    }

    private var recordingThread = Thread(Runnable {
        setup()
        record()
    })

    fun start() {
        recordingThread.start()
    }

    var paused = false


    private fun setup() {
        setThreadPriority(THREAD_PRIORITY_AUDIO)
        if (appContext.checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
            throw RuntimeException("Error receiving permission")
        }
        recordObject = AudioRecord(MIC, sampleRate, channelConfig, audioFormat, bufferSize * bytesPerBufferSlot)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw RuntimeException("Error while setting up buffer")
        }

        if (recordObject == null || recordObject?.state != STATE_INITIALIZED) {
            throw RuntimeException("AudioRecord object not correctly initialized")
        }
    }

    @Synchronized
    private fun distributeArray(array: ShortArray) {
        listenerList.forEach { it.onBufferReady(array) }
    }

    private fun record() {
        recordObject?.startRecording()
        while (shouldRecord) {
            if (paused) {
                sleep(100)
            } else {
                // locks the thread while reading from microphone
                recordObject?.read(audioBuffer, 0, bufferSize)
                // pass copies on to the listeners
                distributeArray(audioBuffer.copyOf())
            }
        }
        recordObject?.stop()
        recordObject?.release()
        recordObject = null
    }

    fun stopRecording() {
        shouldRecord = false
        recordingThread.join()
    }

}

package de.voicegym.voicegym.util.audio

import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioRecord
import android.media.AudioRecord.STATE_INITIALIZED
import android.media.AudioRecord.getMinBufferSize
import android.media.MediaRecorder.AudioSource.MIC
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.setThreadPriority
import android.util.Log
import de.voicegym.voicegym.menu.settings.SettingsBundle.audioFormat
import de.voicegym.voicegym.menu.settings.SettingsBundle.channelConfig
import de.voicegym.voicegym.menu.settings.SettingsBundle.sampleRate
import de.voicegym.voicegym.util.RecordBufferListener

class RecordHelper(private val preferredBufferSize: Int) {


    private val bytesPerBufferSlot = when (audioFormat) {
        ENCODING_PCM_16BIT -> 2
        else               -> throw Error("Unsupported AudioFormat")
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

    init {
        Log.e("RecordHelper", "Recordhelper initialized")
    }

    // Functions
    fun hasPreferredSize(): Boolean = bufferSize == preferredBufferSize

    /**
     * Add listeners that shall receive data from the recordThread
     */
    fun subscribeListener(listener: RecordBufferListener) {
        if (listener.canHandleBufferSize(bufferSize)) listenerList.add(listener)
    }

    fun unSubscribeListener(listener: RecordBufferListener) {
        if (listenerList.contains(listener)) listenerList.remove(listener)
    }

    private var recordingThread = Thread(Runnable {
        Log.i("Recordhelper", "Starting Thread")
        setup()
        record()
        Log.i("Recordhelper", "Thread Done")
    })

    fun start() {
        recordingThread.start()
    }

    private fun setup() {
        setThreadPriority(THREAD_PRIORITY_AUDIO)
        Log.i("RecordObject", "Trying to get a hold of the microphone")
        recordObject = AudioRecord(MIC, sampleRate, channelConfig, audioFormat, bufferSize * bytesPerBufferSlot)
        Log.i("RecordObject", "Got the microphone")
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw RuntimeException("Error while setting up buffer")
        }

        if (recordObject == null || recordObject?.state != STATE_INITIALIZED) {
            throw RuntimeException("AudioRecord object not correctly initialized")
        }
    }


    private fun record() {
        recordObject?.startRecording()
        while (shouldRecord) {
            // locks the thread while reading from microphone
            recordObject?.read(audioBuffer, 0, bufferSize)
            // pass copies on to the listeners
            listenerList.forEach { it.onBufferReady(audioBuffer.copyOf()) }
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

package de.voicegym.voicegym.audioHelper

import android.media.AudioFormat.CHANNEL_IN_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioFormat.ENCODING_PCM_8BIT
import android.media.AudioRecord
import android.media.AudioRecord.STATE_INITIALIZED
import android.media.AudioRecord.getMinBufferSize
import android.media.MediaRecorder.AudioSource.MIC
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.setThreadPriority
import android.util.Log

class RecordHelper(val preferredBufferSize: Int) {

    // Config, could be in constructor
    val sampleRate = 44100
    val channelConfig = CHANNEL_IN_MONO
    val audioFormat = ENCODING_PCM_16BIT

    // depending on config
    val bytesPerBufferSlot = when (audioFormat) {
        ENCODING_PCM_8BIT -> 1
        ENCODING_PCM_16BIT -> 2
        else -> throw Error("Unsupported AudioFormat")
    }
    // we are using the number of slots as a bufferSize, and recalculate it for the recordObject
    val minimumBufferSize = getMinBufferSize(sampleRate, channelConfig, audioFormat) / bytesPerBufferSlot


    val bufferSize = if (preferredBufferSize > minimumBufferSize) preferredBufferSize else minimumBufferSize

    private var recordObject: AudioRecord? = null
    private var audioBuffer = ShortArray(bufferSize)
    private var shouldRecord: Boolean = true

    private val listenerList = ArrayList<RecordBufferListener>()

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


    private var recordingThread = Thread(
            Runnable {
                Log.i("Recordhelper", "Starting Thread")
                setup()
                record()
                Log.i("Recordhelper", "Thread Done")
            });

    fun start() {
        recordingThread.start()
    }

    private fun setup() {
        setThreadPriority(THREAD_PRIORITY_AUDIO);
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
            // pass it on to the listeners
            listenerList.forEach { item -> item.onBufferReady(audioBuffer) }
        }
        recordObject?.stop()
        recordObject?.release()
        recordObject = null
    }

    fun stopRecording() {
        shouldRecord = false
    }

}
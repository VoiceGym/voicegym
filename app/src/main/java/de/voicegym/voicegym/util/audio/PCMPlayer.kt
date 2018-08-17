package de.voicegym.voicegym.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import org.jetbrains.anko.runOnUiThread
import java.lang.Thread.sleep
import java.nio.ShortBuffer
import kotlin.concurrent.thread


class PCMPlayer(val sampleRate: Int, private val buffer: ShortBuffer, val context: Context) {

    private var currentPosition: Int = 0
    var playing: Boolean = false

    private val player: AudioTrack
    private var playerThread: Thread? = null
    private val playBuffer: ShortArray
    private val subscribers = ArrayList<PCMPlayerListener>()

    init {
        val minBufSizeOrError = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minBufferSize = when (minBufSizeOrError) {
            AudioTrack.ERROR, AudioTrack.ERROR_BAD_VALUE ->
                sampleRate * 2
            else                                         ->
                minBufSizeOrError
        }
        playBuffer = ShortArray(minBufferSize)
        player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                    .setBufferSizeInBytes(minBufferSize)
                    .setAudioFormat(AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build()
        } else {
            @Suppress("DEPRECATION") // not deprecated for SDK Versions < 26 and alternatives not available for api level 19
            AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM)
        }
    }

    fun play() {
        if (!playing) {
            playing = true
            if (currentPosition <= buffer.capacity() && currentPosition >= 0) {
                buffer.position(currentPosition)
            } else throw IndexOutOfBoundsException("Cannot seek to position not within range")

            if (currentPosition > 0.99 * buffer.capacity()) {
                // reset
                buffer.position(0)
                currentPosition = 0
                informListeners(0)
            }

            playerThread = thread {
                while (listenersNotReady()) {
                    sleep(100)
                }
                player.play()
                while (playing) {
                    when {
                        buffer.position() + playBuffer.size < buffer.capacity() -> {
                            buffer.get(playBuffer)
                            player.write(playBuffer, 0, playBuffer.size)
                        }
                        buffer.position() < buffer.capacity() - 1               -> {
                            playBuffer.fill(0)
                            buffer.get(playBuffer, 0, buffer.capacity() - buffer.position())
                            player.write(playBuffer, 0, playBuffer.size)
                            playing = false
                        }
                        else                                                    -> playing = false
                    }
                    currentPosition = buffer.position()
                    informListeners(currentPosition)
                }
            }
        }
    }

    fun stop() {
        playing = false
        playerThread?.join()
    }

    fun seekTo(sampleNumber: Int): Int {
        val wasPlaying: Boolean = if (playing) {
            stop()
            true
        } else {
            false
        }

        currentPosition = when {
            sampleNumber < 0                  -> 0
            sampleNumber >= buffer.capacity() -> buffer.capacity() - 1
            else                              -> sampleNumber
        }

        if (wasPlaying) play()
        return currentPosition
    }

    fun seekToRelative(samples: Int): Int {
        return seekTo(currentPosition + samples)
    }

    fun destroy() {
        stop()
        player.release()
    }

    fun subscribeListener(subscriber: PCMPlayerListener) = subscribers.add(subscriber)

    fun unSubscribeListener(subscriber: PCMPlayerListener) = subscribers.remove(subscriber)

    private fun informListeners(position: Int) = subscribers.forEach { listener ->
        context.runOnUiThread { listener.isAtPosition(position) }
    }

    private fun listenersNotReady(): Boolean {
        var readyOrNot = true
        subscribers.forEach { if (!it.isReady()) readyOrNot = false }
        return !readyOrNot
    }

}


interface PCMPlayerListener {
    /**
     * Callback method so the PCMPlayer can inform a subscriber that it currently plays
     * @param sampleNumber: the sampleNumber currently played
     */
    fun isAtPosition(sampleNumber: Int)

    /**
     * PCMPlayer shall only play when all subscribers are in a state to receive new positions
     */
    fun isReady(): Boolean
}

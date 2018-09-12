package de.voicegym.voicegym

import de.voicegym.voicegym.util.audio.PCMStorage
import junit.framework.Assert.assertEquals
import org.junit.Test

class TestPCMStorage {
    @Test
    fun createPCMStorageAndGetInputStream() {
        val storage = PCMStorage(44100)
        assertEquals(44100, storage.sampleRate)

        val shortArray = ShortArray(1) { 256 }
        storage.onBufferReady(shortArray)

        storage.stopListening()

        assertEquals(0, storage.read())
        assertEquals(1, storage.read())
        assertEquals(-1, storage.read())
    }


    @Test
    fun testOrderOfSamples() {
        val storage = PCMStorage(44100)

        val arr1 = ShortArray(1000) { it.toShort() }
        val arr2 = ShortArray(1000) { (it + 1000).toShort() }
        val arr3 = ShortArray(1000) { (it + 2000).toShort() }

        storage.onBufferReady(arr1)
        storage.onBufferReady(arr2)
        storage.onBufferReady(arr3)

        storage.stopListening()
        val buffer=storage.asShortBuffer().array()
        for (i in 0 until 3000) {
            assertEquals(i, buffer[i].toInt())
        }
        System.out.print("First test complete, rewinding and testing again")

        storage.rewind()

        val rewoundbuffer=storage.asShortBuffer().array()
        for (i in 0 until 3000) {
            System.out.println(i)
            assertEquals(i, rewoundbuffer[i].toInt())
        }
    }

}

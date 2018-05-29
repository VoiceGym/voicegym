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

}

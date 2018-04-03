package de.voicegym.voicegym

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */


   // external fun fftw_create_plan(): fftw_plan

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("fftw3")
        }
    }
}

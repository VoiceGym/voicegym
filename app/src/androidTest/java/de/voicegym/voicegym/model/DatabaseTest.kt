package de.voicegym.voicegym.model

import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DatabaseTest {

    companion object {
        @JvmStatic
        private lateinit var db: AppDatabase

        @BeforeClass
        @JvmStatic
        fun initialize() {
            //            db = Room.inMemoryDatabaseBuilder(
            //                    InstrumentationRegistry.getContext(),
            //                    AppDatabase::class.java)
            //                    .build()

            db = AppDatabase.getInstance(InstrumentationRegistry.getTargetContext())!!
        }

        @AfterClass
        @JvmStatic
        fun closeDb() {
            db.close()
        }
    }

    @Test
    fun insertAndGetUser() {
        val recording = Recording(
                123456789L,
                "VoiceGym/abc.m4a",
                1527594386,
                0,
                0
        )
        // When inserting a new user in the data source
        db.recordingDao().insert(recording)

        //The user can be retrieved
        //TODO FIX TEST
        /*
        val recordings = db.recordingDao().getAll()
        assertThat(recordings.size).isEqualTo(1)
        val dbRecording = recordings.get(0)
        assertThat(dbRecording.id).isEqualTo(recording.id)
        assertThat(dbRecording.fileName).isEqualTo(recording.fileName)
        */
    }


}

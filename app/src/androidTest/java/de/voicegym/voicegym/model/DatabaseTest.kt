package de.voicegym.voicegym.model

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DatabaseTest {

    companion object {
        @JvmStatic
        private lateinit var db:AppDatabase

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
        val RECORDING = Recording(
                123456789L,
                "VoiceGym/abc.m4a",
                1527594386L
        )
        // When inserting a new user in the data source
        db.recordingDao().insert(RECORDING)

        //The user can be retrieved
        val recordings = db.recordingDao().getAll()
        assertThat(recordings.size).isEqualTo(1)
        val dbRecording = recordings.get(0)
        assertThat(dbRecording.id).isEqualTo( RECORDING.id)
        assertThat(dbRecording.fileName).isEqualTo(RECORDING.fileName)
    }


}

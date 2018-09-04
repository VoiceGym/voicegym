package de.voicegym.voicegym.model

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import java.util.Calendar

@Entity(tableName = "recordings")
data class Recording(
        @PrimaryKey(autoGenerate = true)
        var id: Long?,
        @ColumnInfo(name = "fileName")
        var fileName: String,
        @ColumnInfo(name = "duration")
        var duration: Int,
        @ColumnInfo(name = "created_at")
        var createdAt: Long,
        @ColumnInfo(name = "updated_at")
        var updatedAt: Long,
        @ColumnInfo(name = "rating")
        var rating: Int,
        @ColumnInfo(name = "title")
        var title: String?) {

    constructor() : this(
            null,
            "",
            0,
            Calendar.getInstance().timeInMillis,
            Calendar.getInstance().timeInMillis,
            0,
            null)
}

@Dao
interface RecordingDao {

    @Query("SELECT * from recordings")
    fun getAll(): LiveData<List<Recording>>

    @Insert(onConflict = REPLACE)
    fun insert(recording: Recording)

    @Query("Select * from recordings where filename=:filename")
    fun getByFileName(filename: String): Recording?

    @Delete
    fun delete(recording: Recording)
}

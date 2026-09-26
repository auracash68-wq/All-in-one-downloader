package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND isPrivate = 0 AND fileSizeBytes > 0 ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND isPrivate = 0 AND mediaType = :type AND fileSizeBytes > 0 ORDER BY timestamp DESC")
    fun getDownloadsByType(type: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND isPrivate = 1 AND fileSizeBytes > 0 ORDER BY timestamp DESC")
    fun getPrivateDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND fileSizeBytes > 0 ORDER BY timestamp DESC")
    suspend fun getCompletedList(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' ORDER BY timestamp DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'COMPLETED' AND isPrivate = 0 AND fileSizeBytes > 0")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DownloadEntity>)

    @Update
    suspend fun update(item: DownloadEntity)

    @Delete
    suspend fun delete(item: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}

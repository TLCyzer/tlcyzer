package de.uni.tuebingen.tlceval.data.daos

import androidx.room.*
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.CaptureAndRect
import de.uni.tuebingen.tlceval.data.CaptureAndSpots
import de.uni.tuebingen.tlceval.data.CaptureFullInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM capture")
    fun getAll(): Flow<List<Capture>>

    @Query("SELECT * FROM capture ORDER BY timestamp DESC")
    fun getAllSortedByTimestampDescending(): Flow<List<Capture>>

    @Query("SELECT * FROM capture ORDER BY agent_name ASC")
    fun getAllSortedByNameAscending(): Flow<List<Capture>>

    @Query("SELECT * FROM capture ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<Capture>

    @Query("SELECT * FROM capture ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOnce(): Capture?

    @Query("SELECT * FROM capture WHERE timestamp LIKE :timestamp LIMIT 1")
    suspend fun findByTimestamp(timestamp: Long): Capture?

    @Query("SELECT * FROM capture WHERE agent_name LIKE :agentName")
    fun findByAgentName(agentName: String): Flow<List<Capture>>

    @Transaction
    @Query("SELECT * FROM capture WHERE timestamp IN (:captureTimestamps)")
    suspend fun loadRectByTimestamps(captureTimestamps: LongArray): List<CaptureAndRect>

    @Transaction
    @Query("SELECT * FROM capture WHERE timestamp IN (:captureTimestamps)")
    suspend fun loadFullInfoByTimestamps(captureTimestamps: LongArray): List<CaptureFullInfo>

    @Transaction
    @Query("SELECT * FROM capture WHERE timestamp IN (:captureTimestamps)")
    suspend fun loadSpotByTimestamps(captureTimestamps: LongArray): List<CaptureAndSpots>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg captures: Capture)

    @Update
    suspend fun updateCaptures(vararg captures: Capture)

    @Delete
    suspend fun delete(capture: Capture)

}
package de.uni.tuebingen.tlceval.data.daos

import androidx.room.*
import de.uni.tuebingen.tlceval.data.Spot

@Dao
interface SpotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg spots: Spot)

    @Update
    suspend fun updateCaptures(vararg spot: Spot)

    @Delete
    suspend fun delete(spot: Spot)
}
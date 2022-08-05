package de.uni.tuebingen.tlceval.data.daos

import androidx.room.*
import de.uni.tuebingen.tlceval.data.Rect

@Dao
interface RectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg rects: Rect)

    @Update
    suspend fun updateCaptures(vararg rect: Rect)

    @Delete
    suspend fun delete(rect: Rect)
}
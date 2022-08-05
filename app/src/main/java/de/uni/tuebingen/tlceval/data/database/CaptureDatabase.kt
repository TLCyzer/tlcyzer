package de.uni.tuebingen.tlceval.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.Rect
import de.uni.tuebingen.tlceval.data.Spot
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import de.uni.tuebingen.tlceval.data.daos.RectDao
import de.uni.tuebingen.tlceval.data.daos.SpotDao


@Database(
    entities = [Capture::class, Rect::class, Spot::class], version = 1,
)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun spotDao(): SpotDao
    abstract fun rectDao(): RectDao
}
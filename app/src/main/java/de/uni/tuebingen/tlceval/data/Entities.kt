package de.uni.tuebingen.tlceval.data

import androidx.room.*
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class Capture(
    @PrimaryKey
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "crop_path") val cropPath: String?,
    @ColumnInfo(name = "background_subtract_path") val backgroundSubtractPath: String?,
    @ColumnInfo(name = "agent_name") val agentName: String?,
    @ColumnInfo(name = "has_dark_spots") val hasDarkSpots: Boolean?,
)

fun Capture.formatTimestamp(): String {
    val calendar = Calendar.getInstance()
    val tz = TimeZone.getDefault()
    calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.timeInMillis));
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    val currenTimeZone = Date(timestamp);

    return sdf.format(currenTimeZone)
}

@Entity
data class Rect(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "capture_timestamp")
    val captureTimestamp: Long,
    @Embedded(prefix = "tl_") val top_left: Point,
    @Embedded(prefix = "tr_") val top_right: Point,
    @Embedded(prefix = "br_") val bottom_right: Point,
    @Embedded(prefix = "bl_") val bottom_left: Point,
    val orientation: Int,
)

data class Point(
    val x: Int,
    val y: Int,
)

@Entity
data class Spot(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    @ColumnInfo(name = "capture_timestamp")
    val captureTimestamp: Long,
    @Embedded val center: Point,
    val radius: Int,
    @ColumnInfo(name = "integration_value")
    val integrationValue: Int,
    val percentage: Float,
    @ColumnInfo(name = "is_reference")
    val isReference: Boolean,
)

data class CaptureAndRect(
    @Embedded val capture: Capture,
    @Relation(
        parentColumn = "timestamp",
        entityColumn = "capture_timestamp"
    )
    val rect: Rect?
)

data class CaptureAndSpots(
    @Embedded val capture: Capture,
    @Relation(
        parentColumn = "timestamp",
        entityColumn = "capture_timestamp"
    )
    val spot: List<Spot>
)

data class CaptureFullInfo(
    @Embedded val capture: Capture,
    @Relation(
        parentColumn = "timestamp",
        entityColumn = "capture_timestamp"
    )
    val spot: List<Spot>,
    @Relation(
        parentColumn = "timestamp",
        entityColumn = "capture_timestamp"
    )
    val rect: Rect?
)
package de.uni.tuebingen.tlceval.data

import kotlinx.serialization.Serializable

@Serializable
data class PointInformation(
    val x: Int,
    val y: Int
)

@Serializable
data class RectInformation(
    val top_left: PointInformation,
    val top_right: PointInformation,
    val bottom_right: PointInformation,
    val bottom_left: PointInformation
)


@Serializable
data class BlobInformation(
    val id: Int,
    val x: Int,
    val y: Int,
    val radius: Int,
    val integrated: Int,
    val percentage: Float
)

@Serializable
data class CaptureMetaInformation(
    val name: String,
    val rect: RectInformation,
    val blobs: List<BlobInformation>
)





package de.uni.tuebingen.tlceval.custom_views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

data class CropRect(
    var topLeft: PointF,
    var topRight: PointF,
    var bottomRight: PointF,
    var bottomLeft: PointF
) {
    constructor() : this(PointF(), PointF(), PointF(), PointF())
}


fun CropRect.sortFromList(points: List<PointF>) {
    val ySorted = points.sortedBy { it.y }

    if (ySorted[0].x > ySorted[1].x) {
        topLeft = ySorted[1]
        topRight = ySorted[0]
    } else {
        topLeft = ySorted[0]
        topRight = ySorted[1]
    }

    if (ySorted[2].x > ySorted[3].x) {
        bottomLeft = ySorted[3]
        bottomRight = ySorted[2]
    } else {
        bottomLeft = ySorted[2]
        bottomRight = ySorted[3]
    }
}

fun CropRect.toList(): List<PointF> {
    return listOf(topLeft, topRight, bottomRight, bottomLeft)
}

fun euclideanDistance(p: PointF, p2: PointF): Float {
    return sqrt((p.x - p2.x).pow(2f) + (p.y - p2.y).pow(2f))
}


enum class CornerSelect(val pos: Int) {
    TOP_LEFT(0),
    TOP_RIGHT(1),
    BOTTOM_RIGHT(2),
    BOTTOM_LEFT(3),
    NOT_SELECTED(-1)
}

// TODO this view is quite far away from anything resembling MVC...
class CropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, val onOrientationSet: (Int) -> Unit
) : SubsamplingScaleImageView(context, attrs), View.OnTouchListener {
    private val paint = Paint()

    // Order should be: TopLeft, TopRight, BottomRight, BottomLeft
    var sPoints: CropRect? = null
        private set

    private var vPoints: CropRect = CropRect() //TODO: Keep points retained
    private var selectedPoint = CornerSelect.NOT_SELECTED
    private var strokeWidth: Int = 0
    private var maxDistanceAllowed: Int = 0

    var currentRotationValue = 0
    var calledOrientationListener = false


    init {
        initialize()
    }

    private fun initialize() {
        setOnTouchListener(this)
        val density = resources.displayMetrics.densityDpi
        strokeWidth = (density / 100f).toInt()
    }

    fun setSourcePoints(cropRect: CropRect) {
        sPoints = cropRect
        invalidate()
    }

    fun getWidthHeightForOrientation(): Pair<Int, Int> {
        return when (this.orientation) {
            0, 180 -> {
                Pair(sWidth, sHeight)
            }
            else -> {
                Pair(sHeight, sWidth)
            }
        }
    }

    fun setRotation(rotateDirection: Int, orientation: Int, rotationValue: Int) {
        if (isReady && currentRotationValue != rotationValue) {
            Timber.d("Changing rotation! $rotateDirection, $orientation, $rotationValue -- $currentRotationValue")

            sPoints?.let { ps ->
                val matrix = Matrix()
                matrix.reset()

                val (rx, ry) = getWidthHeightForOrientation()

                // Move to 0,0 in old rotation
                matrix.postTranslate(-rx / 2f, -ry / 2f)
                // Rotate
                matrix.postRotate(rotateDirection.toFloat())
                // Move to correct position in current rotation
                matrix.postTranslate(ry / 2f, rx / 2f)

                for (p in ps.toList()) {
                    val pts = floatArrayOf(p.x, p.y)
                    matrix.mapPoints(pts)

                    p.set(pts[0], pts[1])
                }
            }
            this.orientation = orientation
            currentRotationValue = rotationValue
            invalidate()
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady) {
            return
        }

        if (this.orientation == ORIENTATION_USE_EXIF && !calledOrientationListener) {
            this.orientation = this.appliedOrientation
            this.currentRotationValue = this.appliedOrientation
            this.onOrientationSet(this.appliedOrientation)
            calledOrientationListener = true
        }

        paint.isAntiAlias = true

        // Validate points. All inside image
        val wh = getWidthHeightForOrientation()
        sPoints = sPoints?.let { crect ->
            val plist = crect.toList().map {
                if (it.x < 0) {
                    it.x = 0f
                }
                if (it.y < 0) {
                    it.y = 0f
                }
                if (it.x >= wh.first) {
                    it.x = (wh.first - 1).toFloat()
                }
                if (it.y >= wh.second) {
                    it.y = (wh.second - 1).toFloat()
                }
                it
            }
            crect.sortFromList(plist)
            crect
        }

        sPoints?.let { maybeRect ->
            val viewPointList =
                maybeRect.toList().zip(vPoints.toList()).map { (s, t) -> sourceToViewCoord(s, t) }
                    .filterNotNull()

            if (viewPointList.size == 4) {
                val viewPointListCircle = listOf(*viewPointList.toTypedArray(), viewPointList[0])

                val radius = scale * sWidth * 0.025f
                maxDistanceAllowed = (radius * 1.5f).toInt()

                viewPointListCircle.withIndex().zipWithNext { a, b ->
                    paint.style = Paint.Style.STROKE
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeWidth = strokeWidth * 2f

                    paint.color = when (a.index) {
                        selectedPoint.pos -> Color.GREEN
                        else -> Color.WHITE
                    }
                    canvas.drawCircle(a.value.x, a.value.y, radius, paint)
                    paint.color = when (b.index) {
                        selectedPoint.pos -> Color.GREEN
                        else -> Color.WHITE
                    }
                    canvas.drawCircle(b.value.x, b.value.y, radius, paint)

                    paint.strokeWidth = strokeWidth.toFloat()
                    paint.color = Color.WHITE

                    canvas.drawLine(a.value.x, a.value.y, b.value.x, b.value.y, paint)
                }
            }
        }

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false
        val prevSelection = selectedPoint
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only capture a single input
                if (event.actionIndex == 0) {
                    val touchPoint = PointF(event.x, event.y)

                    // Check if edge point is within range
                    for ((idx, p) in vPoints.toList().withIndex()) {
                        val distance = euclideanDistance(p, touchPoint)
                        if (distance < maxDistanceAllowed) {    // If so
                            // Select it for moving
                            selectedPoint = CornerSelect.values()[idx]
                        }
                        // Else ignore
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedPoint != CornerSelect.NOT_SELECTED && sPoints != null) {
                    consumed = true
                    val touchPoint = PointF(event.x, event.y)
                    val sTouchPoint = viewToSourceCoord(touchPoint)!!

                    when (selectedPoint) {
                        CornerSelect.TOP_LEFT -> {
                            sPoints!!.topLeft = sTouchPoint
                        }
                        CornerSelect.TOP_RIGHT -> {
                            sPoints!!.topRight = sTouchPoint
                        }
                        CornerSelect.BOTTOM_RIGHT -> {
                            sPoints!!.bottomRight = sTouchPoint
                        }
                        CornerSelect.BOTTOM_LEFT -> {
                            sPoints!!.bottomLeft = sTouchPoint
                        }
                        else -> {
                            return super.onTouchEvent(event)
                        }
                    }
                    invalidate()
                }
                // Moving

            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                selectedPoint = CornerSelect.NOT_SELECTED
                val listOfPoints = sPoints!!.toList()
                sPoints?.sortFromList(listOfPoints)
                invalidate()
            }
            else -> {
                // Do not care
            }
        }

        if (prevSelection != selectedPoint) {
            invalidate()
        }

        return consumed || super.onTouchEvent(event)
    }
}

package de.uni.tuebingen.tlceval.custom_views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.uni.tuebingen.tlceval.features.processor.spot.Circle

interface BlobSelection {
    fun selected(id: Int)
    fun deselect()
    fun moved(x: Int, y: Int)
}

// TODO this view is quite far away from anything resembling MVC...
class BlobSetterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val onDimensionAvailable: (Int, Int, Int) -> Unit
) : SubsamplingScaleImageView(context, attrs), View.OnTouchListener {
    private val paint = Paint()

    var sCircles: Map<Int, Circle> = emptyMap()
        private set

    private var vCircles: MutableMap<Int, Pair<PointF, Float>> = mutableMapOf()
    private var selectedCircle: Int? = null
    var selectionListener: BlobSelection? = null

    private var maxDistanceAllowed: Int = 0

    private var strokeWidth: Int = 0
    private var defaultSpotSize: Int = 0

    fun setDefaultSpotSize(value: Int) {
        defaultSpotSize = value
    }

    fun setStrokeWidth(value: Int) {
        strokeWidth = value
    }


    init {
        initialize()
    }

    private fun initialize() {
        setOnTouchListener(this)

    }

    fun setSourceCircles(circleMap: Map<Int, Circle>) {
        sCircles = circleMap
        vCircles.clear()
        for ((key, value) in sCircles) {
            vCircles[key] = Pair(PointF(), value.radius.toFloat())
        }
        invalidate()
    }

    fun setSelection(id: Int?) {
        selectedCircle = id
        invalidate()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return false
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady) {
            return
        }

        val density = resources.displayMetrics.densityDpi
        onDimensionAvailable(sHeight, sWidth, density)

        paint.isAntiAlias = true

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = strokeWidth * 2f

        // Validate circle center inside image
        val wh = getWidthHeightForOrientation()
        sCircles = sCircles.let {
            sCircles.mapValues {
                var coords = it.value

                if (coords.x <0) {
                    coords = coords.copy(x = 0)
                }
                if (coords.y <0) {
                    coords = coords.copy(y = 0)
                }
                if (coords.x >= wh.first) {
                    coords = coords.copy(x = wh.first - 1)
                }
                if (coords.y >= wh.second) {
                    coords = coords.copy(y = wh.second - 1)
                }

                coords
            }
        }

        for ((key, circle) in sCircles) {
            paint.color = when (key) {
                selectedCircle -> Color.RED
                else -> Color.WHITE
            }

            maxDistanceAllowed = (defaultSpotSize * 1.25f).toInt()

            if (vCircles.containsKey(key)) {
                //Set the center
                sourceToViewCoord(circle.x.toFloat(), circle.y.toFloat(), vCircles[key]!!.first)
                //Now calculate the radius
                val vRadius = circle.radius * scale

                vCircles[key] = vCircles[key]!!.copy(second = vRadius)

                canvas.drawCircle(
                    vCircles[key]!!.first.x,
                    vCircles[key]!!.first.y,
                    vCircles[key]!!.second,
                    paint
                )
            }
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var consumed = false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Only capture a single input
                if (event.actionIndex == 0) {
                    val touchPoint = PointF(event.x, event.y)

                    val selectedId = findClosestvCircle(touchPoint)
                    if (selectedId != null) {
                        selectedCircle = selectedId
                        selectionListener?.selected(selectedId)
                        invalidate()
                    } else {
                        if (selectedCircle != null) {
                            selectionListener?.deselect()
                        }
                        selectedCircle = null
                        invalidate()

                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // We need to have something selected
                if (selectedCircle != null) { // TODO: only if touched for a specific time
                    val touchPoint = PointF(event.x, event.y)
                    val sTouchPoint = viewToSourceCoord(touchPoint)!!
                    val touchedId = findClosestsCircle(sTouchPoint)

                    //Make sure that we are touching the correct blob
                    if (touchedId == selectedCircle) {
                        consumed = true
                        selectionListener?.moved(sTouchPoint.x.toInt(), sTouchPoint.y.toInt())
                    }
                }


            }
        }

        return consumed || super.onTouchEvent(event)
    }

    private fun findClosestvCircle(touchPoint: PointF): Int? {
        if (vCircles.isEmpty()) return null
        val distances: List<Pair<Int, Float>> = vCircles.map { (id, circle) ->
            val (center, _) = circle
            val distance = euclideanDistance(center, touchPoint)
            Pair(id, distance)
        }.sortedBy { (_, distance) -> distance }
        val selectedItem = distances[0]

        return if (selectedItem.second <= maxDistanceAllowed) {
            distances[0].first
        } else {
            null
        }
    }

    private fun findClosestsCircle(touchPoint: PointF): Int? {
        if (sCircles.isEmpty()) return null
        val distances: List<Pair<Int, Float>> = sCircles.map { (id, circle) ->
            val center = PointF(circle.x.toFloat(), circle.y.toFloat())
            val distance = euclideanDistance(center, touchPoint)
            Pair(id, distance)
        }.sortedBy { (_, distance) -> distance }
        val selectedItem = distances[0]

        return if (selectedItem.second <= maxDistanceAllowed) {
            distances[0].first
        } else {
            null
        }
    }
}
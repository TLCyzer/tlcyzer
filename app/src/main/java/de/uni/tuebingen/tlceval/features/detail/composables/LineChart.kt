package de.uni.tuebingen.tlceval.features.detail.composables

import android.content.Context
import android.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import de.uni.tuebingen.tlceval.R
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import kotlin.math.roundToInt

fun List<Pair<Int, Float>>.toLineData(name: String): LineDataSet {
    val entries = this.map {
        Entry(it.first.toFloat(), it.second)
    }
    Timber.d("$this")
    Timber.d("$entries")

    return LineDataSet(entries.sortedBy { it.x }, name)
}

fun LineDataSet.toLineData(context: Context): LineData {
    Timber.d("$this")
    this.lineWidth = 1.75f
    this.circleRadius = 5f
    this.circleHoleRadius = 2.5f
    this.color = ContextCompat.getColor(context, R.color.transparent_white)
    this.setCircleColor(Color.WHITE)
    this.highLightColor = Color.WHITE
    this.setDrawValues(true)
    val formatter = object : ValueFormatter() {
        override fun getPointLabel(entry: Entry?): String {
            if (entry != null) {
                return "${entry.y.roundToInt()}%"
            }
            return super.getPointLabel(entry)
        }
    }
    this.valueTypeface = ResourcesCompat.getFont(context, R.font.oxygen_bold)
    this.valueTextSize = 14f
    this.valueTextColor = Color.WHITE
    this.valueFormatter = formatter

    return LineData(this)
}

private fun setupChart(chart: LineChart, data: LineData, maxVal: Float) {
    chart.description.isEnabled = false
    chart.setDrawGridBackground(false)
    chart.setTouchEnabled(false)
    chart.isDragEnabled = false
    chart.setScaleEnabled(false)
    chart.setPinchZoom(false)
    chart.setBackgroundColor(
        ContextCompat.getColor(
            chart.context,
            R.color.immersive_bars
        )
    )
    chart.setViewPortOffsets(0f, 0f, 0f, 0f)
    chart.data = data

    val l = chart.legend
    l.isEnabled = false

    chart.axisLeft.isEnabled = false
    chart.axisRight.isEnabled = false
    chart.xAxis.isEnabled = false
    chart.xAxis.resetAxisMaximum()
    chart.xAxis.resetAxisMinimum()
    chart.xAxis.axisMinimum = 0f
    chart.xAxis.axisMaximum = maxVal

    chart.animateY(500)
}

@ExperimentalAnimationApi
@Composable
fun LineChartCompose(
    chartDataFlow: Flow<List<Pair<Int, Float>>>,
    maxWidth: Int?,
    chartVisibleFlow: Flow<Boolean>,
    modifier: Modifier,
) {
    val chartData by chartDataFlow.collectAsState(initial = null)
    val chartVisible by chartVisibleFlow.collectAsState(initial = false)

    Timber.d("CD: $chartData")
    Timber.d("MW: $maxWidth")

    AnimatedVisibility(visible = chartVisible) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                LineChart(context)
            },
            update = {
                maxWidth?.let { mw ->
                    chartData?.toLineData(it.context.getString(R.string.percentage))
                        ?.toLineData(it.context)
                        ?.let { lineData ->
                            setupChart(it, lineData, mw.toFloat())
                        }
                }
            }
        )
    }
}
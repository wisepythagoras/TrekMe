package com.peterlaurence.trekme.ui.mapview.components.tileview.plugins

import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import androidx.annotation.ColorInt
import com.moagrius.tileview.TileView
import com.moagrius.tileview.plugins.PathPlugin
import com.peterlaurence.trekme.core.map.gson.RouteGson

private const val DEFAULT_STROKE_COLOR = -0xc0ae4b
private const val DEFAULT_STROKE_WIDTH_DP = 4

/**
 * This plugin differs from [PathPlugin] from the TileView library in that is uses
 * [Canvas.drawLines] to draw routes (which is hardware-accelerated), instead of [Canvas.drawPath]
 * (which is more fancy but much slower).
 *
 * @author peterLaurence on 10/05/2019
 */
class RoutePlugin: TileView.Plugin, TileView.CanvasDecorator, TileView.Listener {
    private lateinit var routeList: List<RouteGson.Route>
    private var scale = 1f
    private lateinit var defaultPaint: Paint
    private lateinit var tileView: TileView

    override fun install(tileView: TileView) {
        this.tileView = tileView
        tileView.addCanvasDecorator(this)
        tileView.addListener(this)

        val metrics = tileView.resources.displayMetrics
        val mStrokeWidthDefault = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP.toFloat(), metrics)

        defaultPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = DEFAULT_STROKE_COLOR
            strokeWidth = mStrokeWidthDefault
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun decorate(canvas: Canvas) {
        if (this::routeList.isInitialized) {
            for (route in routeList) {
                if (route.data is DrawablePath) {
                    val drawablePath = route.data as DrawablePath
                    if (drawablePath.paint == null) {
                        drawablePath.paint = Paint(defaultPaint)
                    }
                    val paint = drawablePath.paint!!

                    if (route.visible) {
                        paint.strokeWidth = drawablePath.width ?: defaultPaint.strokeWidth / scale
                        canvas.drawLines(drawablePath.path, paint)
                    }
                }
            }
        }
    }

    override fun onScaleChanged(scale: Float, previous: Float) {
        this.scale = scale
    }

    fun setRoutes(routeList: List<RouteGson.Route>) {
        this.routeList = routeList
    }

    fun redraw() {
        tileView.invalidate()
    }

    /**
     * Set the paint's color. Note that the color is an int containing alpha
     * as well as r,g,b. This 32bit value is not premultiplied, meaning that
     * its alpha can be any value, regardless of the values of r,g,b.
     * See the Color class for more details.
     *
     * @param color The new color (including alpha) to set in the paint.
     */
    fun setColor(@ColorInt color: Int) {
        defaultPaint.color = color
    }

    class DrawablePath(val path: FloatArray, val width: Float? = null, var paint: Paint? = null)
}
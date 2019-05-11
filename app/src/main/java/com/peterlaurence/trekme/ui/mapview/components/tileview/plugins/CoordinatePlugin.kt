package com.peterlaurence.trekme.ui.mapview.components.tileview.plugins

import com.moagrius.tileview.TileView

/**
 * The [TileView] needs to have coordinates set on each of its borders. These are called "relative
 * coordinates".
 *
 * The purpose of this plugin is to provide the handle to set those relative coordinates, and utility
 * methods to convert them to pixel coordinates (and the other way around).
 */
class CoordinatePlugin(private val west: Double,
                       private val north: Double,
                       east: Double,
                       south: Double) : TileView.Plugin, TileView.Listener, TileView.ReadyListener {

    private var scale = 1f

    private val height: Double = south - north
    private val width: Double = east - west

    private var widthInPixel: Int = 0
    private var heightInPixel: Int = 0

    override fun install(tileView: TileView) {
        tileView.addReadyListener(this)
        tileView.addListener(this)
    }

    override fun onReady(tileView: TileView) {
        widthInPixel = tileView.contentWidth
        heightInPixel = tileView.contentHeight
    }

    /**
     * Coordinate to pixel is multiplied by scale, pixel to coordinate is divided by scale
     */
    override fun onScaleChanged(scale: Float, previous: Float) {
        this.scale = scale
    }

    /**
     * Translate [relativeX] coordinate to an x pixel value.
     *
     * @param relativeX Typically the projected X value, can also be the longitude
     * @return The pixel value.
     */
    fun translateRelativeX(relativeX: Double): Int {
        val factor = (relativeX - west) / width
        return (widthInPixel * factor * scale).toInt()
    }

    /**
     * Translate [relativeY] coordinate to a y pixel value.
     *
     * @param relativeY Typically the projected Y value, can also be the latitude
     * @return The pixel value.
     */
    fun translateRelativeY(relativeY: Double): Int {
        val factor = (relativeY - north) / height
        return (heightInPixel * factor * scale).toInt()
    }

    /**
     * Translate an x pixel value to a relative X value.
     * If there is something wrong with the scale or [widthInPixel], default to [west] value.
     */
    fun xToRelativeX(x: Int): Double {
        return if (widthInPixel != 0 || scale != 0f) {
            west + x / scale * width / widthInPixel
        } else {
            west
        }

    }

    /**
     * Translate a y pixel value to a relative Y value.
     * If there is something wrong with the scale or [heightInPixel], default to [north] value.
     */
    fun yToRelativeY(y: Int): Double {
        return if (heightInPixel != 0 || scale != 0f) {
            north + y / scale * height / heightInPixel
        } else {
            north
        }
    }
}
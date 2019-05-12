package com.peterlaurence.trekme.ui.mapview.components.tileview

import com.moagrius.tileview.TileView
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.ui.mapview.components.tileview.plugins.CoordinatePlugin
import com.peterlaurence.trekme.ui.mapview.components.tileview.plugins.RoutePlugin

class HandlerTileView(builder: TileView.Builder, val coordinatePlugin: CoordinatePlugin) {
    private val routePlugin = RoutePlugin()
    private val liveRoutePlugin = RoutePlugin()

    init {
        builder.installPlugin(coordinatePlugin)
        builder.installPlugin(routePlugin)
        builder.installPlugin(liveRoutePlugin)
    }

    fun setRoutes(routeList: List<RouteGson.Route>) {
        routePlugin.setRoutes(routeList)
    }

    fun setLiveRoute(routeList: List<RouteGson.Route>) {
        liveRoutePlugin.setRoutes(routeList)
    }

    fun setLiveRouteColor(color: Int) {
        liveRoutePlugin.setColor(color)
    }

    fun routeChanged() {
        routePlugin.redraw()
    }

    fun translateRelativeX(relativeX: Double): Int {
        return coordinatePlugin.translateRelativeX(relativeX)
    }

    fun translateRelativeY(relativeY: Double): Int {
        return coordinatePlugin.translateRelativeY(relativeY)
    }
}
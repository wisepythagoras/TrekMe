package com.peterlaurence.trekme.ui.mapview

import android.util.Log
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getRoutesForMap
import com.peterlaurence.trekme.ui.mapview.components.tileview.HandlerTileView
import com.peterlaurence.trekme.ui.mapview.components.tileview.plugins.RoutePlugin
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * All [RouteGson.Route] are managed here.
 * This object is intended to be used exclusively by the [MapViewFragment].
 *
 * After being created, the method [init] has to be called.
 *
 * @author peterLaurence on 13/05/17 -- Converted to Kotlin on 16/02/2019
 */
class RouteLayer(private val coroutineScope: CoroutineScope) :
        TracksManageFragment.TrackChangeListener,
        CoroutineScope by coroutineScope {
    private lateinit var handlerTileView: HandlerTileView
    private lateinit var map: Map

    private val TAG = "RouteLayer"

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * [RouteGson.Route] are added to the [Map].
     *
     * @param map       the [Map] associated with the change
     * @param routeList a list of [RouteGson.Route]
     */
    override fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>) {
        Log.d(TAG, routeList.size.toString() + " new route received for map " + map.name)

        drawRoutes()
    }

    override fun onTrackVisibilityChanged() {
        handlerTileView.routeChanged()
    }

    /**
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, handlerTileView: HandlerTileView) {
        this.map = map
        this.handlerTileView = handlerTileView

        if (this.map.areRoutesDefined()) {
            drawRoutes()
        } else {
            acquireThenDrawRoutes(this.map)
        }
    }

    /**
     * The "live" route will be re-drawn completely, using the same pattern as other routes.
     */
    fun updateLiveRoute(route: RouteGson.Route, map: Map) {
        /* Redraw completely */
        drawLiveRouteCompletely(route)
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        getRoutesForMap(map)

        /* Then draw them */
        drawRoutes()
    }

    private fun drawRoutes() {
        /* Display all routes */
        map.routes?.let { routes ->
            if (routes.isNotEmpty()) {
                drawRoutes(routes, handlerTileView) {
                    handlerTileView.setRoutes(this)
                }
            }
        }
    }

    private fun drawLiveRouteCompletely(liveRoute: RouteGson.Route) {
        drawRoutes(listOf(liveRoute), handlerTileView) {
            handlerTileView.setLiveRoute(this)
        }
    }

    /**
     * Use [Channel]s to communicate between coroutines and ensure no concurrent access on resources.
     * A [Channel] is represented here with ****
     *
     *                      Worker pool
     *                   _________________
     *   routes         |  _____________  |         paths          _______________
     * [********] ----> | | producePath | | ----> [*******] ----> | pathProcessor |
     *                  |  -------------  |                        ---------------
     *                  |  _____________  |
     *                  | | producePath | |
     *                  |  -------------  |
     *                  ------------------
     */
    private fun CoroutineScope.drawRoutes(routeList: List<RouteGson.Route>,
                                          handlerTileView: HandlerTileView,
                                          action: List<RouteGson.Route>.() -> Unit) = launch {

        val routes = Channel<RouteGson.Route>()
        val paths = Channel<Pair<RouteGson.Route, FloatArray>>()

        repeat(2) {
            producePath(routes, paths, handlerTileView)
        }
        pathProcessor(paths, action)

        routeList.forEach {
            routes.send(it)
        }
    }

    private fun CoroutineScope.pathProcessor(paths: ReceiveChannel<Pair<RouteGson.Route, FloatArray>>,
                                             action: List<RouteGson.Route>.() -> Unit) = launch {
        val routesToDraw = mutableListOf<RouteGson.Route>()
        for ((route, lines) in paths) {
            /* Set the route data */
            val drawablePath = RoutePlugin.DrawablePath(lines)
            route.apply {
                data = drawablePath
            }
            routesToDraw.add(route)
            routesToDraw.action()
        }
    }

    /**
     * Each [RouteGson.Route] of a [Map] needs to provide data in a format that the
     * [HandlerTileView] understands.
     * This is done off UI thread.
     */
    private fun CoroutineScope.producePath(routes: ReceiveChannel<RouteGson.Route>,
                                           paths: SendChannel<Pair<RouteGson.Route, FloatArray>>,
                                           handlerTileView: HandlerTileView) = launch(Dispatchers.Default) {
        for (route in routes) {
            try {
                val lines = route.toPath(handlerTileView) ?: continue
                paths.send(Pair(route, lines))
            } catch (e: Exception) {
                // ignore and continue the loop
            }
        }
    }

    /**
     * Convert a [RouteGson.Route] to a [FloatArray] which is the drawable data structure expected
     * by the view that will represent it.
     */
    private fun RouteGson.Route.toPath(handlerTileView: HandlerTileView): FloatArray? {
        val markerList = route_markers ?: listOf()
        /* If there is only one marker, the path has no sense */
        if (markerList.size < 2) return null

        val size = markerList.size * 4 - 4
        val lines = FloatArray(size)

        var i = 0
        var init = true
        for (marker in markerList) {
            val relativeX = marker.getRelativeX(map)
            val relativeY = marker.getRelativeY(map)
            if (init) {
                lines[i] = handlerTileView.translateRelativeX(relativeX).toFloat()
                lines[i + 1] = handlerTileView.translateRelativeY(relativeY).toFloat()
                init = false
                i += 2
            } else {
                lines[i] = handlerTileView.translateRelativeX(relativeX).toFloat()
                lines[i + 1] = handlerTileView.translateRelativeY(relativeY).toFloat()
                if (i + 2 >= size) break
                lines[i + 2] = lines[i]
                lines[i + 3] = lines[i + 1]
                i += 4
            }
        }

        return lines
    }

    private fun MarkerGson.Marker.getRelativeX(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_x else lon
    }

    private fun MarkerGson.Marker.getRelativeY(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_y else lat
    }
}

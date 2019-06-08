package com.peterlaurence.mapview.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

const val N_WORKERS = 4

/**
 * @param [visibleTileLocations] channel of [TileLocation], which capacity should be [Channel.CONFLATED].
 * @param [tilesOutput] channel of [Tile], which capacity should be [Channel.UNLIMITED]
 */
fun CoroutineScope.processVisibleTiles(visibleTileLocations: ReceiveChannel<List<TileLocation>>,
                                       tilesOutput: SendChannel<Tile>,
                                       tileProvider: TileProvider) {
    val tilesToDownload = Channel<Tile>(capacity = Channel.UNLIMITED)
    val tilesDownloadedFromWorker = Channel<Tile>(capacity = Channel.UNLIMITED)

    repeat(N_WORKERS) { worker(tilesToDownload, tilesDownloadedFromWorker) }
    tileCollector(visibleTileLocations, tilesToDownload, tilesDownloadedFromWorker, tilesOutput,
            tileProvider)
}

fun CoroutineScope.worker(tilesToDownload: ReceiveChannel<Tile>,
                          tilesDownloaded: SendChannel<Tile>) = launch {
    for (tile in tilesToDownload) {
        // TODO : download tile, use the tileStreamProvider
        tilesDownloaded.send(tile)
    }
}

fun CoroutineScope.tileCollector(tileLocations: ReceiveChannel<List<TileLocation>>,
                                 tilesToDownload: SendChannel<Tile>,
                                 tilesDownloadedFromWorker: ReceiveChannel<Tile>,
                                 tilesOutput: SendChannel<Tile>,
                                 tileProvider: TileProvider) = launch {

    val locationsBeingProcessed = mutableListOf<TileLocation>()

    while (true) {
        select<Unit> {
            tileLocations.onReceive {
                for (loc in it) {
                    if (!locationsBeingProcessed.contains(loc)) {
                        /* Add it to the list of locations being processed */
                        locationsBeingProcessed.add(loc)

                        /* Now download the tile */
                        val tile = tileProvider.getTile(loc.zoom, loc.row, loc.col)
                        tilesToDownload.send(tile)
                    }
                }
            }

            tilesDownloadedFromWorker.onReceive {
                tilesOutput.send(it)

                /* Now remove the location from the list */
                val loc = TileLocation(it.zoom, it.row, it.col)
                locationsBeingProcessed.remove(loc)
            }
        }
    }
}

data class TileLocation(val zoom: Int, val row: Int, val col: Int)

interface TileProvider {
    fun getTile(zoom: Int, row: Int, col: Int): Tile
}
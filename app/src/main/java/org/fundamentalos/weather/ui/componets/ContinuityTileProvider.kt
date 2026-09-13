package org.fundamentalos.weather.ui.componets

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.tileprovider.ExpirableBitmapDrawable
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.ReusableBitmapDrawable
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex

/** Bridge a zoom-in with an already cached parent while the normal request runs.
 * Uses only memory already loaded by navigation: no network prefetch or main-thread disk IO.
 */
internal class ContinuityTileProvider(context: Context, source: ITileSource) : MapTileProviderBasic(context, source) {
    init { ensureCapacity(128) }

    override fun getMapTile(index: Long): Drawable? {
        super.getMapTile(index)?.let {
            (it as? BitmapDrawable)?.paint?.isFilterBitmap = true
            return it
        }
        val zoom = MapTileIndex.getZoom(index)
        for (difference in 1..minOf(zoom, 4)) {
            val parent = MapTileIndex.getTileIndex(zoom - difference,
                MapTileIndex.getX(index) shr difference, MapTileIndex.getY(index) shr difference)
            val drawable = tileCache.getMapTile(parent) as? BitmapDrawable ?: continue
            val bitmap = MapTileApproximater.approximateTileFromLowerZoom(drawable, index, difference) ?: continue
            val preview = ReusableBitmapDrawable(bitmap).apply { paint.isFilterBitmap = true }
            putTileIntoCache(index, preview, ExpirableBitmapDrawable.SCALED)
            return tileCache.getMapTile(index)
        }
        return null
    }
}

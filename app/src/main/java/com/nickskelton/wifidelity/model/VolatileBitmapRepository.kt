package com.nickskelton.wifidelity.model

import android.graphics.Bitmap
import java.util.UUID

class VolatileBitmapRepository : SingleItemRepository<Bitmap> {

    private val items = HashMap<String, Bitmap>()

    override fun add(item: Bitmap): String {
        val id = UUID.randomUUID()
        items[id.toString()] = item
        return id.toString()
    }

    override fun remove(id: String): Boolean {
        return items.remove(id) != null
    }

    override fun get(id: String): Bitmap? {
        return items[id]
    }

    override fun clear() {
        items.clear()
    }
}
package com.nickskelton.wifidelity.model

import android.graphics.Bitmap
import io.fotoapparat.result.Photo
import java.util.UUID

class VolatilePhotoRepository : SingleItemRepository<Photo> {

    private val items = HashMap<String, Photo>()

    override fun add(item: Photo): String {
        val id = UUID.randomUUID()
        items[id.toString()] = item
        return id.toString()
    }

    override fun remove(id: String): Boolean {
        return items.remove(id) != null
    }

    override fun get(id: String): Photo? {
        return items[id]
    }

    override fun clear() {
        items.clear()
    }
}
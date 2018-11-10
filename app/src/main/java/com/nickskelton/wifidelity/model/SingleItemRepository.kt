package com.nickskelton.wifidelity.model

import android.graphics.Bitmap

interface SingleItemRepository<T> {
    fun add(item: T): String
    fun remove(id: String): Boolean
    fun get(id: String): T?
    fun clear()
}

interface SingleBitmapRepository : SingleItemRepository<Bitmap>
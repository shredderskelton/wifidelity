package com.nickskelton.wifidelity.view.adapter

import android.graphics.drawable.BitmapDrawable

data class BlockListItem(
    val bitmapMine: BitmapDrawable?,
    val foundText: String,
    val onSelected: (BlockListItem) -> Unit
)
package com.nickskelton.wifidelity.view.adapter

import com.nickskelton.wifidelity.R

sealed class BlockListItem(
    val drawableIconRes: Int,
    val titleText: String,
    val subtitleText: String,
    val onSelected: (BlockListItem) -> Unit
)

class NetworkBlockListItem(
    networkName: String,
    onSelected: (BlockListItem) -> Unit
) : BlockListItem(
    R.drawable.ic_network_wifi,
    networkName,
    "Network Item",
    onSelected
)

class TextBlockListItem(
    foundText: String,
    val strength: Int,
    onSelected: (BlockListItem) -> Unit
) : BlockListItem(
    R.drawable.ic_text_format,
    foundText,
    strength.toString(),
    onSelected
)


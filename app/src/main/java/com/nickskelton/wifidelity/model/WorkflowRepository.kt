package com.nickskelton.wifidelity.model

import android.graphics.drawable.BitmapDrawable
import com.nickskelton.wifidelity.view.adapter.BlockListItem

class WorkflowRepository {
    var networkName: String = ""
    var results: List<Pair<BitmapDrawable, String>> = emptyList()
}
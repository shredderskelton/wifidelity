package com.nickskelton.wifidelity.utils

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log

class PerformanceLogger(private val label: String) {
    private val mSplits = mutableListOf<Long>()
    private val mSplitLabels = mutableListOf<String>()

    init {
        addSplit("begin")
    }

    fun reset() {
        mSplits.clear()
        mSplitLabels.clear()
    }

    fun addSplit(splitLabel: String) {
        val now = SystemClock.elapsedRealtime()
        mSplits.add(now)
        mSplitLabels.add(splitLabel)
    }

    @SuppressLint("LogNotTimber")
    fun dumpToLog() {
        Log.d("Performance", "$label: begin")
        val first = mSplits[0]
        var now = first
        for (i in 1 until mSplits.size) {
            now = mSplits[i]
            val splitLabel = mSplitLabels[i]
            val prev = mSplits[i - 1]
            Log.d("Performance", "$label:      ${(now - prev)} ms, $splitLabel")
        }
        Log.d("Performance", "$label: end, ${(now - first)}ms")
    }
}
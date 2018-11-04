package com.nickskelton.wifidelity.view

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.nickskelton.wifidelity.R

class TextResultsActivity : AppCompatActivity() {

    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_result)
        textView = findViewById(R.id.textView)
    }
}
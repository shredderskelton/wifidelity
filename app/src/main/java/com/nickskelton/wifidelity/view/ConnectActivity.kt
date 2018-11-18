package com.nickskelton.wifidelity.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.chip.Chip
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityConnectBinding
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import kotlinx.android.synthetic.main.activity_connect.*
import kotlinx.android.synthetic.main.activity_connect.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ConnectActivity : AppCompatActivity() {
    companion object {
        fun start(context: Context, netPwd: Pair<String, String>) {
            val intent = Intent(context, ConnectActivity::class.java)
            intent.putExtra("network", netPwd.first)
            intent.putExtra("password", netPwd.second)
            context.startActivity(intent)
        }
    }

    private val networkParam by lazy {
        intent.getStringExtra("network")
    }

    private val passwordParam by lazy {
        intent.getStringExtra("password")
    }

    private val viewModel: ConnectViewModel by viewModel { parametersOf(networkParam, passwordParam) }

    private lateinit var binding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.setLifecycleOwner(this)
        binding.vm = viewModel

        title = "Connect"

        viewModel.bind()
    }

    private fun ConnectViewModel.bind() {
        observeNonNull(networkSuggestions) { suggestions ->
            networkChips.removeAllViews()
            suggestions.forEach { addChip(it) }
        }
    }

    private fun addChip(suggestion: String) {
        val chip = Chip(this).apply {
            text = suggestion
            setOnClickListener { view ->
                viewModel.network = (view as Chip).text.toString()
            }
        }
        networkChips.addView(chip)
    }
}

fun View.visible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}
package com.nickskelton.wifidelity.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityChoosePasswordBinding
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import kotlinx.android.synthetic.main.activity_choose_network.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChoosePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChoosePasswordBinding
    private val adapter = SimpleBlockAdapter()

    private val viewModel: ChoosePasswordViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_password)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_password)
        binding.setLifecycleOwner(this)
        binding.vm = viewModel

        binding.vm!!.apply {
            adapter.updateItems(items)
            observeNonNull(actionNext) {
                startConnect(it)
            }
        }

        recyclerMan.adapter = adapter
        recyclerMan.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        setTitle("Select the password: ")
    }

    private fun startConnect(netPwd: Pair<String, String>) {
        ConnectActivity.start(this, netPwd)
    }
}
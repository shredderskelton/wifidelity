package com.nickskelton.wifidelity.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityConnectBinding
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import kotlinx.android.synthetic.main.activity_choose_network.*
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

    private lateinit var binding:ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        binding.setLifecycleOwner(this)
        binding.vm = viewModel

//        binding.vm!!.apply {
//            observeNonNull(actionNext) {
//                startConnect(it)
//            }
//        }

        setTitle("Connect")
    }
}
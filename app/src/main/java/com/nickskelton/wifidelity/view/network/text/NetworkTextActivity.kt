package com.nickskelton.wifidelity.view.network.text

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.view.WifiPermissionDialog
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_network.*
import org.koin.core.parameter.parametersOf
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.Serializable

class NetworkTextActivity : AppCompatActivity() {
    companion object {
        private const val ARG_CONFIG = "config"

        fun start(context: Context, args: Args) {
            val intent = Intent(context, NetworkTextActivity::class.java)
            intent.putExtra(ARG_CONFIG, args)
            context.startActivity(intent)
        }
    }

    data class Args(val results: List<String>) : Serializable

    private val viewModel: NetworkTextViewModel by viewModel { parametersOf(args) }

    private val requiredPermissions =
        arrayOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION)

    private lateinit var rxPermissions: RxPermissions
    private val adapter = SimpleBlockAdapter()

    private val args by lazy {
        intent.getSerializableExtra(ARG_CONFIG) as Args
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        setSupportActionBar(toolbar)

        title = "Select your wifi network: "
        setupPermissions()
        viewModel.bind()
    }

    private fun NetworkTextViewModel.bind() {
        observeNonNull(adapterItems) {
            adapter.updateItems(it)
            recyclerView.scrollToPosition(0)
        }
        observeNonNull(exactMatchFound) {
            //TODO
            Timber.d("Exact match found: Show Dialog ")
        }
        observeNonNull(exactMatches) { matches ->
            //TODO
            matches.forEach {
                Timber.d("Exact match found: $it")
            }
        }
    }

    private fun setupPermissions() {
        rxPermissions = RxPermissions(this)
        val allPermissionsGranted = requiredPermissions.all { rxPermissions.isGranted(it) }
        if (!allPermissionsGranted) {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        WifiPermissionDialog().apply {
            onContinue = {
                rxPermissions
                    .request(*requiredPermissions)
                    .subscribe {
                        if (it) {
                            Timber.w("User declined permissions")
                        }
                    }
            }
            show(supportFragmentManager, "")
        }
    }
}
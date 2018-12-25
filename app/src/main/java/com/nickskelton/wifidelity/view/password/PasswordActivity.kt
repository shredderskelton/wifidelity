package com.nickskelton.wifidelity.view.password

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.view.ConnectActivity
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import kotlinx.android.synthetic.main.activity_network.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.Serializable

class PasswordActivity : AppCompatActivity() {
    companion object {
        private const val ARG_CONFIG = "pwdconfig"

        fun start(context: Context, args: Args) {
            val intent = Intent(context, PasswordActivity::class.java)
            intent.putExtra(ARG_CONFIG, args)
            context.startActivity(intent)
        }
    }

    data class Args(val selectedNetwork: String, val capturedResults: List<String>) : Serializable

    private val viewModel: PasswordViewModel by viewModel { parametersOf(args) }
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

        title = "Select the password: "
        viewModel.bind()
    }

    private fun PasswordViewModel.bind() {
        adapter.updateItems(items)
        recyclerView.scrollToPosition(0)

        observeNonNull(actionNext) {
            ConnectActivity.start(baseContext, Pair(args.selectedNetwork, it))
        }
    }
}
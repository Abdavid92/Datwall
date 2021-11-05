package com.smartsolutions.paquetes.ui.setup

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.IReplaceFragments
import com.smartsolutions.paquetes.ui.activation.PurchasedFragment
import com.smartsolutions.paquetes.ui.addOpenActivityListener
import com.smartsolutions.paquetes.ui.next
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AbstractActivity(R.layout.activity_setup),
    OnCompletedListener, IReplaceFragments {

    private val viewModel by viewModels<SetupViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.configurations.observe(this) {
            nextOrComplete()
        }

        viewModel.addOpenActivityListener(this) { activity, application ->
            application.removeOpenActivityListener(this)
            startActivity(Intent(this, activity))
            finish()
        }
    }

    override fun onCompleted() {
        nextOrComplete()
    }

    private fun nextOrComplete() {
        if (viewModel.hasNextConfiguration()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.setup_container, viewModel.nextConfiguration()!!.fragment, null)
                .commit()
        } else {
            viewModel.next()
        }
    }

    override fun replace(fragment: AbstractSettingsFragment) {
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.setup_container, fragment)
        }
    }
}
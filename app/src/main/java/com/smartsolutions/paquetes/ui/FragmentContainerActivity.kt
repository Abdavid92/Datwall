package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.setup.OnCompletedListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentContainerActivity : AbstractActivity(R.layout.activity_fragment_container),
    OnCompletedListener, IReplaceFragments {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentName = intent.getStringExtra(EXTRA_FRAGMENT)

        if (fragmentName != null) {

            val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                fragmentName
            ).apply {
                arguments = intent.extras
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.container_view, fragment)
                .commit()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onCompleted() {
        setResult(RESULT_OK)
        finish()
    }

    override fun replace(fragment: AbstractSettingsFragment) {
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.container_view, fragment)
        }
    }

    companion object {

        /**
         * Nombre de la clase del fragmento a instanciar.
         * */
        const val EXTRA_FRAGMENT = "com.smartsolutions.paquetes.extra.FRAGMENT"
    }
}
package com.smartsolutions.paquetes.ui

import android.os.Bundle
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.setup.OnCompletedListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentContainerActivity : AbstractActivity(R.layout.activity_fragment_container),
    OnCompletedListener {

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

    companion object {

        const val EXTRA_FRAGMENT = "com.smartsolutions.paquetes.extra.FRAGMENT"
    }
}
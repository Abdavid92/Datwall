package com.smartsolutions.paquetes.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.permissions.PermissionsFragment
import dagger.hilt.android.AndroidEntryPoint
import java.lang.NullPointerException

@AndroidEntryPoint
class FragmentContainerActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_container)

        handleIntents()
    }


    private fun handleIntents() {
        if (ACTION_OPEN_FRAGMENT == intent.action) {
            val fragment = intent.getStringExtra(EXTRA_FRAGMENT) ?: throw NullPointerException()

            when (fragment) {
                EXTRA_FRAGMENT_PERMISSIONS -> {
                    openPermissions(
                        intent.getIntArrayExtra(EXTRA_PERMISSIONS_REQUESTS_CODES)
                            ?: throw NullPointerException()
                    )
                }
                else -> {
                    finish()
                }
            }

        } else {
            finish()
        }
    }


    private fun openPermissions(requestCodes: IntArray) {

        val fragment = PermissionsFragment.newInstance(
            requestCodes,
            object : PermissionsFragment.PermissionFragmentCallback {
                override fun onFinished() {
                    finish()
                }
            })

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, fragment).commit()
    }


    companion object {
        const val ACTION_OPEN_FRAGMENT = "action_open_fragment"
        const val EXTRA_FRAGMENT = "extra_fragment"
        const val EXTRA_FRAGMENT_PERMISSIONS = "extra_fragment_permissions"
        const val EXTRA_PERMISSIONS_REQUESTS_CODES = "extra_permissions_request_codes"
    }
}
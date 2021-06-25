package com.smartsolutions.paquetes.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.ui.ApplicationFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.PermissionsManager
import com.smartsolutions.paquetes.ui.permissions.PermissionsFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : ApplicationFragment() {

    private val homeViewModel by viewModels<HomeViewModel>()

    @Inject
    lateinit var permissionsManager: PermissionsManager

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val codes = permissionsManager.getDeniedPermissions(false).map {
            it.requestCode
        }

        parentFragmentManager.beginTransaction()
            .add(R.id.container, PermissionsFragment.newInstance(codes.toIntArray()))
            .commit()
    }
}
package com.smartsolutions.paquetes.ui.applications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.databinding.FragmentApplicationsPlaceholderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_KEY = "arg_key"

/**
 * Fragmento del control de las aplicaciones.
 * */
@AndroidEntryPoint
class PlaceHolderFragment : AbstractFragment(),
    AppsListAdapter.OnAppChangeListener,
    ApplicationsFragment.ApplicationsFragmentListener
{

    /**
     * Enlace a la vista.
     * */
    private var _binding: FragmentApplicationsPlaceholderBinding? = null
    private val binding: FragmentApplicationsPlaceholderBinding
        get() = _binding!!

    private val viewModel by viewModels<ApplicationsViewModel>(
        { requireParentFragment() }
    )

    /**
     * Adaptador de la lista de aplicaciones.
     * */
    private var adapter: AppsListAdapter? = null

    /**
     * Clave que indica cuál es la lista de aplicaciones a crear.
     * Las claves admitidas son [SectionsPagerAdapter.USER_APPS] y
     * [SectionsPagerAdapter.SYSTEM_APPS].
     * */
    private lateinit var key: String

    /**
     * Launcher que lanza la [AppControlActivity] y espera el resultado.
     * */
    private val appControlLauncher = registerForActivityResult(
        object : ActivityResultContract<App, App?>() {

            override fun createIntent(context: Context, input: App): Intent {
                return Intent(context, AppControlActivity::class.java)
                    .putExtra(AppControlActivity.EXTRA_APP, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): App? {
                if (resultCode == Activity.RESULT_OK)
                    return intent?.getParcelableExtra(AppControlActivity.EXTRA_APP)
                return null
            }
        }
    ) {
        it?.let { app ->
            onAppChange(app)
            adapter?.updateApp(app)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        key = arguments?.getString(ARG_KEY) ?: SectionsPagerAdapter.USER_APPS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApplicationsPlaceholderBinding.inflate(
            inflater,
            container,
            false
        )

        //Helper de la ui con métodos de utilidades
        val uiHelper = UIHelper(requireActivity())

        //Aplico los colores del tema al SwipeRefreshLayout
        binding.refresh.apply {
            uiHelper.applySwipeRefreshTheme(this)
            isRefreshing = true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.refresh.setOnRefreshListener {
            if(!viewModel.commitUpdateApps()) {
                binding.refresh.isRefreshing = false
            }
        }

        viewModel.getApps(key).observe(viewLifecycleOwner) {
            if (adapter != null) {
                adapter!!.updateList(it.first, it.second)
            } else {
                adapter = AppsListAdapter(
                    this,
                    appControlLauncher,
                    viewModel.iconManager,
                    it.first,
                    it.second)

                binding.appsList.adapter = adapter
            }
            binding.refresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()

        /*Se invoca cuando se cambia el filtro.
        * Se asigna en el método onResume para que
        * el fragmento que esté visible sea el que
        * escuche el evento. El otro no.*/
        viewModel.filterChangeListener = {
            binding.refresh.isRefreshing = true
        }
    }

    /**
     * Se invoca cuando se cambian las propiedades de una app.
     * */
    override fun onAppChange(app: IApp) {
        viewModel.addAppToUpdate(app)
    }

    /**
     * Se invoca cuando se está buscando una app.
     * */
    override fun onQueryApp(query: String?) {
        //adapter?.search(query)

        if (query != null && query.isNotBlank() && adapter != null)
            viewModel.scheduleSearchQuery(query, adapter!!)
    }

    override fun onUpList() {
        runCatching {
            binding.appsList.smoothScrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

        fun newInstance(@FragmentAppsKey key: String): Fragment {
            val fragment = PlaceHolderFragment()

            val args = Bundle().apply {
                putString(ARG_KEY, key)
            }

            fragment.arguments = args

            return fragment
        }
    }
}
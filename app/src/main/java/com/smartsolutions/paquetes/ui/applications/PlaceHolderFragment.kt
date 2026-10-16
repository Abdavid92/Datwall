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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.AppListSection
import com.smartsolutions.paquetes.managers.models.AppListSectionType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
            if(!viewModel.onCommitChanges()) {
                binding.refresh.isRefreshing = false
            }
        }

        adapter = AppsListAdapter(
            this,
            appControlLauncher,
            viewModel.iconManager,
            viewModel.state.value.filter
        )
        binding.appsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state
                    .map { state ->
                        Triple(
                            state.loading,
                            state.filter,
                            if (key == SectionsPagerAdapter.SYSTEM_APPS) state.systemSections else state.userSections
                        )
                    }
                    .collect { (loading, filter, sections) ->
                        if (!loading) {
                            adapter?.submit(filter, rows(sections))
                            binding.refresh.isRefreshing = false
                        }
                    }
            }
        }
    }

    /**
     * Filas de la lista: cada sección con su cabecera.
     * */
    private fun rows(sections: List<AppListSection>): List<IApp> =
        sections.flatMap { section ->
            val header = section.type?.let { listOf(HeaderApp.newInstance(headerText(it, section.apps.size))) }
            (header ?: emptyList()) + section.apps
        }

    private fun headerText(type: AppListSectionType, count: Int): String =
        getString(
            when (type) {
                AppListSectionType.ALLOWED -> R.string.allowed_applications
                AppListSectionType.BLOCKED -> R.string.blocked_applications
                AppListSectionType.FREE -> R.string.free_apps
                AppListSectionType.NATIONAL -> R.string.national_apps
                AppListSectionType.INTERNATIONAL -> R.string.international_apps
                AppListSectionType.MESSAGING -> R.string.messaging_apps
            },
            count
        )

    /**
     * Se invoca cuando se cambian las propiedades de una app.
     * */
    override fun onAppChange(app: IApp) {
        viewModel.onAccessChanged(app)
    }

    override fun onUpList() {
        runCatching {
            binding.appsList.smoothScrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        _binding = null
        adapter = null
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
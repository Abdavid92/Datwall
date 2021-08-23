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
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentApplicationsPlaceholderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_KEY = "arg_key"

/**
 * Fragmento del control de las aplicaciones.
 * */
@AndroidEntryPoint
class PlaceHolderFragment : Fragment(),
    AppsListAdapter.OnAppChangeListener,
    ApplicationsFragment.OnQueryAppListener
{

    /**
     * Enlace a la vista.
     * */
    private lateinit var binding: FragmentApplicationsPlaceholderBinding

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

            override fun createIntent(context: Context, input: App?): Intent {
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
        binding = FragmentApplicationsPlaceholderBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Helper de la ui con métodos de utilidades
        val uiHelper = UIHelper(requireContext())

        //Aplico los colores del tema al SwipeRefreshLayout
        binding.refresh.apply {
            uiHelper.getColorTheme(R.attr.colorOnPrimary)?.let {
                setProgressBackgroundColorSchemeColor(it)
            }
            uiHelper.getColorTheme(R.attr.colorAccent)?.let {
                setColorSchemeColors(it)
            }
            isRefreshing = true
        }

        binding.refresh.setOnRefreshListener {
            if(!viewModel.commitUpdateApps()) {
                binding.refresh.isRefreshing = false
            }
        }

        viewModel.getApps(key).observe(viewLifecycleOwner) {
            if (adapter != null) {
                /*Se debe borrar la cache del RecyclerView para forzar la actualización
                * de todos los ViewHolders.*/
                //binding.appsList.recycledViewPool.clear()
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
        adapter?.search(query)
    }

    override fun onDetach() {
        super.onDetach()
        //Confirmo los cambios
        viewModel.commitUpdateApps()
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
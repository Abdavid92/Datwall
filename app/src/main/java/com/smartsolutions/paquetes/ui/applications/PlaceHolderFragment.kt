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
import com.smartsolutions.paquetes.repositories.models.App
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_KEY = "arg_key"

@AndroidEntryPoint
class PlaceHolderFragment : Fragment() {

    private lateinit var binding: FragmentApplicationsPlaceholderBinding

    private val viewModel by viewModels<ApplicationsViewModel>()

    private var adapter: AppsListAdapter? = null

    private lateinit var key: String

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
        if (it != null) {
            viewModel.appsToUpdate.add(it)
            adapter?.updateApp(it)
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

        binding.refresh.isRefreshing = true

        binding.refresh.setOnRefreshListener {
            if(!viewModel.commitUpdateApps()) {
                binding.refresh.isRefreshing = false
            }
        }

        viewModel.getApps(key).observe(viewLifecycleOwner) {
            if (adapter != null) {
                adapter!!.updateList(it)
            } else {
                adapter = AppsListAdapter(
                    requireActivity(),
                    appControlLauncher,
                    viewModel.iconManager.get(),
                    it)
                binding.appsList.adapter = adapter
            }
            binding.refresh.isRefreshing = false
        }
    }

    override fun onDetach() {
        super.onDetach()
        viewModel.commitUpdateApps()
    }

    companion object {

        fun newInstance(key: String): Fragment {
            val fragment = PlaceHolderFragment()

            val args = Bundle().apply {
                putString(ARG_KEY, key)
            }

            fragment.arguments = args

            return fragment
        }
    }
}
package com.smartsolutions.paquetes.ui.applications

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentApplicationsBinding
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationsFragment : AbstractFragment() {

    private var _binding: FragmentApplicationsBinding? = null
    private val binding: FragmentApplicationsBinding
        get() = _binding!!

    private val viewModel by viewModels<ApplicationsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApplicationsBinding.inflate(
            inflater,
            container,
            false
        )

        (requireActivity() as AppCompatActivity)
            .setSupportActionBar(binding.toolbar)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionsAdapter = SectionsPagerAdapter(this)
        binding.viewPager.adapter = sectionsAdapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = sectionsAdapter.pages[position].second
        }.attach()

        binding.fabUp.setOnClickListener {
            childFragmentManager.fragments.forEach {
                if (it is ApplicationsFragmentListener)
                    it.onUpList()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.applications_menu, menu)

        val search = binding.toolbar.menu.findItem(R.id.app_bar_search)
            .actionView as SearchView
        setSearchViewQueryListener(search)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> {
                showFilterOptions()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSearchViewQueryListener(search: SearchView) {
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                childFragmentManager.fragments.filter { it is ApplicationsFragmentListener }
                    .forEach { fragment ->
                        fragment as ApplicationsFragmentListener
                        fragment.onQueryApp(newText)
                    }
                return true
            }
        })
    }

    private fun showFilterOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_filter_title)
            .setItems(R.array.filter_options) { _, which ->
                viewModel.setFilter(AppsFilter.values()[which])
            }.show()
    }

    override fun onPause() {
        super.onPause()

        //Confirmo los cambios
        viewModel.commitUpdateApps()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    interface ApplicationsFragmentListener {
        fun onQueryApp(query: String?)

        fun onUpList()
    }
}
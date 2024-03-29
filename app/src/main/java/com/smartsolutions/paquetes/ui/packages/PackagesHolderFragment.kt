package com.smartsolutions.paquetes.ui.packages

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesHolderBinding
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.FragmentContainerActivity
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint


private const val SIM_ID = "sim_id"
@AndroidEntryPoint
class PackagesHolderFragment : Fragment(), PackagesViewModel.PurchaseResult {

    private lateinit var simId: String

    private var _binding: FragmentPackagesHolderBinding? = null
    private val binding: FragmentPackagesHolderBinding
        get() = _binding!!


    private val viewModel by viewModels<PackagesViewModel>()

    private var adapter: PackagesRecyclerAdapter? = null
    private var sim: Sim? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            simId = it.getString(SIM_ID) ?: throw IllegalArgumentException()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesHolderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonConfigureSim.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    FragmentContainerActivity::class.java
                ).apply {
                    putExtra(
                        FragmentContainerActivity.EXTRA_FRAGMENT,
                        PackagesConfigurationFragment::class.java.name
                    )
                    putExtra(
                        PackagesConfigurationFragment.EXTRA_DEFAULT_SIM_ID,
                        sim?.id
                    )
                }
            )
        }

        viewModel.getSimAndPackages(simId).observe(viewLifecycleOwner) {
            sim = it.first

            if (it.second.isNotEmpty()) {
                binding.apply {
                    linSimNotConfigured.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    setAdapter(it.second)
                }
            }else {
                binding.apply {
                    linSimNotConfigured.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                }
            }
        }

    }

    private fun setAdapter(list: List<IDataPackage>){
        if (adapter == null){
            adapter = PackagesRecyclerAdapter(this, list)
            binding.recycler.adapter = adapter
        }else {
            adapter?.dataPackages = list
            adapter?.notifyDataSetChanged()
        }
    }

    fun purchasePackage(iDataPackage: IDataPackage){
        val dataPackage = iDataPackage as DataPackage
        sim?.let {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Comprar ${dataPackage.name}")
                .setMessage(dataPackage.description)
                .setPositiveButton(getString(R.string.purchase)){_, _ ->
                    viewModel.invokeOnDefaultSim(
                        requireContext(),
                        it,
                        SimType.VOICE,
                        parentFragmentManager
                    ) {
                        viewModel.purchasePackage(it, dataPackage, this)
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }
    }

    override fun onSuccess() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package), Toast.LENGTH_SHORT).show()
    }

    override fun onFailed() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package_failed), Toast.LENGTH_SHORT).show()
    }

    override fun onMissingPermission() {
        Toast.makeText(requireContext(), getString(R.string.purchasing_package_permissions), Toast.LENGTH_SHORT).show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val frag = SinglePermissionFragment.newInstance(IPermissionsManager.CALL_CODE)
            frag.show(childFragmentManager, "PermissionFragment")
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {

        @JvmStatic
        fun newInstance(simID: String) =
            PackagesHolderFragment().apply {
                arguments = Bundle().apply {
                    putString(SIM_ID, simID)
                }
            }
    }
}
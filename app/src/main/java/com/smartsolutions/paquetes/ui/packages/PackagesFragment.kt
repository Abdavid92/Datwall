package com.smartsolutions.paquetes.ui.packages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.viewModels
import com.google.zxing.integration.android.IntentIntegrator
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPackagesBinding
import com.smartsolutions.paquetes.helpers.setTabLayoutMediatorSims
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.AbstractFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PackagesFragment : AbstractFragment() {

    companion object {
        fun newInstance() = PackagesFragment()
    }

    private lateinit var scannerActivity: ActivityResultLauncher<Unit>

    private val viewModel by viewModels<PackagesViewModel> ()

    private var adapter: PackagesPagerAdapter? = null

    private var _binding: FragmentPackagesBinding? = null
    private val binding: FragmentPackagesBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        scannerActivity = registerForActivityResult(
            object : ActivityResultContract<Unit, String?>() {
                override fun createIntent(context: Context, input: Unit): Intent {
                    return IntentIntegrator(requireActivity())
                        .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                        .setPrompt(getString(R.string.qr_prompt))
                        .createScanIntent()
                }

                override fun parseResult(resultCode: Int, intent: Intent?): String? {
                    IntentIntegrator.parseActivityResult(resultCode, intent)?.let {
                        return it.contents
                    }

                    return null
                }

            }
        ) { voucher ->
            if (voucher != null && validateVoucher(voucher)) {
                viewModel.sendUssdCode("*662*$voucher#")
            } else {
                Toast.makeText(requireContext(), R.string.fail_scan_qr, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateVoucher(voucher: String): Boolean {
        val trim = voucher.replace(" ", "")

        if (trim.isDigitsOnly() && trim.length == 16) {
            return true
        }

        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackagesBinding.inflate(layoutInflater, container, false)

        (requireActivity() as AppCompatActivity)
            .setSupportActionBar(_binding!!.toolbar)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getInstalledSims().observe(viewLifecycleOwner) {
            setAdapter(it)
            setTabLayoutMediatorSims(requireContext(), binding.tabs, binding.pager, it)

            if (it.size <= 1){
                binding.tabs.visibility = View.GONE
            }else {
                binding.tabs.visibility = View.VISIBLE
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.packages_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_scan_qr) {

            scannerActivity.launch(Unit)

            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setAdapter(sims: List<Sim>){
        if (adapter == null){
            adapter = PackagesPagerAdapter(sims, this)
            binding.pager.adapter = adapter
        } else {
            adapter?.sims = sims
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
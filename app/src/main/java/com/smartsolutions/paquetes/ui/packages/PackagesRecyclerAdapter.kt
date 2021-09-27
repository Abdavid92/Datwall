package com.smartsolutions.paquetes.ui.packages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.databinding.ItemHeaderBinding
import com.smartsolutions.paquetes.databinding.ItemHeaderPackagesBinding
import com.smartsolutions.paquetes.databinding.ItemPackagesBinding
import com.smartsolutions.paquetes.databinding.ItemPlanBinding
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.IDataPackage

class PackagesRecyclerAdapter(
    private val fragment: PackagesFragment,
    var dataPackages: List<IDataPackage>
): RecyclerView.Adapter<PackagesRecyclerAdapter.ItemHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when {
            dataPackages[position] is PackagesViewModel.HeaderPackagesItem -> 0
            dataPackages[position].network == Networks.NETWORK_3G_4G ||
            dataPackages[position].network == Networks.NETWORK_3G -> 1
            dataPackages[position].network == Networks.NETWORK_4G -> 2
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(viewType){
            0 -> HeaderHolder(ItemHeaderPackagesBinding.inflate(layoutInflater, parent, false))
            1 -> PlanHolder(ItemPlanBinding.inflate(layoutInflater,parent, false))
            2 -> PackagesHolder(ItemPackagesBinding.inflate(layoutInflater, parent, false))
            else -> {
              PackagesHolder(ItemPackagesBinding.inflate(layoutInflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.onBind(dataPackages[position])
    }

    override fun getItemCount(): Int {
       return dataPackages.size
    }


    abstract class ItemHolder(view: View): RecyclerView.ViewHolder(view){
        abstract fun onBind(iPackage: IDataPackage)
    }

    inner class HeaderHolder(private val binding: ItemHeaderPackagesBinding): ItemHolder(binding.root){

        override fun onBind(iPackage: IDataPackage) {
            binding.headerText.text = iPackage.name
        }

    }

    inner class PlanHolder(private val binding: ItemPlanBinding): ItemHolder(binding.root){

        override fun onBind(iPackage: IDataPackage) {
            binding.apply {
                val international = DataUnitBytes(iPackage.bytes).getValue()
                val lteInternational = DataUnitBytes(iPackage.bytesLte).getValue()
                val national = DataUnitBytes(iPackage.nationalBytes).getValue()

                packageTitle.text = iPackage.name
                price.text = iPackage.price.toString()
                internationalAllValue.text = "${international.value}"
                internationalAllUnit.text = "${international.dataUnit}"
                internationalLteValue.text = "${lteInternational.value}"
                internationalLteUnit.text = "${lteInternational.dataUnit}"
                nationalValue.text = "${national.value}"
                nationalUnit.text = "${national.dataUnit}"
                minutesValue.text = iPackage.minutes.toString()
                smsValue.text = iPackage.sms.toString()

                buttonPurchase.setOnClickListener {
                    fragment.purchasePackage(iPackage)
                }
            }
        }

    }

    inner class PackagesHolder(private val binding: ItemPackagesBinding): ItemHolder(binding.root){

        override fun onBind(iPackage: IDataPackage) {
            binding.apply {
                val lteInternational = DataUnitBytes(iPackage.bytesLte).getValue()
                val national = DataUnitBytes(iPackage.nationalBytes).getValue()

                titlePackage.text = iPackage.name
                textPrice.text = iPackage.price.toString()

                if (iPackage.id == DataPackages.PackageId.P_4GB_12GB_LTE){
                    val international = DataUnitBytes(iPackage.bytes).getValue()
                    internationalLteValue.text = "${international.value}\n + \n${lteInternational.value} (LTE)"
                }else {
                    internationalLteValue.text = "${lteInternational.value}"
                }

                internationalLteUnit.text = "${lteInternational.dataUnit}"
                nationalValue.text = "${national.value}"
                nationalUnit.text = "${national.dataUnit}"

                buttonPurchase.setOnClickListener {
                    fragment.purchasePackage(iPackage)
                }
            }
        }

    }
}
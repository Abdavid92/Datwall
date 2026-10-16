package com.smartsolutions.paquetes.ui.packages

import com.smartsolutions.paquetes.helpers.DataFormatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.*
import com.smartsolutions.paquetes.repositories.models.IDataPackage

class PackagesRecyclerAdapter(
    private val fragment: PackagesHolderFragment,
    var dataPackages: List<IDataPackage>
): RecyclerView.Adapter<PackagesRecyclerAdapter.ItemHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (dataPackages[position] is PackagesViewModel.HeaderPackagesItem) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(viewType){
            0 -> HeaderHolder(ItemHeaderPackagesBinding.inflate(layoutInflater, parent, false))
            else -> PlanHolder(ItemPlan2Binding.inflate(layoutInflater,parent, false))
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

    inner class HeaderHolder(private var binding: ItemHeaderPackagesBinding?): ItemHolder(binding!!.root){

        override fun onBind(iPackage: IDataPackage) {
            binding?.headerText?.text = iPackage.name
        }
    }

    inner class PlanHolder(private var binding: ItemPlan2Binding?): ItemHolder(binding!!.root){

        override fun onBind(iPackage: IDataPackage) {
            binding?.apply {
                val international = DataFormatter.bytes(iPackage.bytes)
                val national = DataFormatter.bytes(iPackage.nationalBytes)

                titlePackage.text = iPackage.name
                textPrice.text = iPackage.price.toString()
                valueMb.text = "${international.value} ${international.dataUnit}"
                valueMbCu.text = "${national.value} ${national.dataUnit}"
                valueMin.text = "${iPackage.minutes} min"
                valueSms.text = "${iPackage.sms} sms"

                //La oferta actual no tiene datos solo para LTE. Los recursos a cero no se muestran.
                hMbLte.visibility = View.GONE
                valueMbLte.visibility = View.GONE
                visibleIf(iPackage.nationalBytes > 0, hMbCu, valueMbCu)
                visibleIf(iPackage.minutes > 0, hMin, valueMin)
                visibleIf(iPackage.sms > 0, hSms, valueSms)
                visibleIf(iPackage.minutes > 0 || iPackage.sms > 0, divider1)

                buttonBuy.setOnClickListener {
                    fragment.purchasePackage(iPackage)
                }
            }
        }

    }

    private fun visibleIf(visible: Boolean, vararg views: View) {
        views.forEach { it.visibility = if (visible) View.VISIBLE else View.GONE }
    }
}

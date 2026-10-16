package com.smartsolutions.paquetes.ui.history

import com.smartsolutions.paquetes.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemHeaderHistoryBinding
import com.smartsolutions.paquetes.databinding.ItemHistoryBinding
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import java.text.SimpleDateFormat
import java.util.*

class HistoryRecyclerAdapter constructor(
    var history: List<IPurchasedPackage>
) : RecyclerView.Adapter<HistoryRecyclerAdapter.AbstractItemHolder>() {

    fun update(historyNew: List<IPurchasedPackage>){
        val old = history
        history = historyNew

        val result = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return old.size
            }

            override fun getNewListSize(): Int {
                return history.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition] == history[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return old[oldItemPosition].date == history[newItemPosition].date
            }
        })

        result.dispatchUpdatesTo(this)
    }


    override fun getItemViewType(position: Int): Int {

        return if (history[position] is HistoryViewModel.HeaderPurchasedPackage) {
            ItemType.HEADER.ordinal
        } else {
            ItemType.ITEM.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ItemType.HEADER.ordinal -> {
                HeaderViewHolder(
                    ItemHeaderHistoryBinding.inflate(inflater, parent, false)
                )
            }
            else -> {
                ItemViewHolder(
                    ItemHistoryBinding.inflate(inflater, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: AbstractItemHolder, position: Int) {
        holder.onBind(history[position])
    }

    override fun getItemCount(): Int {
        return history.size
    }

    abstract class AbstractItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(purchasedPackage: IPurchasedPackage)
    }


    inner class HeaderViewHolder(var binding: ItemHeaderHistoryBinding?) :
        AbstractItemHolder(binding!!.root) {

        override fun onBind(purchasedPackage: IPurchasedPackage) {
            binding?.apply {
                val data = purchasedPackage as HistoryViewModel.HeaderPurchasedPackage
                titleMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(data.date))
                textTotalPrice.text = itemView.context.getString(R.string.price_cup, data.priceTotal)
            }
        }

    }

    inner class ItemViewHolder(var binding: ItemHistoryBinding?) :
        AbstractItemHolder(binding!!.root) {

        override fun onBind(purchasedPackage: IPurchasedPackage) {
            binding?.apply {
                val data = purchasedPackage as PurchasedPackage
                val date = SimpleDateFormat("dd MMM hh:mm aa", Locale.getDefault()).format(
                    Date(data.date)
                )
                // Compra hecha desde la app cuyo SMS de confirmación no se compartió.
                textDate.text = if (data.pending)
                    root.context.getString(R.string.purchase_unconfirmed, date)
                else
                    date
                textPackage.text = data.dataPackage.name
                textPrice.text = itemView.context.getString(R.string.price_cup, data.dataPackage.price.toInt())
            }
        }

    }


    enum class ItemType {
        HEADER,
        ITEM
    }

}
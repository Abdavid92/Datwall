package com.smartsolutions.paquetes.ui.history

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

    private var historyShow = history


    fun update(historyNew: List<IPurchasedPackage>){
        history = historyNew

        val result = DiffUtil.calculateDiff(object: DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return historyShow.size
            }

            override fun getNewListSize(): Int {
                return history.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return historyShow[oldItemPosition] == history[newItemPosition]
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return historyShow[oldItemPosition].date == history[newItemPosition].date
            }
        })

        historyShow = history

        result.dispatchUpdatesTo(this)
    }


    fun filter(header: HistoryViewModel.HeaderPurchasedPackage?){
        if (header == null){
            historyShow = history
            notifyDataSetChanged()
        }else {
            val index = history.indexOf(header)
            historyShow = history.subList(index, index + header.cuantity)
            notifyDataSetChanged()
        }
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
        return historyShow.size
    }

    abstract class AbstractItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun onBind(purchasedPackage: IPurchasedPackage)
    }


    inner class HeaderViewHolder(var binding: ItemHeaderHistoryBinding?) :
        AbstractItemHolder(binding!!.root) {

        override fun onBind(purchasedPackage: IPurchasedPackage) {
            binding?.apply {
                val data = purchasedPackage as HistoryViewModel.HeaderPurchasedPackage
                titleMonth.text = data.month
                textTotalPrice.text = "${data.priceTotal} $"
            }
        }

    }

    inner class ItemViewHolder(var binding: ItemHistoryBinding?) :
        AbstractItemHolder(binding!!.root) {

        override fun onBind(purchasedPackage: IPurchasedPackage) {
            binding?.apply {
                val data = purchasedPackage as PurchasedPackage
                textDate.text = SimpleDateFormat("dd MMM hh:mm aa", Locale.getDefault()).format(
                    Date(data.date)
                )
                textPackage.text = data.dataPackage.name
                textPrice.text = "${data.dataPackage.price.toInt()} $"
            }
        }

    }


    enum class ItemType {
        HEADER,
        ITEM
    }

}
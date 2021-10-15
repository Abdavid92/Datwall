package com.smartsolutions.paquetes.ui.resume

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemUserDataBytesBinding
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils.Companion.nameLegible
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.text.SimpleDateFormat
import java.util.*

class UserDataBytesRecyclerAdapter constructor(
    private val fragment: ResumeHolderFragment,
    var userData: List<UserDataBytes>
): RecyclerView.Adapter<UserDataBytesRecyclerAdapter.ViewHolder>() {

    private var userDataShow = userData

    private val diffUtilCallback = object : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return userDataShow.size
        }

        override fun getNewListSize(): Int {
            return userData.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return userDataShow[oldItemPosition].type == userData[newItemPosition].type
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return userDataShow[oldItemPosition].bytes == userData[newItemPosition].bytes
        }
    }

    fun update(userDataNew: List<UserDataBytes>){
        userData = userDataNew

        val result = DiffUtil.calculateDiff(diffUtilCallback)

        userDataShow = userData

        result.dispatchUpdatesTo(this)
    }


    inner class ViewHolder(private val binding: ItemUserDataBytesBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(userDataBytes: UserDataBytes){
            val usage = DataUnitBytes(userDataBytes.initialBytes - userDataBytes.bytes).getValue()
            val rest = DataUnitBytes(userDataBytes.bytes).getValue()
            val percent = NetworkUsageUtils.calculatePercent(userDataBytes.initialBytes.toDouble(), userDataBytes.bytes.toDouble())
            val expire = if (userDataBytes.expiredTime == 0L){
                "Desconocido"
            }else {
                SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(userDataBytes.expiredTime))
            }
            val restDate = NetworkUsageUtils.calculateDiffDate(System.currentTimeMillis(), userDataBytes.expiredTime)

            binding.progressBar.apply {
                max = 100
                progress = if (percent <= 0){
                    1f
                }else {
                    percent.toFloat()
                }
            }

            binding.dataType.text = userDataBytes.getName(fragment.requireContext())
            binding.textRestValue.text = rest.value.toString()
            binding.textRestUnit.text = rest.dataUnit.name
            binding.textUsageValue.text = usage.value.toString()
            binding.textUsageUnit.text = usage.dataUnit.name
            binding.expireDate.text = expire
            binding.restDate.text = "${restDate.first} ${restDate.second.nameLegible().lowercase()}"

            binding.root.setOnClickListener {
                fragment.showChartUsageGeneral(userDataBytes.type)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemUserDataBytesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(userDataShow[position])
    }

    override fun getItemCount(): Int {
        return userDataShow.size
    }

}
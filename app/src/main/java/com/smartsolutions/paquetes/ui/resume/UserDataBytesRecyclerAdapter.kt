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
    private val context: Context,
    var userData: List<UserDataBytes>
): RecyclerView.Adapter<UserDataBytesRecyclerAdapter.ViewHolder>() {

    private var userDataShow = userData

    fun update(userDataNew: List<UserDataBytes>){
        userData = userDataNew

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

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

        })

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

            binding.progressBar.max = 100
            binding.progressBar.progress = if (percent <= 0){
                1
            }else {
                percent
            }
            binding.dataType.text = userDataBytes.getName(context)
            binding.textRestValue.text = rest.value.toString()
            binding.textRestUnit.text = rest.dataUnit.name
            binding.textUsageValue.text = usage.value.toString()
            binding.textUsageUnit.text = usage.dataUnit.name
            binding.expireDate.text = expire
            binding.restDate.text = "${restDate.first} ${restDate.second.nameLegible().lowercase()}"
            binding.textPercent.text = "${percent}%"
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
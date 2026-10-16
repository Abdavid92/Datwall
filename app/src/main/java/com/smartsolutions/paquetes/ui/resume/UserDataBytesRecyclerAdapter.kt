package com.smartsolutions.paquetes.ui.resume

import com.smartsolutions.paquetes.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.helpers.DisplayTexts
import com.smartsolutions.paquetes.databinding.ItemUserDataBytesBinding
import com.smartsolutions.paquetes.helpers.DataFormatter
import com.smartsolutions.paquetes.helpers.DateCalendarUtils.Companion.nameLegible
import com.smartsolutions.paquetes.managers.UserDataSummary
import com.smartsolutions.paquetes.repositories.models.UserDataBytes

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


    inner class ViewHolder(var binding: ItemUserDataBytesBinding?): RecyclerView.ViewHolder(binding!!.root){

        fun bind(userDataBytes: UserDataBytes) {

            binding?.apply {

                val item = UserDataSummary.item(userDataBytes)
                val usage = DataFormatter.bytes(item.usedBytes)
                val rest = DataFormatter.bytes(item.restBytes)
                val percent = item.percent
                val expire = DataFormatter.expiryDate(userDataBytes.expiredTime)
                    ?: fragment.getString(R.string.unknown_date)
                val restDate = item.timeLeft

                progressBar.apply {
                    max = 100
                    progress = if (percent <= 0){
                        1f
                    }else {
                        percent.toFloat()
                    }
                }

                dataType.text = DisplayTexts.dataTypeName(userDataBytes.type, fragment.requireContext())
                textRestValue.text = rest.value.toString()
                textRestUnit.text = rest.dataUnit.name
                textUsageValue.text = usage.value.toString()
                textUsageUnit.text = usage.dataUnit.name
                expireDate.text = expire

                this.restDate.text = if (restDate.first > 0) {
                    "${restDate.first} ${restDate.second.nameLegible().lowercase()}"
                } else {
                    "-"
                }

                root.setOnClickListener {
                    fragment.showChartUsageGeneral(userDataBytes.type)
                }

                root.setOnLongClickListener {
                    fragment.showEditFragment(userDataBytes.type)
                    return@setOnLongClickListener true
                }
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
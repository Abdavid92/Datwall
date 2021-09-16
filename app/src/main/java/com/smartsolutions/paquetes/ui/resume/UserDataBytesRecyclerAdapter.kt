package com.smartsolutions.paquetes.ui.resume

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemUserDataBytesBinding
import com.smartsolutions.paquetes.repositories.models.UserDataBytes

class UserDataBytesRecyclerAdapter constructor(
    var userData: List<UserDataBytes>
): RecyclerView.Adapter<UserDataBytesRecyclerAdapter.ViewHolder>() {

    class ViewHolder(private val binding: ItemUserDataBytesBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(userDataBytes: UserDataBytes){
            binding.dataType.text = userDataBytes.type.name
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
       holder.bind(userData[position])
    }

    override fun getItemCount(): Int {
        return userData.size
    }

}
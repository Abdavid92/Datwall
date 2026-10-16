package com.smartsolutions.paquetes.ui.permissions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemPermissionBinding
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.models.Permission

/**
 * Fila de la lista de permisos: el permiso y lo que pasará al pulsar su botón.
 * */
data class PermissionItem(val permission: Permission, val action: PermissionAction)

class PermissionsAdapter(
    private val onAction: (PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionsAdapter.ViewHolder>() {

    var items: List<PermissionItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemPermissionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PermissionItem) {
            val granted = item.action == PermissionAction.Nothing

            binding.permissionName.setText(PermissionTexts.name(item.permission))
            binding.description.setText(PermissionTexts.description(item.permission))
            binding.state.setText(
                if (granted) R.string.permission_state_granted else R.string.permission_state_denied
            )
            binding.btnGrant.visibility = if (granted) View.GONE else View.VISIBLE
            binding.btnGrant.setText(
                if (item.action == PermissionAction.OpenAppSettings)
                    R.string.btn_open_settings
                else
                    R.string.btn_grant
            )
            binding.btnGrant.setOnClickListener { onAction(item) }
        }
    }
}

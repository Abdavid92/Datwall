package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.Permission
import moe.feng.common.stepperview.IStepperAdapter
import moe.feng.common.stepperview.VerticalStepperItemView

class StepperAdapter(
    permissions: List<Permission>,
    private val activity: PermissionsActivity
) : IStepperAdapter {

    private val permissions: List<Permission> = listOf(
        *permissions.toTypedArray(),
        Permission(
            "Permisos completados",
            emptyArray(),
            "Ya han sido concedidos todos los permisos necesarios. Puede continuar",
            Permission.Category.Required,
            0,
            { return@Permission true },
            {}
        )
    )

    override fun getTitle(position: Int): CharSequence {
        return permissions[position].name
    }

    override fun getSummary(position: Int): CharSequence? {
        return null
    }

    override fun size() = permissions.size

    override fun onCreateCustomView(position: Int, context: Context?, stepper: VerticalStepperItemView?): View {
        val permission = permissions[position]

        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_permission, stepper, false)

        view.findViewById<MaterialTextView>(R.id.description)
            .text = permission.description

        view.findViewById<AppCompatButton>(R.id.btn_grant).apply {
            if (position == permissions.size - 1) {
                setOnClickListener { activity.nextStep() }
                text = activity.getString(R.string.btn_continue)
            } else {
                setOnClickListener {
                    permissions[position].apply {
                        requestPermissionActivity(activity)
                    }
                }
            }
        }

        view.findViewById<AppCompatButton>(R.id.btn_jump).apply {
            if (permission.category == Permission.Category.Required)
                visibility = View.GONE
            else {
                setOnClickListener {
                    activity.nextStep()
                }
            }
        }

        return view
    }

    override fun onShow(p0: Int) {
    }

    override fun onHide(p0: Int) {
    }
}
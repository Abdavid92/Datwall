package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.Permission
import moe.feng.common.stepperview.IStepperAdapter
import moe.feng.common.stepperview.VerticalStepperItemView

class StepperAdapter(
    private val permissions: List<Permission>,
    private val fragment: PermissionsFragment
) : IStepperAdapter {

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

        view.findViewById<AppCompatButton>(R.id.btn_ok).setOnClickListener {
            permissions[position].apply {
                requestPermissionFragment(fragment)
            }
        }

        view.findViewById<AppCompatButton>(R.id.btn_cancel).apply {
            if (permission.category == Permission.Category.Required)
                visibility = View.GONE
            else {
                text = context?.getString(R.string.jump)
                setOnClickListener {
                    if (stepper?.isLastStep == false)
                        stepper.nextStep()
                    else
                        fragment.notifyFinished()
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
package com.smartsolutions.paquetes.ui.permissions

import android.app.Activity
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
    private val permissions: List<Permission>,
    private val activity: Activity
) : IStepperAdapter {

    override fun getTitle(position: Int): CharSequence {
        return permissions[position].name
    }

    override fun getSummary(position: Int): CharSequence? {
        return null
    }

    override fun size() = permissions.size

    override fun onCreateCustomView(position: Int, context: Context?, stepper: VerticalStepperItemView?): View {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_permission, stepper, false)

        view.findViewById<MaterialTextView>(R.id.description)
            .text = permissions[position].description

        view.findViewById<AppCompatButton>(R.id.btn_ok).setOnClickListener {
            permissions[position].apply {
                requestPermission(activity)
            }
        }

        view.findViewById<AppCompatButton>(R.id.btn_cancel).apply {
            text = context?.getString(R.string.jump)
            visibility = View.GONE
            setOnClickListener {
                stepper?.nextStep()
            }
        }

        return view
    }

    override fun onShow(p0: Int) {
    }

    override fun onHide(p0: Int) {
    }
}
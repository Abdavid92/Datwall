package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import com.smartsolutions.paquetes.R

abstract class TransparentActivity : AbstractActivity {

    constructor() : super()

    constructor(@LayoutRes contentLayoutResId: Int) : super(contentLayoutResId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.setBackgroundResource(R.drawable.transparent_background_dark)
    }
}
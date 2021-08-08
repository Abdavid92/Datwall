package com.smartsolutions.paquetes.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import com.smartsolutions.paquetes.R
import kotlin.properties.Delegates

/**
 * TODO: document your custom view class.
 */
class ExpandingItemView : LinearLayout {

    private var mHeaderId by Delegates.notNull<Int>()
    private lateinit var mHeader: ViewGroup

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        loadAttributes(attrs, 0)
        initViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        loadAttributes(attrs, defStyle)
        initViews()
    }

    private fun loadAttributes(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.ExpandingItemView, defStyle, 0
        )

        try {
            mHeaderId = a.getResourceId(R.styleable.ExpandingItemView_layout_header, 0)
        } finally {
            a.recycle()
        }
    }

    private fun initViews() {
        val inflater = LayoutInflater.from(context)

        mHeader = inflater.inflate(mHeaderId, this, false) as ViewGroup
    }
}
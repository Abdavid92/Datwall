package com.smartsolutions.paquetes.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.smartsolutions.paquetes.R
import java.lang.Exception
import kotlin.math.min
import kotlin.properties.Delegates


class CircleView : View {


    private lateinit var circlePaint: Paint
    private var centerX by Delegates.notNull<Float>()
    private var centerY by Delegates.notNull<Float>()
    private var radius by Delegates.notNull<Float>()

    var circleColor: Int = Color.GRAY
        set(value) {
            field = value
            try {
                circlePaint.color = value
            } catch (e: Exception) {}
            invalidate()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes

        circlePaint = Paint()
        circlePaint.style = Paint.Style.FILL

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CircleView, defStyle, 0)
        circleColor = attributes.getColor(R.styleable.CircleView_circleColor, circleColor)

        attributes.recycle()

        circlePaint.color = circleColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)

        val size = min(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2F
        centerY = h / 2F
        radius = min(w, h) / 2F
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, circlePaint)
    }
}

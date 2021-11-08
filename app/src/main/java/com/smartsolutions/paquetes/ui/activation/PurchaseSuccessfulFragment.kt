package com.smartsolutions.paquetes.ui.activation

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

class PurchaseSuccessfulFragment : AbstractSettingsFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(
            R.layout.fragment_purchase_successful,
            container,
            false
        ).apply {

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confetti = view.findViewById<KonfettiView>(R.id.confetti_background)

        confetti.build()
            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
            .setDirection(0.0, 180.0)
            .setSpeed(0.1f, 1f)
            .setFadeOutEnabled(true)
            .setTimeToLive(500L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(Size(10, 3f))
            .setPosition(-50f, confetti.width + 1000f, -50f, -50f)
            .streamFor(100, 3000L)

        view.findViewById<Button>(R.id.btn_continue).setOnClickListener {
            complete()
        }
    }
}
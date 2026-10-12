package com.smartsolutions.paquetes.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * En Android 14+ registrar un receiver de una acción propia sin indicar si es
 * exportado lanza SecurityException. Esta prueba falla con el registro anterior.
 * */
@RunWith(AndroidJUnit4::class)
class USSDHelperReceiverTest {

    @Test
    fun responseReceiverReceivesResponseFromOwnPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)
        var response: Array<CharSequence>? = null

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                response = intent.getCharSequenceArrayExtra(USSDHelper.EXTRA_RESPONSE)
                latch.countDown()
            }
        }

        USSDHelper.registerResponseReceiver(context, receiver)

        try {
            context.sendBroadcast(
                USSDHelper.createResponseIntent(context, arrayOf("Saldo: 10.00 CUP"))
            )

            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertEquals("Saldo: 10.00 CUP", response?.first()?.toString())
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}

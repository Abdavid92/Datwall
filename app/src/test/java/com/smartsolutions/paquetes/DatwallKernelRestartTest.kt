package com.smartsolutions.paquetes

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

class DatwallKernelRestartTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        val activityManager = mockk<ActivityManager>()
        every { context.getSystemService("activity") } returns activityManager
        every { context.getSystemService(ActivityManager::class.java) } returns activityManager

        // El kernel observa este almacén al crearse.
        mockkStatic("com.smartsolutions.paquetes.PreferencesKeysKt")
        every { context.internalDataStore } returns mockk<DataStore<Preferences>> {
            every { data } returns emptyFlow()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.smartsolutions.paquetes.PreferencesKeysKt")
    }

    private fun kernel() = spyk(
        DatwallKernel(
            context, mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(),
            mockk(), mockk(), mockk(), mockk(), mockk()
        )
    )

    @Test
    fun restartForgetsTheOldDestinationAndRunsTheStartOnce() = runBlocking<Unit> {
        val kernel = kernel()
        val entered = CompletableDeferred<Unit>()
        coEvery { kernel.main() } coAnswers { entered.complete(Unit) }

        kernel.restart()

        withTimeout(5_000) { entered.await() }
        verify(exactly = 1) { kernel.clearStartDestination() }
        coVerify(exactly = 1) { kernel.main() }
    }

    @Test
    fun aRestartWhileTheStartIsRunningDoesNotLaunchAnotherOne() = runBlocking<Unit> {
        val kernel = kernel()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { kernel.main() } coAnswers {
            entered.complete(Unit)
            release.await()
        }

        kernel.restart()
        withTimeout(5_000) { entered.await() }

        kernel.restart()

        coVerify(exactly = 1) { kernel.main() }

        release.complete(Unit)
    }

    @Test
    fun afterTheStartFinishesItCanBeRestartedAgain() = runBlocking<Unit> {
        val kernel = kernel()
        var runs = 0
        val second = CompletableDeferred<Unit>()
        coEvery { kernel.main() } coAnswers {
            runs++
            if (runs == 2) second.complete(Unit)
        }

        kernel.restart()
        // Espera a que termine la primera ejecución antes de pedir otra.
        withTimeout(5_000) {
            while (runs < 1) kotlinx.coroutines.delay(10)
            kernel.restart()
            second.await()
        }

        coVerify(exactly = 2) { kernel.main() }
    }
}

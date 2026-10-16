package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PurchasedPackagesManagerTest {

    private fun purchase(id: Long, pending: Boolean) = PurchasedPackage(
        id, id, IDataPackageManager.ConnectionMode.USSD, "2", pending, DataPackages.PackageId.DailyBag
    )

    @Test
    fun clearHistoryKeepsUnconfirmedPurchases() = runTest {
        val confirmed = purchase(1, pending = false)
        val pending = purchase(2, pending = true)

        val repository = mockk<IPurchasedPackageRepository> {
            every { getAll() } returns flowOf(listOf(confirmed, pending))
            coEvery { delete(any<List<PurchasedPackage>>()) } returns 1
        }

        PurchasedPackagesManager(mockk(relaxed = true), repository, mockk(relaxed = true)).clearHistory()

        coVerify(exactly = 1) { repository.delete(listOf(confirmed)) }
    }
}

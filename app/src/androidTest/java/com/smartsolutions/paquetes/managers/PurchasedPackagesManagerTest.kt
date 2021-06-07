package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

import org.junit.Rule
import javax.inject.Inject

@HiltAndroidTest
class PurchasedPackagesManagerTest {

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Inject
    lateinit var purchasedPackagesManager: PurchasedPackagesManager

    val id = createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price)

    @Before
    fun setUp() {
        rule.inject()
    }

    @Test
    fun newPurchased() {
        runBlocking {

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                1,
                IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)

            purchasedPackagesManager
                .newPurchased(
                    createDataPackageId(DataPackagesContract.P_1Gb.name, DataPackagesContract.P_1Gb.price),
                    1,
                    IDataPackageManager.BuyMode.USSD)
        }
    }

    @Test
    fun confirmPurchased() {
        runBlocking {
            purchasedPackagesManager.confirmPurchased(id, 1)
        }
    }
}
package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.SmsInboxReaderHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject

class PurchasedPackagesManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val purchasedPackageRepository: IPurchasedPackageRepository,
    private val smsReader: SmsInboxReaderHelper,
    private val simDelegate: SimDelegate,
    private val simManager: ISimManager
) : IPurchasedPackagesManager {

    override suspend fun newPurchased(
        dataPackageId: DataPackages.PackageId,
        simId: String,
        buyMode: IDataPackageManager.ConnectionMode,
        pending: Boolean
    ) {
        val purchasedPackage = PurchasedPackage(
            0,
            System.currentTimeMillis(),
            buyMode,
            simId,
            pending,
            dataPackageId
        )
        purchasedPackageRepository.create(purchasedPackage)
    }

    override suspend fun confirmPurchased(dataPackageId: DataPackages.PackageId, simId: String) {
        val pending = purchasedPackageRepository
            .getPending(dataPackageId)
            .first()
            .toMutableList()

        if (pending.isNotEmpty()) {

            val pendingToDelete = mutableListOf<PurchasedPackage>()

            pending.forEach {
                if (System.currentTimeMillis() - it.date > DateUtils.MILLIS_PER_DAY) {
                    purchasedPackageRepository.delete(it)
                    pendingToDelete.add(it)
                }
            }

            pending.removeAll(pendingToDelete)

            if (pending.isNotEmpty()) {
                val pendingToConfirmed = pending.firstOrNull { it.simId == simId }

                if (pendingToConfirmed != null) {
                    pendingToConfirmed.pending = false
                    purchasedPackageRepository.update(pendingToConfirmed)
                } else {
                    newPurchased(
                        dataPackageId,
                        simId,
                        IDataPackageManager.ConnectionMode.Unknown,
                        false
                    )
                }
            }
        }
    }

    override fun getHistory(): Flow<List<PurchasedPackage>> =
        purchasedPackageRepository.getAll()

    override suspend fun clearHistory() {
        purchasedPackageRepository
            .getAll()
            .firstOrNull()?.let {
                purchasedPackageRepository.delete(it)
            }
    }


    override suspend fun seedOldPurchasedPackages() {
        if (context.dataStore.data.firstOrNull()?.get(PreferencesKeys.IS_SEED_OLD_PURCHASED_PACKAGES) != true) {

            context.dataStore.edit {
                it[PreferencesKeys.IS_SEED_OLD_PURCHASED_PACKAGES] = true
            }

            val smses = smsReader.getAllSmsReceived().filter { it.number.contains("cubacel", true) }
            val packages = mutableListOf<PurchasedPackage>()

            try {
                smses.forEach { sms ->
                    for (pack in DataPackages.PACKAGES) {
                        if (sms.body.contains(pack.smsKey, true)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                simDelegate.getSubcriptionInfo(sms.subscriptionId.toInt())?.let {
                                    packages.add(
                                        PurchasedPackage(
                                            System.currentTimeMillis(),
                                            sms.date,
                                            IDataPackageManager.ConnectionMode.USSD,
                                            simDelegate.getSimId(it),
                                            false,
                                            pack.id
                                        )
                                    )
                                }
                            }else {
                                packages.add(
                                    PurchasedPackage(
                                        System.currentTimeMillis(),
                                        sms.date,
                                        IDataPackageManager.ConnectionMode.USSD,
                                        simManager.getDefaultSim(SimDelegate.SimType.DATA).id,
                                        false,
                                        pack.id
                                    )
                                )
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
            }

            purchasedPackageRepository.create(packages)
        }
    }

}
package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import org.apache.commons.lang3.time.DateUtils

object PendingPurchaseSelector {

    /**
     * Compra sin confirmar que confirma un SMS de compra de [dataPackageId] en [simId]: la más
     * reciente de las últimas 24 horas. La app solo espera el SMS 5 minutos tras comprar, así
     * que una compra más antigua no puede ser la del SMS.
     * */
    fun select(
        purchases: List<PurchasedPackage>,
        dataPackageId: DataPackages.PackageId,
        simId: String,
        now: Long
    ): PurchasedPackage? =
        purchases
            .filter {
                it.pending && it.dataPackageId == dataPackageId && it.simId == simId &&
                        now - it.date <= DateUtils.MILLIS_PER_DAY
            }
            .maxByOrNull { it.date }
}

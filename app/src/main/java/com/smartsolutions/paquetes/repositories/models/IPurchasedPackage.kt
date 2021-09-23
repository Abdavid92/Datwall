package com.smartsolutions.paquetes.repositories.models

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager

interface IPurchasedPackage{

    val id: Long

    val date: Long

    val origin: IDataPackageManager.ConnectionMode

    var simId: String

    var pending: Boolean

    val dataPackageId: DataPackages.PackageId

}
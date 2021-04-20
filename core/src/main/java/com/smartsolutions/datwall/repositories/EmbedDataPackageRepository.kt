package com.smartsolutions.datwall.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartsolutions.datwall.repositories.models.DataPackage

class EmbedDataPackageRepository: IDataPackageRepository {

    private var increment = 1

    private val packages = mutableListOf<DataPackage>()

    override fun getAll(): LiveData<List<DataPackage>> =
        MutableLiveData(packages)

    override suspend fun get(id: Int): DataPackage? =
        packages.firstOrNull { it.id ==  id }

    override suspend fun create(dataPackage: DataPackage): Long {
        throw NotImplementedError()
        /*packages.add(dataPackage.copy(
            id = ++increment,
            name = dataPackage.name,
            description = dataPackage.description,
            price = dataPackage.price,
            bytes = dataPackage.bytes,
            bonoBytes = dataPackage.bonoBytes,
            bonoCuBytes = dataPackage.bonoCuBytes,
            network = dataPackage.network,
            ussd = dataPackage.ussd,
            url = dataPackage.url,
            active = dataPackage.active
        ))

        return increment.toLong()*/
    }

    override suspend fun create(dataPackages: List<DataPackage>): List<Long> {
        throw NotImplementedError()
    }

    override suspend fun update(dataPackage: DataPackage): Int {
        throw NotImplementedError()
    }

    override suspend fun update(dataPackages: List<DataPackage>): Int {
        throw NotImplementedError()
    }

    override suspend fun delete(dataPackage: DataPackage): Int {
        if (packages.remove(dataPackage))
            return 1
        return 0
    }

    override suspend fun delete(dataPackages: List<DataPackage>): Int {

        var deleted = 0

        dataPackages.forEach {
            if (packages.remove(it))
                deleted++
        }

        return deleted
    }
}
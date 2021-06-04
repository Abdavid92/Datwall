package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IMiCubacelAccountDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MiCubacelAccountRepository @Inject constructor(
    private val miCubacelAccountDao: IMiCubacelAccountDao,
    private val simDao: ISimDao
) : IMiCubacelAccountRepository {

    override suspend fun create(account: MiCubacelAccount) {
        miCubacelAccountDao.create(account)
    }

    override suspend fun create(accounts: List<MiCubacelAccount>) {
        miCubacelAccountDao.create(accounts)
    }

    override suspend fun createOrUpdate(account: MiCubacelAccount) {
        if (miCubacelAccountDao.get(account.simId) == null) {
            miCubacelAccountDao.create(account)
        } else {
            miCubacelAccountDao.update(account)
        }
    }

    override suspend fun all(): List<MiCubacelAccount> =
        miCubacelAccountDao.all().map {
            transform(it)
        }

    override fun flow(): Flow<List<MiCubacelAccount>> =
        miCubacelAccountDao.flow().map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun get(id: String): MiCubacelAccount? =
        miCubacelAccountDao.get(id)?.apply {
            transform(this)
        }

    override suspend fun getByPhone(phone: String): MiCubacelAccount? =
        miCubacelAccountDao.getByPhone(phone)?.apply {
            transform(this)
        }

    override suspend fun update(account: MiCubacelAccount) =
        miCubacelAccountDao.update(account)

    override suspend fun update(accounts: List<MiCubacelAccount>) =
        miCubacelAccountDao.update(accounts)

    override suspend fun delete(account: MiCubacelAccount) {
        miCubacelAccountDao.delete(account)
    }

    private suspend fun transform(account: MiCubacelAccount): MiCubacelAccount {
        simDao.get(account.simId)?.let {
            account.sim = it
        }
        return account
    }
}
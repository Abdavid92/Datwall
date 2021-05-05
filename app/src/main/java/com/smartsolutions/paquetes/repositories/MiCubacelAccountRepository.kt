package com.smartsolutions.paquetes.repositories

import android.content.Context
import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.commons.lang.SerializationUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class MiCubacelAccountRepository @Inject constructor(
    @ApplicationContext
    private val context: Context
): IMiCubacelAccountRepository {

    private val accounts = "accounts.bin"

    override suspend fun create(account: MiCubacelAccount): Boolean {
        val list = read()
        if (list.size < 2 && !list.contains(account)) {
            list.add(account)
            return write(list)
        }
        return false
    }

    override suspend fun update(account: MiCubacelAccount): Boolean {
        val list = read()
        val index = list.indexOf(account)

        if (index != -1) {
            list[index] = account
            return write(list)
        }
        return false
    }

    override suspend fun createOrUpdate(account: MiCubacelAccount): Boolean {
        if (!update(account))
            return create(account)
        return true
    }

    override suspend fun delete(account: MiCubacelAccount): Boolean {
        val list = read()

        if (list.remove(account))
            return write(list)
        return false
    }

    override fun get(phone: String): Flow<MiCubacelAccount> =
        flow {
            read().firstOrNull { it.phone == phone }?.let {
                emit(it)
            }
        }

    override fun get(simIndex: Int): Flow<MiCubacelAccount> =
        flow {
            read().firstOrNull { it.simIndex == simIndex }?.let {
                emit(it)
            }
        }

    override fun getAll(): Flow<List<MiCubacelAccount>> =
        flow {
            emit(read())
        }

    private fun read(): MutableList<MiCubacelAccount> {
        synchronized(this) {
            val file = File(context.filesDir, accounts)

            if (!file.exists())
                return mutableListOf()

            return try {
                mutableListOf(*SerializationUtils.deserialize(FileInputStream(file)) as Array<out MiCubacelAccount>)
            } catch (e: Exception) {
                file.delete()
                mutableListOf()
            }
        }
    }

    private fun write(list: List<MiCubacelAccount>): Boolean {
        synchronized(this) {
            val file = File(context.filesDir, accounts)

            try {
                if (!file.exists() && !file.createNewFile())
                    return false

                SerializationUtils.serialize(list.toTypedArray(), FileOutputStream(file))
                return true
            } catch (e: Exception) {

            }
            return false
        }
    }
}
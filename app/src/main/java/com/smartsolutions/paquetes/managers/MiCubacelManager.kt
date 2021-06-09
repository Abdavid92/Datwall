package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.exceptions.NotFoundException
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.micubacel.MCubacelClient
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import com.smartsolutions.paquetes.serverApis.models.Result
import javax.inject.Inject

class MiCubacelManager @Inject constructor(
    private val miCubacelAccountRepository: IMiCubacelAccountRepository,
    private val client: MCubacelClient,
    private val simManager: SimManager,
    private val userDataBytesManager: IUserDataBytesManager
): BaseMiCubacelManager() {

    private val attempts = 9

    override suspend fun signIn(account: MiCubacelAccount): Result<Unit> {
        return try {
            account.cookies = sendRequests(attempts) {
                client.signIn(account.phone, account.password)
            }

            miCubacelAccountRepository.createOrUpdate(account)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun signUp(firstName: String, lastName: String, phone: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun verifyCode(code: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun createPassword(password: String): Result<MiCubacelAccount> {
        TODO("Not yet implemented")
    }

    override suspend fun synchronizeUserDataBytes(account: MiCubacelAccount?): Result<Unit> {

        val account1 = account ?: miCubacelAccountRepository.get(simManager.getDefaultDataSim().id)

        account1?.let {
            return try {
                getUserDataBytes(account1)
                Result.Success(Unit)

            } catch (e: Exception) {
                loginAndRetry(e, account1) { getUserDataBytes(account1) }
            }
        }

        return Result.Failure(NotFoundException())
    }

    override suspend fun getProducts(account: MiCubacelAccount): Result<List<ProductGroup>> {
        MCubacelClient.COOKIES = account.cookies.toMutableMap()

        return try {
            Result.Success(
                sendRequests(attempts) { client.getProducts() }
            )
        } catch (e: Exception) {
            return loginAndRetry(e, account) { client.getProducts() }
        }
    }

    override suspend fun buyProduct(url: String): Result<Unit> {
        return try {
            val urlConfirmation = sendQueueRequests(attempts) {
                client.resolveUrlBuyProductConfirmation(url)
            }

            sendQueueRequests(attempts) {
                client.buyProduct(urlConfirmation)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private suspend fun getUserDataBytes(account: MiCubacelAccount) {
        MCubacelClient.COOKIES = account.cookies.toMutableMap()
        val data = sendRequests(attempts) { client.obtainPackagesInfo() }
        userDataBytesManager.synchronizeUserDataBytes(data, account.simId)
    }
}
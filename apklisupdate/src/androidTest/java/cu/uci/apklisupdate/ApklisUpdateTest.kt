package cu.uci.apklisupdate

import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*

class ApklisUpdateTest {

    @Test
    fun getAppUpdate() {
        runBlocking {

            val result = ApklisUpdate.getAppUpdate("com.smartsolutions.paquetes")

            assertTrue(result.isSuccess)
        }
    }
}
package com.smartsolutions.datwall

import com.smartsolutions.datwall.webApis.IMiCubacelApi
import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class MiCubacelApiTest {

    @Inject
    lateinit var api : IMiCubacelApi

    @get:Rule
    val rule = HiltAndroidRule(this)

    @Before
    fun setUp(){
        rule.inject()
    }

    @Test
    fun joinUp(){
        val account = MiCubacelAccount("Elier", "Viera", "52379969","abcdefgh", "sdasdadas", null, null, true)

        runBlocking {
            val result = api.signUpNewUser(account)

            val resultt = api.signUpVerifyRegistration("1234")

            val resulte = api.signUpEnterPassword(account)
        }

    }

    @Test
    fun signIn() {
        val account = MiCubacelAccount(
            "Abel",
        "Llera",
            "55055870",
            "Abel.2021",
            "Abel.2021",
            null,
            null
        )

        runBlocking {

            val result = api.signIn(account)


        }
    }

}
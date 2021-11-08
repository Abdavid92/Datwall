package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.os.Build
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SmsInboxReaderHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {


    suspend fun getAllSmsReceived(): List<SMS>{
        val list = mutableListOf<SMS>()

        withContext(Dispatchers.IO) {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER
            )?.use { cursor ->
                if (cursor.moveToFirst()){
                    repeat(cursor.count){
                        kotlin.runCatching {
                            list.add(
                                SMS(
                                    cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)),
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))
                                    } else {
                                        -1L
                                    }
                                )
                            )
                        }.onFailure {
                            Log.i("SMS_READER", "Failure ${it.message}")
                        }
                        if (!cursor.moveToNext()){
                            return@repeat
                        }
                    }
                }
            }
        }

        return list
    }


    data class SMS(
        var date: Long,
        var number: String,
        var body: String,
        var subscriptionId: Long
    )

}
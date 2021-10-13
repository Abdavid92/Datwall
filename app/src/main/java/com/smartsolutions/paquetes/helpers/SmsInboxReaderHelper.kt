package com.smartsolutions.paquetes.helpers

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class SmsInboxReaderHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getAllSmsReceived(): List<SMS>{
        val list = mutableListOf<SMS>()

        context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, Telephony.Sms.DEFAULT_SORT_ORDER)?.let { cursor ->

            while (cursor.moveToNext()){
                kotlin.runCatching {
                    list.add(SMS(
                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.SUBSCRIPTION_ID))
                    ))
                }
            }

            cursor.close()
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
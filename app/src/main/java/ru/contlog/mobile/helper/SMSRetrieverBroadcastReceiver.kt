package ru.contlog.mobile.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status


class SMSRetrieverBroadcastReceiver(private val onSmsReceived: (String, String?) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e(TAG, "onReceive: Получили Broadcast, но intent == null!")
            return
        }

        val extras = intent.extras
        if (extras == null) {
            Log.e(TAG, "onReceive: Получили Broadcast с нормальным intent, но extras == null!")
            return
        }

        val status = extras.get(SmsRetriever.EXTRA_STATUS) as Status?
        if (status == null) {
            Log.e(
                TAG,
                "onReceive: Получили Broadcast с нормальным intent и extras, но SmsRetriever.EXTRA_STATUS даёт null!"
            )
            return
        }

        when (status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                val senderAddress = extras.getString(SmsRetriever.EXTRA_SMS_ORIGINATING_ADDRESS, "N/A") // N/A = NO ACKNOWLEDGMENT
                val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)

                onSmsReceived(senderAddress, message)
            }

            CommonStatusCodes.TIMEOUT -> {
                Log.e(
                    TAG,
                    "onReceive: SMSRetrieverBroadcastReceiver получил сообщение, что произошёл таймаут ожидания СМС!"
                )
            }
        }
    }

    companion object {
        const val TAG = "Contlog.SMSRetrieverBroadcastReceiver"
    }
}
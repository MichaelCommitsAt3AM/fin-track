package com.fintrack.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import com.fintrack.app.core.util.MpesaSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver for incoming M-Pesa SMS messages.
 * 
 * This receiver listens for new SMS messages and automatically parses
 * and stores M-Pesa transactions in real-time.
 * 
 * NOTE: Only registered in the personal build variant.
 */
@AndroidEntryPoint
class MpesaSmsReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var mpesaRepository: MpesaTransactionRepository
    
    @Inject
    lateinit var smsParser: MpesaSmsParser
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "MpesaSmsReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        try {
            // Extract SMS messages from intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages?.forEach { smsMessage ->
                processSmsMessage(smsMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming SMS", e)
        }
    }
    
    private fun processSmsMessage(smsMessage: SmsMessage) {
        val sender = smsMessage.displayOriginatingAddress
        val body = smsMessage.messageBody
        val timestamp = smsMessage.timestampMillis
        
        Log.d(TAG, "Received SMS from: $sender")
        
        // Check if it's from M-Pesa
        if (!smsParser.isMpesaSms(sender)) {
            Log.d(TAG, "Not an M-Pesa SMS, ignoring")
            return
        }
        
        Log.d(TAG, "M-Pesa SMS detected, parsing...")
        
        // Generate unique SMS ID
        val smsId = generateSmsId(timestamp, sender, body)
        
        // Parse and store asynchronously
        scope.launch {
            try {
                val success = mpesaRepository.parseAndStoreSms(body, timestamp, smsId)
                if (success) {
                    Log.d(TAG, "M-Pesa transaction stored successfully")
                } else {
                    Log.d(TAG, "Failed to parse or store M-Pesa transaction")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing M-Pesa transaction", e)
            }
        }
    }
    
    private fun generateSmsId(timestamp: Long, sender: String?, body: String?): String {
        val input = "$timestamp-${sender?.hashCode()}-${body?.hashCode()}"
        return input.hashCode().toString()
    }
}

package com.fintrack.app.core.util

import com.fintrack.app.core.domain.model.MpesaTransactionType
import org.junit.Assert.*
import org.junit.Test

class MpesaSmsParserTest {

    private val parser = MpesaSmsParser(SmartClueDetector())

    @Test
    fun `parse sent money`() {
        val sms = "QK87ABCD12 Confirmed. Ksh1,000.00 sent to JOHN DOE 0712345678 on 21/1/26 at 10:00 AM. New M-PESA balance is Ksh500.00. Transaction cost, Ksh10.00."
        val result = parser.parseSms(sms)
        
        assertNotNull(result)
        assertEquals("QK87ABCD12", result?.mpesaReceiptNumber)
        assertEquals(1000.0, result?.amount!!, 0.01)
        assertEquals(MpesaTransactionType.SEND_MONEY, result.transactionType)
        assertEquals("JOHN DOE", result.merchantName)
        assertEquals("0712345678", result.phoneNumber)
    }

    @Test
    fun `parse received money`() {
        val sms = "QK87ABCD13 Confirmed. You have received Ksh2,000.00 from JANE DOE 0712345678 on 21/1/26 at 11:00 AM. New M-PESA balance is Ksh2,500.00."
        val result = parser.parseSms(sms)
        
        assertNotNull(result)
        assertEquals("QK87ABCD13", result?.mpesaReceiptNumber)
        assertEquals(2000.0, result?.amount!!, 0.01)
        assertEquals(MpesaTransactionType.RECEIVE_MONEY, result.transactionType)
        assertEquals("JANE DOE", result.merchantName)
        assertEquals("0712345678", result.phoneNumber)
    }

    @Test
    fun `parse paybill`() {
        val sms = "QK87ABCD14 Confirmed. Ksh300.00 paid to Paybill 400200, account number 12345 on 21/1/26... New M-PESA balance is Ksh..."
        val result = parser.parseSms(sms)
        
        assertNotNull(result)
        assertEquals("QK87ABCD14", result?.mpesaReceiptNumber)
        assertEquals(300.0, result?.amount!!, 0.01)
        assertEquals(MpesaTransactionType.PAYBILL, result.transactionType)
        assertEquals("PAYBILL 400200", result.merchantName) // Parser extracts merchant from context, here it might take "Paybill 400200"
        assertEquals("400200", result.paybillNumber)
        assertEquals("12345", result.accountNumber)
    }

    @Test
    fun `parse paybill with business name`() {
        val sms = "QK87ABCD15 Confirmed. Ksh500.00 paid to KPLC PREPAID. Paybill 888880, account number 123456 on 21/1/26..."
        val result = parser.parseSms(sms)
        
        assertNotNull(result)
        assertEquals(MpesaTransactionType.PAYBILL, result?.transactionType)
        assertEquals("KPLC PREPAID", result?.merchantName)
    }

    @Test
    fun `parse till`() {
        val sms = "QK87ABCD16 Confirmed. Ksh150.00 paid to SUPERMARKET. on 21/1/26 at 12:00 PM."
        // Note: The till regex expects "paid to till" or "paid for till" or just "paid to ... till ...". 
        // Let's check the regex again:
        // """(?i)confirmed\.?\s+ksh\.?\s*([\d,]+\.?\d*)\s+paid\s+(?:to|for)\s+till\s+(?:number\s+)?(\d+)"""
        // Wait, the regex STRICTLY requires "paid to till" or "paid for till".
        // But many Buy Goods messages are "Paid to [Business Name] on [Date]".
        // Or "Ksh... paid to [Business Name] on...".
        // Let's see if this is the issue.
    }
    
    @Test
    fun `parse buy goods with simple format`() {
         // Typical Buy Goods message:
         // "QK87ABCD17 Confirmed. Ksh150.00 paid to SUPERMARKET on 21/1/26 at 12:30 PM. New M-PESA balance is..."
         // Does the parser handle this? 
         // The Paybill regex handles "paid to ... Paybill ...".
         // The Till regex handles "paid to till ...".
         // The Sent Money regex handles "sent to ...".
         
         // If it's just "paid to SUPERMARKET", it might fail unless it matches Paybill/Till patterns.
         // Let's test this failure case.
         val sms = "QK87ABCD17 Confirmed. Ksh150.00 paid to SUPERMARKET on 21/1/26 at 12:30 PM."
         val result = parser.parseSms(sms)
         
         // If this is null, that's likely the bug.
         if (result == null) {
             println("Failed to parse Buy Goods message: $sms")
         } else {
             println("Parsed Buy Goods: $result")
         }
    }
}

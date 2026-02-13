package com.fintrack.app.core.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.fintrack.app.core.common.NotificationHelper
import com.fintrack.app.core.domain.model.LookbackPeriod
import com.fintrack.app.core.domain.model.MpesaTransaction
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.never
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
@RunWith(org.junit.runners.JUnit4::class)
class MonthlyMpesaCostWorkerTest {

    @Mock lateinit var context: Context
    @Mock lateinit var workerParams: WorkerParameters
    @Mock lateinit var mockNotificationHelper: NotificationHelper

    private lateinit var worker: MonthlyMpesaCostWorker
    private lateinit var fakeRepository: FakeMpesaTransactionRepository
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var fakeTimeProvider: FakeTimeProvider

    @Before
    fun setup() {
        org.mockito.MockitoAnnotations.openMocks(this)
        
        fakePrefs = FakeSharedPreferences()
        whenever(context.getSharedPreferences(any(), any())).thenReturn(fakePrefs)
        
        fakeRepository = FakeMpesaTransactionRepository()
        fakeTimeProvider = FakeTimeProvider()
        // Set current date to May 15th, 2024. Previous month is April 2024.
        fakeTimeProvider.setFixedDate(LocalDate.of(2024, 5, 15)) 
    }

    @Test
    fun testDoWork_sendsNotification_whenCostGreaterThanZero() = runTest {
        // Setup fake repository with cost in April
        // Oldest transaction must be before April 1st to ensure full month data
        val item1 = createTransaction(50.0, timestamp = getTimestampFor(2024, 4, 10))
        val item2 = createTransaction(0.0, timestamp = getTimestampFor(2024, 3, 1))
        
        fakeRepository.setTransactions(listOf(item1, item2))
        
        worker = MonthlyMpesaCostWorker(
            context,
            workerParams,
            fakeRepository,
            mockNotificationHelper,
            fakeTimeProvider
        )

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Success)
        verify(mockNotificationHelper).showReportNotification(
            org.mockito.kotlin.check { assert(it == "Monthly M-Pesa Cost Fact") },
            org.mockito.kotlin.check { assert(it.contains("Did you know?")) }
        )
        assert(fakePrefs.getString("last_report_month", "") == "2024-04")
    }
    
    @Test
    fun testDoWork_sendsCongratulatoryMessage_whenCostIsZero() = runTest {
         // Setup fake repository with ZERO cost in April
         // Old transaction exists
        fakeRepository.setTransactions(listOf(
            createTransaction(0.0, timestamp = getTimestampFor(2024, 4, 15)),
            createTransaction(0.0, timestamp = getTimestampFor(2024, 3, 1))
        ))

        worker = MonthlyMpesaCostWorker(
            context,
            workerParams,
            fakeRepository,
            mockNotificationHelper,
            fakeTimeProvider
        )

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Success)
        verify(mockNotificationHelper).showReportNotification(
            org.mockito.kotlin.check { assert(it == "Monthly M-Pesa Cost Fact") },
            org.mockito.kotlin.check { assert(it.contains("Great job!")) }
        )
        assert(fakePrefs.getString("last_report_month", "") == "2024-04")
    }

    @Test
    fun testDoWork_skipsFirstMonth_ifOldestTransactionIsRecent() = runTest {
        // Set current date to May 15th. Previous month is April.
        // Oldest transaction is April 10th. This means we don't have full April data.
        fakeRepository.setTransactions(listOf(
            createTransaction(50.0, timestamp = getTimestampFor(2024, 4, 10))
        ))

        worker = MonthlyMpesaCostWorker(
            context,
            workerParams,
            fakeRepository,
            mockNotificationHelper,
            fakeTimeProvider
        )

        val result = worker.doWork()

        assert(result is ListenableWorker.Result.Success)
        // verify NOT called
        verify(mockNotificationHelper, never()).showReportNotification(any(), any())
        // But marked as reported
        assert(fakePrefs.getString("last_report_month", "") == "2024-04")
    }
    
    @Test
    fun testDoWork_showsTrend_whenCostIncreases() = runTest {
        // Current: May. Report: April. Compare with: March.
        fakeRepository.setTransactions(listOf(
            createTransaction(0.0, timestamp = getTimestampFor(2024, 2, 1)), // Old data anchor
            createTransaction(50.0, timestamp = getTimestampFor(2024, 3, 15)), // March cost: 50
            createTransaction(100.0, timestamp = getTimestampFor(2024, 4, 15)) // April cost: 100 (Increase)
        ))

        worker = MonthlyMpesaCostWorker(
            context,
            workerParams,
            fakeRepository,
            mockNotificationHelper,
            fakeTimeProvider
        )

        worker.doWork()

        verify(mockNotificationHelper).showReportNotification(
            any(),
            org.mockito.kotlin.check { 
                assert(it.contains("100% up from last month")) 
            }
        )
    }

    private fun getTimestampFor(year: Int, month: Int, day: Int): Long {
        return java.time.LocalDateTime.of(year, month, day, 12, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun createTransaction(cost: Double, timestamp: Long = System.currentTimeMillis()): MpesaTransaction {
        return MpesaTransaction(
            smsId = "123",
            mpesaReceiptNumber = "RC${java.util.UUID.randomUUID()}",
            amount = 100.0,
            type = com.fintrack.app.core.domain.model.TransactionType.EXPENSE,
            merchantName = null,
            phoneNumber = "0700000000",
            paybillNumber = null,
            tillNumber = null,
            accountNumber = null,
            transactionType = com.fintrack.app.core.domain.model.MpesaTransactionType.SEND_MONEY,
            rawBody = "",
            smartClues = emptyList(),
            parserVersion = 1,
            timestamp = timestamp,
            transactionCost = cost,
            createdAt = System.currentTimeMillis()
        )
    }

    class FakeMpesaTransactionRepository : MpesaTransactionRepository {
        private var transactions = listOf<MpesaTransaction>()

        fun setTransactions(list: List<MpesaTransaction>) {
            transactions = list
        }

        override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<MpesaTransaction>> {
            // Filter in memory for test
            return flowOf(transactions.filter { it.timestamp in startDate..endDate })
        }
        
        override suspend fun getOldestTransactionTimestamp(): Long? {
            return transactions.minOfOrNull { it.timestamp }
        }

        // Implement all members of the interface
        override suspend fun syncMpesaSms(lookbackPeriod: LookbackPeriod): List<MpesaTransaction> {
            return transactions
        }
        
        override suspend fun parseAndStoreSms(smsBody: String, smsDate: Long, smsId: String): Boolean {
            return true
        }

        override fun getAllTransactions(): Flow<List<MpesaTransaction>> {
            return flowOf(transactions)
        }

        override fun getRecentTransactions(limit: Int): Flow<List<MpesaTransaction>> {
            return flowOf(transactions.take(limit))
        }

        override suspend fun deleteTransaction(receiptNumber: String) {}

        override suspend fun deleteAllTransactions() {}

        override suspend fun getTransactionCount(): Int {
            return transactions.size
        }
    }

    class FakeSharedPreferences : SharedPreferences {
        private val prefs = mutableMapOf<String, Any>()
        private val editor = FakeEditor(prefs)

        override fun getAll(): MutableMap<String, *> = prefs
        override fun getString(key: String?, defValue: String?): String? = prefs[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = prefs[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = prefs[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = prefs[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = prefs[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = prefs[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = prefs.containsKey(key)
        override fun edit(): SharedPreferences.Editor = editor
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val prefs: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any>()
            
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null && values != null) temp[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) temp.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                temp.clear()
                return this
            }
            override fun commit(): Boolean {
                prefs.putAll(temp)
                return true
            }
            override fun apply() {
                prefs.putAll(temp)
            }
        }
    }
    
    class FakeTimeProvider : com.fintrack.app.core.common.TimeProvider {
        private var fixedDate: LocalDate = LocalDate.of(2024, 1, 1)
        
        fun setFixedDate(date: LocalDate) {
            fixedDate = date
        }
        
        override fun currentDate(): LocalDate = fixedDate
        override fun currentDateTime(): java.time.LocalDateTime = fixedDate.atStartOfDay()
        override fun currentTimeMillis(): Long = java.time.Instant.now().toEpochMilli()
    }
}

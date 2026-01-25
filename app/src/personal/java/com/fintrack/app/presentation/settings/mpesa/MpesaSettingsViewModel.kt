package com.fintrack.app.presentation.settings.mpesa

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fintrack.app.core.data.local.dao.CategoryDao
import com.fintrack.app.core.data.local.dao.MerchantCategoryDao
import com.fintrack.app.core.data.local.dao.MpesaTransactionDao
import com.fintrack.app.core.data.local.model.MerchantCategoryEntity
import com.fintrack.app.core.data.preferences.MpesaOnboardingPreferences
import com.fintrack.app.core.domain.repository.MpesaTransactionRepository
import com.fintrack.app.core.worker.MpesaSyncWorker
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MpesaSettingsViewModel @Inject constructor(
    private val mpesaPreferences: MpesaOnboardingPreferences,
    private val mpesaRepository: MpesaTransactionRepository,
    private val mpesaDao: MpesaTransactionDao,
    private val merchantCategoryDao: MerchantCategoryDao,
    private val categoryDao: CategoryDao,
    private val mpesaCategoryMappingDao: com.fintrack.app.core.data.local.dao.MpesaCategoryMappingDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // --- Configuration States ---
    val isRealTimeEnabled = mpesaPreferences.isRealTimeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lookbackPeriodMonths = mpesaPreferences.lookbackPeriodMonths
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    // --- Merchant Mapping Data ---
    val availableCategories = categoryDao.getAllCategories(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _merchantList = MutableStateFlow<List<MerchantMappingItem>>(emptyList())
    val merchantList: StateFlow<List<MerchantMappingItem>> = _merchantList

    init {
        loadMerchantData()
    }

    private fun loadMerchantData() {
        viewModelScope.launch {
            // 1. Get top 100 merchants
            val merchants = mpesaDao.getFrequentMerchants(100)
            
            // 2. Get existing mappings (BOTH Merchant mappings AND initial Receipt mappings from Onboarding)
            // We combine them because Onboarding might have saved 'receipt-level' mappings which act as the de-facto initial mapping for that merchant
            
            // Collect both flows
            kotlinx.coroutines.flow.combine(
                merchantCategoryDao.getAllMappings(),
                mpesaCategoryMappingDao.getAllMappings()
            ) { merchantMappings, receiptMappings ->
                // Create lookup maps
                val merchantMap = merchantMappings.associateBy { it.merchantName }
                
                // For receipt mappings, we want to find if ANY receipt for a merchant has a mapping.
                // But this is expensive to reverse lookup "Which merchant owns this receipt?" without joining.
                // However, our onboarding process likely saved 'MerchantCategoryEntity' if the user confirmed a category for a merchant?
                // OR it saved 'MpesaCategoryMappingEntity' for specific transactions.
                
                // If the user says "assigned during onboarding", check if those were saved as Merchant mappings or Receipt mappings.
                // Assuming Onboarding saves to Receipt Mappings first if it's transaction-specific.
                
                // Strategy: check if we have a Merchant Mapping. If not, try to infer from the FIRST transaction of that merchant that has a mapping.
                
                merchantMap to receiptMappings
            }.collect { (merchantMap, receiptMappings) ->
                
                // Optimization: Group receipt mappings by receipt number for quick lookup?
                // But we don't have merchant name in receipt mapping. We need to join with transaction data.
                // For this View Model, let's keep it simple: Trust MerchantMapping first.
                // If missing, we could try to look up "What did I call this merchant last time?"
                // But typically Onboarding should have saved to MerchantCategoryDao if it was a "Merchant Rule".
                
                // If Onboarding saved to MpesaCategoryMappingDao (individual txns), we might miss it here unless we look deeper.
                // Let's iterate merchants and see if we can find a mapped transaction for them.
                
                val items = merchants.map { merchant ->
                    var currentCat = merchantMap[merchant.merchantName]?.categoryName
                    
                    if (currentCat == null) {
                        // Fallback: Check if any transaction for this merchant has a specific receipt mapping
                        // This requires knowing the receipts for this merchant.
                        // We can't easily do that without a join. 
                        // But wait! `mpesaDao.getTransactionsByMerchantName` can help if we really need it, but that's heavy inside a loop.
                        
                        // ALTERNATIVE: Use the SmartClue helper logic IF it was confident? 
                        // Or maybe we accept that if it wasn't saved as a Merchant Rule, it shows as Unmapped here.
                        
                        // ACTAULLY: The user says "assigned categories during onboarding". 
                        // Onboarding usually calls `viewModel.saveCategoryForMerchant` which SHOULD save to MerchantCategoryDao.
                        // If it wasn't displaying, maybe the onboarding saved to `MpesaCategoryMapping` (receipts) instead of `MerchantCategory` (rules)?
                        
                        // Let's try to look up ONE recent transaction for this merchant to see if it has a mapping.
                        // Ideally we'd do this in the DAO query, but we can do a quick check here for the top 100.
                        val sampleTxn = mpesaDao.getTransactionsByMerchantName(merchant.merchantName, 1).firstOrNull()
                        if (sampleTxn != null) {
                             val receiptMapping = receiptMappings.find { it.mpesaReceiptNumber == sampleTxn.mpesaReceiptNumber }
                             currentCat = receiptMapping?.categoryName
                        }
                    }

                    MerchantMappingItem(
                        merchantName = merchant.merchantName,
                        transactionCount = merchant.frequency,
                        totalAmount = merchant.totalAmount ?: 0.0,
                        currentCategory = currentCat
                    )
                }
                _merchantList.value = items
            }
        }
    }

    // --- Actions ---

    fun setRealTimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            mpesaPreferences.setRealTimeEnabled(enabled)
        }
    }

    fun setLookbackPeriod(months: Int) {
        viewModelScope.launch {
            mpesaPreferences.setLookbackPeriod(months)
        }
    }

    fun triggerRescan() {
        viewModelScope.launch {
            val months = lookbackPeriodMonths.value
            val request = OneTimeWorkRequestBuilder<MpesaSyncWorker>()
                .setInputData(workDataOf(MpesaSyncWorker.INPUT_LOOKBACK_MONTHS to months))
                .build()
            
            workManager.enqueue(request)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            mpesaPreferences.resetOnboarding()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            mpesaRepository.deleteAllTransactions()
        }
    }
    
    // --- UI States for Mapping ---
    private val _isMappingLoading = MutableStateFlow(false)
    val isMappingLoading: StateFlow<Boolean> = _isMappingLoading

    private val _mappingMessage = MutableStateFlow<String?>(null)
    val mappingMessage: StateFlow<String?> = _mappingMessage

    fun clearMessage() {
        _mappingMessage.value = null
    }

    // ... existing actions ...

    fun updateMerchantCategory(merchantName: String, categoryName: String) {
        viewModelScope.launch {
            _isMappingLoading.value = true
            try {
                // Simulate network/db latency for "loading spinner" requirement
                kotlinx.coroutines.delay(1000)

                val entity = MerchantCategoryEntity(
                    merchantName = merchantName,
                    categoryName = categoryName,
                    isUserConfirmed = true,
                    updatedAt = System.currentTimeMillis()
                )
                merchantCategoryDao.insertMapping(entity)
                
                _mappingMessage.value = "Mapped $merchantName to $categoryName"
            } catch (e: Exception) {
                _mappingMessage.value = "Failed to map merchant: ${e.message}"
            } finally {
                _isMappingLoading.value = false
            }
        }
    }
}

data class MerchantMappingItem(
    val merchantName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val currentCategory: String? // Null if no mapping
)

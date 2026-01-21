package com.example.fintrack.presentation.settings.mpesa

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.fintrack.core.data.local.dao.CategoryDao
import com.example.fintrack.core.data.local.dao.MerchantCategoryDao
import com.example.fintrack.core.data.local.dao.MpesaTransactionDao
import com.example.fintrack.core.data.local.model.MerchantCategoryEntity
import com.example.fintrack.core.data.preferences.MpesaOnboardingPreferences
import com.example.fintrack.core.domain.repository.MpesaTransactionRepository
import com.example.fintrack.core.worker.MpesaSyncWorker
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
            
            // 2. Get existing mappings
            merchantCategoryDao.getAllMappings().collect { mappings ->
                val mappingMap = mappings.associateBy { it.merchantName }
                
                val items = merchants.map { merchant ->
                    MerchantMappingItem(
                        merchantName = merchant.merchantName,
                        transactionCount = merchant.frequency,
                        totalAmount = merchant.totalAmount ?: 0.0,
                        currentCategory = mappingMap[merchant.merchantName]?.categoryName
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
    
    fun updateMerchantCategory(merchantName: String, categoryName: String) {
        viewModelScope.launch {
            val entity = MerchantCategoryEntity(
                merchantName = merchantName,
                categoryName = categoryName,
                isUserConfirmed = true,
                updatedAt = System.currentTimeMillis()
            )
            merchantCategoryDao.insertMapping(entity)
        }
    }
}

data class MerchantMappingItem(
    val merchantName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val currentCategory: String? // Null if no mapping
)

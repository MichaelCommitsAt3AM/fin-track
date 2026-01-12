package com.example.fintrack.core.domain.repository

import com.example.fintrack.core.domain.model.Saving
import com.example.fintrack.core.domain.model.Contribution
import kotlinx.coroutines.flow.Flow

interface SavingRepository {
    
    suspend fun insertSaving(saving: Saving)
    
    suspend fun updateSaving(saving: Saving)
    
    fun getSaving(savingId: String): Flow<Saving?>
    
    fun getAllSavings(userId: String): Flow<List<Saving>>
    
    suspend fun deleteSaving(savingId: String)
    
    suspend fun addContribution(contribution: Contribution, currentSavingAmount: Double)
    
    fun getContributionsForSaving(savingId: String): Flow<List<Contribution>>
    
    suspend fun syncSavingsFromCloud()
}

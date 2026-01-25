package com.fintrack.app.core.domain.repository

import com.fintrack.app.core.domain.model.Saving
import com.fintrack.app.core.domain.model.Contribution
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
    
    suspend fun syncUnsyncedSavings()
}

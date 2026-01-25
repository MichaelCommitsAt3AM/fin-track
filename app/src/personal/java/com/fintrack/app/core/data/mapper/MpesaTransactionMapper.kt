package com.fintrack.app.core.data.mapper

import com.fintrack.app.core.data.local.model.MpesaTransactionEntity
import com.fintrack.app.core.domain.model.MpesaTransaction
import com.fintrack.app.core.domain.model.MpesaTransactionType
import com.fintrack.app.core.domain.model.TransactionType

/**
 * Mapper for converting between MpesaTransactionEntity and MpesaTransaction domain model.
 */
object MpesaTransactionMapper {
    
    private const val CLUE_SEPARATOR = "||"
    
    fun toEntity(domain: MpesaTransaction): MpesaTransactionEntity {
        return MpesaTransactionEntity(
            mpesaReceiptNumber = domain.mpesaReceiptNumber,
            smsId = domain.smsId,
            amount = domain.amount,
            type = domain.type.name,
            merchantName = domain.merchantName,
            phoneNumber = domain.phoneNumber,
            paybillNumber = domain.paybillNumber,
            tillNumber = domain.tillNumber,
            accountNumber = domain.accountNumber,
            transactionType = domain.transactionType.name,
            rawBody = domain.rawBody,
            smartClues = domain.smartClues.joinToString(CLUE_SEPARATOR),
            parserVersion = domain.parserVersion,
            timestamp = domain.timestamp,
            createdAt = domain.createdAt
        )
    }
    
    fun toDomain(entity: MpesaTransactionEntity): MpesaTransaction {
        val smartCluesList = entity.smartClues
            ?.split(CLUE_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        
        return MpesaTransaction(
            smsId = entity.smsId,
            mpesaReceiptNumber = entity.mpesaReceiptNumber,
            amount = entity.amount,
            type = TransactionType.valueOf(entity.type),
            merchantName = entity.merchantName,
            phoneNumber = entity.phoneNumber,
            paybillNumber = entity.paybillNumber,
            tillNumber = entity.tillNumber,
            accountNumber = entity.accountNumber,
            transactionType = MpesaTransactionType.valueOf(entity.transactionType),
            rawBody = entity.rawBody,
            smartClues = smartCluesList,
            parserVersion = entity.parserVersion,
            timestamp = entity.timestamp,
            createdAt = entity.createdAt
        )
    }
    
    fun toDomainList(entities: List<MpesaTransactionEntity>): List<MpesaTransaction> {
        return entities.map { toDomain(it) }
    }
}

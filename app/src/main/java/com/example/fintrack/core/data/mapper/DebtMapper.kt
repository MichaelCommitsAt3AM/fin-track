package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.data.local.model.DebtEntity
import com.example.fintrack.core.data.local.model.PaymentEntity
import com.example.fintrack.core.domain.model.Debt
import com.example.fintrack.core.domain.model.DebtType
import com.example.fintrack.core.domain.model.Payment

// Entity to Domain
fun DebtEntity.toDomain(): Debt {
    return Debt(
        id = id,
        userId = userId,
        title = title,
        originalAmount = originalAmount,
        currentBalance = currentBalance,
        minimumPayment = minimumPayment,
        dueDate = dueDate,
        interestRate = interestRate,
        notes = notes,
        iconName = iconName,
        debtType = DebtType.valueOf(debtType),
        createdAt = createdAt
    )
}

// Domain to Entity
fun Debt.toEntity(): DebtEntity {
    return DebtEntity(
        id = id,
        userId = userId,
        title = title,
        originalAmount = originalAmount,
        currentBalance = currentBalance,
        minimumPayment = minimumPayment,
        dueDate = dueDate,
        interestRate = interestRate,
        notes = notes,
        iconName = iconName,
        debtType = debtType.name,
        createdAt = createdAt
    )
}

// Payment Entity to Domain
fun PaymentEntity.toDomain(): Payment {
    return Payment(
        id = id,
        debtId = debtId,
        amount = amount,
        date = date,
        note = note
    )
}

// Payment Domain to Entity
fun Payment.toEntity(): PaymentEntity {
    return PaymentEntity(
        id = id,
        debtId = debtId,
        amount = amount,
        date = date,
        note = note
    )
}

package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.data.local.model.SavingEntity
import com.example.fintrack.core.data.local.model.ContributionEntity
import com.example.fintrack.core.domain.model.Saving
import com.example.fintrack.core.domain.model.Contribution

// Entity to Domain
fun SavingEntity.toDomain(): Saving {
    return Saving(
        id = id,
        userId = userId,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        notes = notes,
        iconName = iconName,
        createdAt = createdAt
    )
}

// Domain to Entity
fun Saving.toEntity(): SavingEntity {
    return SavingEntity(
        id = id,
        userId = userId,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        notes = notes,
        iconName = iconName,
        createdAt = createdAt
    )
}

// Contribution Entity to Domain
fun ContributionEntity.toDomain(): Contribution {
    return Contribution(
        id = id,
        savingId = savingId,
        amount = amount,
        date = date,
        note = note
    )
}

// Contribution Domain to Entity
fun Contribution.toEntity(): ContributionEntity {
    return ContributionEntity(
        id = id,
        savingId = savingId,
        amount = amount,
        date = date,
        note = note
    )
}

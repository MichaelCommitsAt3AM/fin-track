package com.example.fintrack.core.data.mapper

import com.example.fintrack.core.data.local.model.UserEntity
import com.example.fintrack.core.domain.model.User

// Entity to Domain
fun UserEntity.toDomain(): User {
    return User(
        userId = this.userId,
        fullName = this.fullName,
        email = this.email,
        phoneNumber = this.phoneNumber,
        avatarId = this.avatarId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

// Domain to Entity
fun User.toEntity(): UserEntity {
    return UserEntity(
        userId = this.userId,
        fullName = this.fullName,
        email = this.email,
        phoneNumber = this.phoneNumber,
        avatarId = this.avatarId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

package com.fintrack.app.presentation.utils

import com.fintrack.app.R

/**
 * Shared function to map the database ID to the drawable resource.
 * This ensures consistency across Home, Settings, and Profile screens.
 */
fun getAvatarResource(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_default
        2 -> R.drawable.avatar_female
        3 -> R.drawable.avatar_male
        4 -> R.drawable.avatar_grandpa
        5 -> R.drawable.avatar_girl
        else -> R.drawable.avatar_default
    }
}
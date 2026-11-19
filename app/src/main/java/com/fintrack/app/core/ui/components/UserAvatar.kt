package com.fintrack.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.fintrack.app.presentation.utils.getAvatarResource

/** `avatarId` value meaning "no profile picture" (e.g. local-only users who skipped profile setup). */
const val NO_AVATAR = 0

/**
 * The user's avatar, or a neutral person icon when they have none.
 * Pass size, clip, border and click handling through [modifier], as with an Image.
 */
@Composable
fun UserAvatar(
    avatarId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Profile picture"
) {
    if (avatarId == NO_AVATAR) {
        BoxWithConstraints(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(minOf(maxWidth, maxHeight) * 0.6f)
            )
        }
    } else {
        Image(
            painter = painterResource(id = getAvatarResource(avatarId)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

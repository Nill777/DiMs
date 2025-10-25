package com.distributedMessenger.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.distributedMessenger.core.User

@Composable
fun BlockItem(user: User,
              onBlock: () -> Unit,
              onUnblock: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(user.username)

        IconButton(onClick = {
            if (user.blockedUsersId != null) {
                onUnblock() // Если пользователь заблокирован, разблокируем
            } else {
                onBlock() // Если пользователь не заблокирован, блокируем
            }
        })
        {
            Icon(
                imageVector = if (user.blockedUsersId != null) Icons.Filled.Lock else Icons.Filled.Done,
                contentDescription = if (user.blockedUsersId != null) "Unblock" else "Block",
                tint = if (user.blockedUsersId != null) MaterialTheme.colorScheme.error else LocalContentColor.current
            )
        }
    }
}

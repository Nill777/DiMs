package com.distributedMessenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.distributedMessenger.core.Message
import com.distributedMessenger.ui.R

@Composable
fun ChatListItem(
    chatName: String,
    lastMessage: Message?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(bottom = 1.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка (иконка заглушка)
        Icon(
            painter = painterResource(R.drawable.email_1),
            contentDescription = "Avatar",
            modifier = Modifier
//                .background(Color.Blue)
                .size(58.dp)
                .padding(start = 4.dp, end = 4.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chatName,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = lastMessage?.content?.truncateForPreview() ?: "",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }

        Column(
            modifier = Modifier
//                .weight(1f)
                .wrapContentWidth()
                .padding(1.dp),
        ) {
            Text(
                text = lastMessage?.formatTimestamp() ?: "∞",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun String.truncateForPreview(): String {
    val maxChars = 30
    val firstNewLine = indexOf('\n')

    return when {
        // Если есть перенос строки в пределах 30 символов
        firstNewLine in 1..<maxChars -> substring(0, firstNewLine) + "..."
        // Если строка длиннее 30 символов без переносов
        length > maxChars -> take(maxChars) + "..."
        // Если строка короче 30 символов и без переносов
        else -> this
    }
}

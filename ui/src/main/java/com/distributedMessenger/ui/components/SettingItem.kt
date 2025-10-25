package com.distributedMessenger.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.distributedMessenger.core.AppSettingType

@Composable
fun SettingItem(type: AppSettingType,
                currentValue: Int,
                onValueChange: (Int) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Название настройки
        Text(
            text = type.settingName,
            modifier = Modifier.weight(1f)
        )
        // Кнопка с текущим значением
        Button(onClick = { showMenu = true }) {
            Text(type.possibleValues[currentValue] ?: "")
        }

        // Выпадающее меню
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            type.possibleValues.forEach { (key, value) ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onValueChange(key)
                        showMenu = false
                    }
                )
            }
        }
    }
}

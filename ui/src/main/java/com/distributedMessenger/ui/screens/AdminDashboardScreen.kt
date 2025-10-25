package com.distributedMessenger.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distributedMessenger.logger.Logger
import com.distributedMessenger.presenter.viewmodels.AdminViewModel
import com.distributedMessenger.ui.NavigationController

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel,
                         navigationController: NavigationController) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                Logger.log("AdminDashboard", "Navigation: User Management")
                navigationController.navigateToUserManagement()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text(
                text = "Управление ролями",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Button(
            onClick = {
                Logger.log("AdminDashboard", "Navigation: Block Management")
                navigationController.navigateToBlockManagement()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text(
                text = "Блокировка пользователей",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Button(
            onClick = {
                Logger.log("AdminDashboard", "Navigation: App Settings")
                navigationController.navigateToAppSettings()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text(
                text = "Настройки приложения",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

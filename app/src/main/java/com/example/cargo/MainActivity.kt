package com.example.cargo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.cargo.ui.screens.AddShipmentScreen
import com.example.cargo.ui.screens.DashboardScreen
import com.example.cargo.ui.screens.ShipmentDetailsScreen
import com.example.cargo.ui.theme.CargoTheme
import com.example.cargo.viewmodel.ShipmentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ShipmentViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShipmentViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CargoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNav(viewModel)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun AppNav(viewModel: ShipmentViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onAdd = { navController.navigate("add") },
                onShipmentClick = { id -> navController.navigate("details/$id") }
            )
        }
        composable("add") {
            AddShipmentScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "details/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            ShipmentDetailsScreen(
                viewModel = viewModel,
                shipmentId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

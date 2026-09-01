package com.example.cargo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cargo.ui.theme.AuroraBackground
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cargo.ui.screens.AddShipmentScreen
import com.example.cargo.ui.screens.ArchiveScreen
import com.example.cargo.ui.screens.ContactsScreen
import com.example.cargo.ui.screens.DashboardScreen
import com.example.cargo.ui.screens.SettingsScreen
import com.example.cargo.ui.screens.ShipmentDetailsScreen
import com.example.cargo.ui.theme.CargoTheme
import com.example.cargo.viewmodel.ShipmentViewModel

data class TabItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

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
            val darkMode by viewModel.settings.darkMode.collectAsState(initial = true)
            CargoTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun MainApp(viewModel: ShipmentViewModel) {
    val navController = rememberNavController()
    val tabs = listOf(
        TabItem("dashboard", "داشبورد", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        TabItem("archive", "بایگانی", Icons.Filled.Archive, Icons.Outlined.Archive),
        TabItem("settings", "تنظیمات", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in tabs.map { it.route }

    // Contact picking: which phone field opened the contacts book
    var pickTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "dashboard") {
                FloatingActionButton(
                    onClick = { navController.navigate("add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, "ثبت بار جدید")
                }
            }
        }
    ) { padding ->
        AuroraBackground {
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
            enterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(260)) { it / 24 }
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) +
                    androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { it / 24 }
            }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onAdd = { navController.navigate("add") },
                    onShipmentClick = { id -> navController.navigate("details/$id") }
                )
            }
            composable("archive") {
                ArchiveScreen(
                    viewModel = viewModel,
                    onShipmentClick = { id -> navController.navigate("details/$id") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenContacts = { navController.navigate("contacts?pick=false") }
                )
            }
            composable("add") {
                AddShipmentScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenContacts = { which ->
                        pickTarget = which
                        navController.navigate("contacts?pick=true")
                    }
                )
            }
            composable(
                "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: 0
                val current by viewModel.filteredShipments.collectAsState()
                val initial = current.firstOrNull { it.id == id }
                AddShipmentScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    initialShipment = initial,
                    onOpenContacts = { which ->
                        pickTarget = which
                        navController.navigate("contacts?pick=true")
                    }
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
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("edit/$id") }
                )
            }
            composable(
                "contacts?pick={pick}",
                arguments = listOf(navArgument("pick") { type = NavType.StringType; defaultValue = "false" })
            ) { backStackEntry ->
                val pick = backStackEntry.arguments?.getString("pick") == "true"
                ContactsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    pickMode = pick,
                    onPick = { name, phone ->
                        when (pickTarget) {
                            "sender" -> viewModel.pendingSenderName = name
                            "receiver" -> viewModel.pendingReceiverName = name
                        }
                        when (pickTarget) {
                            "sender" -> viewModel.pendingSenderPhone = phone
                            "receiver" -> viewModel.pendingReceiverPhone = phone
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
        }
    }
}
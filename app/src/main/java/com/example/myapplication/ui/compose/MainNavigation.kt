package com.example.myapplication.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.compose.calendar.CalendarScreen
import com.example.myapplication.ui.compose.input.InputScreen
import com.example.myapplication.ui.compose.report.ReportScreen
import com.example.myapplication.ui.compose.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Input : Screen("input", "入力", Icons.Filled.AddCircle)
    object Calendar : Screen("calendar", "カレンダー", Icons.Filled.DateRange)
    object Report : Screen("report", "レポート", Icons.Filled.List)
    object Settings : Screen("settings", "その他", Icons.Filled.Settings)
}

val bottomNavigationItems = listOf(
    Screen.Input,
    Screen.Calendar,
    Screen.Report,
    Screen.Settings
)

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            FluidBottomBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Input.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Input.route) { InputScreen(navController) }
                composable(Screen.Calendar.route) { CalendarScreen() }
                composable(Screen.Report.route) { ReportScreen(navController) }
                composable(Screen.Settings.route) { SettingsScreen(navController) }
                
                // Sub-settings stubs
                composable("category_edit") { com.example.myapplication.ui.compose.settings.CategoryEditScreen() }
                composable("fixed_cost") { com.example.myapplication.ui.compose.settings.FixedCostScreen() }
                composable("quota") { com.example.myapplication.ui.compose.settings.QuotaScreen() }
                
                composable(
                    route = "category_report/{categoryId}/{categoryName}/{currentMonth}",
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("categoryName") { type = NavType.StringType },
                        navArgument("currentMonth") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    com.example.myapplication.ui.compose.report.CategoryReportScreen(
                        navController = navController,
                        categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0,
                        categoryName = backStackEntry.arguments?.getString("categoryName") ?: "",
                        currentMonthStr = backStackEntry.arguments?.getString("currentMonth") ?: ""
                    )
                }
            }
        }
    }
}

@Composable
fun FluidBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Apple-style translucent bottom bar
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.7f)) // Translucent background
            // Note: RenderEffect for blur requires a specialized modifier if we want to blur what's behind it.
            // Since Compose 1.4+, we can use modifier.blur but it blurs the component itself.
            // A true backdrop filter requires drawing the background with RenderEffect.
            // For simplicity in Phase 1, we use a translucent background.
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            bottomNavigationItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                    label = { Text(screen.title) },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

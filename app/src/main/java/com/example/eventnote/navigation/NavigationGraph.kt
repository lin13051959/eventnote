package com.example.eventnote.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eventnote.screens.CreateEditScreen
import com.example.eventnote.screens.EventDetailScreen
import com.example.eventnote.screens.ListScreen
import com.example.eventnote.screens.SettingsScreen
import com.example.eventnote.ui.viewmodel.EventNoteViewModel

sealed class Screen(val route: String) {
    object List : Screen("list")
    object Detail : Screen("detail/{eventId}") {
        fun createRoute(eventId: Long) = "detail/$eventId"
    }
    object Create : Screen("create")
    object Edit : Screen("edit/{eventId}") {
        fun createRoute(eventId: Long) = "edit/$eventId"
    }
    object Settings : Screen("settings")
}

@Composable
fun NavigationGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: EventNoteViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.List.route
    ) {
        composable(Screen.List.route) {
            ListScreen(
                viewModel = viewModel,
                onEventClick = { eventId ->
                    navController.navigate(Screen.Detail.createRoute(eventId))
                },
                onCreateEvent = {
                    navController.navigate(Screen.Create.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Detail.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull()
            EventDetailScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                onEdit = { eventId?.let { 
                    navController.navigate(Screen.Edit.createRoute(it))
                } }
            )
        }
        
        composable(Screen.Create.route) {
            CreateEditScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                onSave = { navController.navigateUp() }
            )
        }
        
        composable(Screen.Edit.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.toLongOrNull()
            CreateEditScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                onSave = { navController.navigateUp() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.navigateUp() }
            )
        }
    }
}

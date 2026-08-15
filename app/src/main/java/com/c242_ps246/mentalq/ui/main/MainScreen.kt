package com.c242_ps246.mentalq.ui.main

import android.app.Application
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.c242_ps246.mentalq.ui.main.chat.ChatRoomScreen
import com.c242_ps246.mentalq.ui.main.chat.ChatScreen
import com.c242_ps246.mentalq.ui.main.dashboard.DashboardScreen
import com.c242_ps246.mentalq.ui.main.note.NoteScreen
import com.c242_ps246.mentalq.ui.main.note.detail.DetailNoteScreen
import com.c242_ps246.mentalq.ui.main.profile.ProfileScreen
import com.c242_ps246.mentalq.ui.main.psychologist.PsychologistScreen
import com.c242_ps246.mentalq.ui.main.psychologist.midtrans.MidtransScreen
import com.c242_ps246.mentalq.ui.main.psychologist.midtrans.MidtransWebView
import com.c242_ps246.mentalq.ui.navigation.Routes

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    userRole: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = when (currentRoute) {
        Routes.DASHBOARD, Routes.NOTE, Routes.CHAT, Routes.PROFILE -> true
        else -> false
    }

    var selectedItem by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentRoute) {
        selectedItem = when (currentRoute) {
            Routes.DASHBOARD -> 0
            Routes.NOTE -> 1
            Routes.CHAT -> 2
            Routes.PROFILE -> 3
            else -> selectedItem
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            enterTransition = {
                EnterTransition.None
            },
            exitTransition = {
                ExitTransition.None
            }
        ) {
            composable(
                route = Routes.DASHBOARD
            ) {
                DashboardScreen(
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate("${Routes.NOTE_DETAIL}/$noteId")
                    },
                    onNavigateToPsychologistList = {
                        navController.navigate(Routes.PSYCHOLOGIST_LIST)
                    }
                )
            }

            composable(
                route = Routes.PSYCHOLOGIST_LIST
            ) {
                PsychologistScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNavigateToMidtransWebView = { userId, itemId ->
                        navController.navigate("${Routes.MIDTRANS_WEBVIEW}/$userId/$itemId")
                    }
                )
            }

            composable(
                route = "${Routes.MIDTRANS_MAIN_SCREEN}/{orderId}/{userId}/{itemId}",
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                MidtransScreen(
                    orderId = orderId,
                    onSuccess = { chatId ->
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                        }
                        navController.navigate("${Routes.CHAT_ROOM}/$chatId")
                    },
                    onFailed = {
                        navController.navigate(Routes.PSYCHOLOGIST_LIST) {
                            popUpTo(backStackEntry.destination.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackClick = {
                        navController.navigate(Routes.PSYCHOLOGIST_LIST) {
                            popUpTo(backStackEntry.destination.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = "${Routes.MIDTRANS_WEBVIEW}/{userId}/{itemId}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
                MidtransWebView(
                    itemId = itemId,
                    onBackClick = { orderId ->
                        if (orderId == null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("${Routes.MIDTRANS_MAIN_SCREEN}/$orderId/$userId/$itemId") {
                                popUpTo(backStackEntry.destination.id) { inclusive = true }
                            }
                        }
                    }
                )
            }


            composable(
                route = Routes.NOTE
            ) {
                NoteScreen(
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate("${Routes.NOTE_DETAIL}/$noteId")
                    },
                )
            }
            composable(
                route = "${Routes.NOTE_DETAIL}/{noteId}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: return@composable
                DetailNoteScreen(
                    noteId = noteId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    application = LocalContext.current.applicationContext as Application
                )
            }


            composable(
                route = Routes.CHAT
            ) {
                ChatScreen(
                    onNavigateToChatRoom = { chatId ->
                        navController.navigate("${Routes.CHAT_ROOM}/$chatId")
                    },
                    onBackClick = {
                        navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                    }
                )
            }
            composable(
                route = "${Routes.CHAT_ROOM}/{chatId}",
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatRoomScreen(
                    chatRoomId = chatId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Routes.PROFILE
            ) {
                ProfileScreen(
                    onLogout = onLogout
                )
            }
        }

        if (shouldShowBottomBar) {
            CustomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = selectedItem,
                onItemSelected = { index, route ->
                    selectedItem = index
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                userRole = userRole
            )
        }
    }
}

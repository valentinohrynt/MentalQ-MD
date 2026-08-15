package com.c242_ps246.mentalq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.c242_ps246.mentalq.ui.animation.PageAnimation.slideInFromBottom
import com.c242_ps246.mentalq.ui.animation.PageAnimation.slideOutToBottom
import com.c242_ps246.mentalq.ui.auth.AuthScreen
import com.c242_ps246.mentalq.ui.main.MainScreen
import com.c242_ps246.mentalq.ui.main.PsychologistMainScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    roleFromSplash: String? = null
) {
    val navController = rememberNavController()
    var sessionRole by rememberSaveable { mutableStateOf(roleFromSplash) }

    NavHost(
        navController = navController,
        startDestination = if (sessionRole == null) {
            Routes.AUTH
        } else {
            when (sessionRole) {
                "user" -> {
                    Routes.MAIN_SCREEN
                }

                "psychologist" -> {
                    Routes.PSYCHOLOGIST_MAIN_SCREEN
                }

                else -> {
                    Routes.AUTH
                }
            }
        },
        modifier = modifier
    ) {
        composable(
            Routes.AUTH,
            enterTransition = { slideInFromBottom },
            exitTransition = { slideOutToBottom }
        ) {
            AuthScreen(
                onSuccess = { authenticatedRole ->
                    sessionRole = authenticatedRole
                    when (authenticatedRole) {
                        "user" -> navController.navigate(Routes.MAIN_SCREEN) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }

                        "psychologist" -> navController.navigate(Routes.PSYCHOLOGIST_MAIN_SCREEN) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.MAIN_SCREEN) {
            MainScreen(
                onLogout = {
                    sessionRole = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                userRole = sessionRole ?: "user"
            )
        }
        composable(Routes.PSYCHOLOGIST_MAIN_SCREEN) {
            PsychologistMainScreen(
                onLogout = {
                    sessionRole = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                userRole = sessionRole ?: "psychologist"
            )
        }
    }
}

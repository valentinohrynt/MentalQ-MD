package com.c242_ps246.mentalq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
    tokenFromSplash: String? = null,
    roleFromSplash: String? = null
) {
    val navController = rememberNavController()
    var hasLoggedOut by rememberSaveable { mutableStateOf(false) }
    rememberCoroutineScope()

    var splashToken by rememberSaveable { mutableStateOf(tokenFromSplash) }
    var splashRole by rememberSaveable { mutableStateOf(roleFromSplash) }

    NavHost(
        navController = navController,
        startDestination = if (splashRole == null) {
            Routes.AUTH
        } else {
            when (splashRole) {
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
                tokenFromSplash = if (hasLoggedOut) null else tokenFromSplash,
                roleFromSplash = if (hasLoggedOut) null else roleFromSplash,
                onSuccess = { authenticatedRole ->
                    hasLoggedOut = false
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
                    hasLoggedOut = true
                    splashToken = null
                    splashRole = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userRole = splashRole ?: "user"
            )
        }
        composable(Routes.PSYCHOLOGIST_MAIN_SCREEN) {
            PsychologistMainScreen(
                onLogout = {
                    hasLoggedOut = true
                    splashToken = null
                    splashRole = null
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userRole = splashRole ?: "psychologist"
            )
        }
    }
}

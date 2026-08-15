package com.c242_ps246.mentalq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.c242_ps246.mentalq.ui.navigation.AppNavigation
import com.c242_ps246.mentalq.ui.onboarding.OnboardingScreen
import com.c242_ps246.mentalq.ui.onboarding.OnboardingViewModel
import com.c242_ps246.mentalq.ui.splash.SplashScreen
import com.c242_ps246.mentalq.ui.theme.MentalQTheme
import com.c242_ps246.mentalq.ui.utils.NetworkAwareContent
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplashScreen by remember { mutableStateOf(true) }
            val shouldShowOnboarding by onboardingViewModel.shouldShowOnboarding.collectAsStateWithLifecycle()
            var userRole by remember { mutableStateOf<String?>(null) }

            if (showSplashScreen) {
                MentalQTheme {
                    SplashScreen { _, role ->
                        showSplashScreen = false
                        userRole = role
                    }
                }
            } else {
                AppContent(
                    userRole,
                    shouldShowOnboarding,
                    onboardingViewModel
                )
            }
        }
    }
}

@Composable
fun AppContent(
    userRole: String?,
    shouldShowOnboarding: Boolean,
    viewModel: OnboardingViewModel
) {

    NetworkAwareContent {
        if (shouldShowOnboarding) {
            MentalQTheme {
                OnboardingScreen(
                    onFinished = {
                        viewModel.onOnboardingCompleted()
                    }
                )
            }
        } else {
            MentalQTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        roleFromSplash = userRole,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

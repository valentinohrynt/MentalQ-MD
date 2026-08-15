package com.c242_ps246.mentalq.ui.main

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.ui.navigation.Routes
import com.c242_ps246.mentalq.ui.theme.MentalQTheme

@Composable
fun CustomNavigationBar(
    modifier: Modifier = Modifier,
    selectedItem: Int,
    onItemSelected: (Int, String) -> Unit,
    userRole: String
) {
    val items = remember(userRole) { if (userRole == "user") {
        listOf(
            BottomNavItem("Dashboard", R.drawable.ic_home, Routes.DASHBOARD),
            BottomNavItem("Note", R.drawable.ic_note, Routes.NOTE),
            BottomNavItem("Chat", R.drawable.ic_chat, Routes.CHAT),
            BottomNavItem("Profile", R.drawable.ic_profile, Routes.PROFILE)
        )
    } else {
        listOf(
            BottomNavItem("Chat", R.drawable.ic_chat, Routes.CHAT),
            BottomNavItem("Profile", R.drawable.ic_profile, Routes.PROFILE)
        )
    } }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = if (userRole == "user")
            modifier
                .width(320.dp)
                .height(90.dp)
                .padding(16.dp)
        else
            modifier
                .width(160.dp)
                .height(90.dp)
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        NavigationBar(
            modifier = modifier
                .matchParentSize()
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(50.dp),
                    clip = false
                )
                .background(colorScheme.background, shape = RoundedCornerShape(50.dp))
                .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(50.dp))
                .align(Alignment.Center),
            containerColor = Color.Transparent,
            windowInsets = WindowInsets(0, 0, 0, 0),
            contentColor = colorScheme.onPrimary,
            tonalElevation = 8.dp,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedItem == index

                val transition = updateTransition(
                    targetState = isSelected,
                    label = "Selected Transition"
                )

                val animatedPadding by transition.animateDp(transitionSpec = {
                    if (targetState) {
                        tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                    } else {
                        tween(durationMillis = 500, easing = FastOutLinearInEasing)
                    }
                }, label = "Padding Animation") { state ->
                    if (state) 10.dp else 0.dp
                }

                NavigationBarItem(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.CenterVertically),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.secondary,
                        unselectedIconColor = colorScheme.tertiary,
                        selectedTextColor = colorScheme.onSecondary,
                        unselectedTextColor = colorScheme.onTertiary,
                        indicatorColor = Color.Transparent,
                    ),
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selectedItem == index) colorScheme.secondary else Color.Transparent
                                )
                                .padding(animatedPadding)
                        ) {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.label,
                                tint = if (selectedItem == index) colorScheme.onSecondary else colorScheme.tertiary
                            )
                        }
                    },
                    selected = isSelected,
                    onClick = {
                        onItemSelected(index, item.route)
                    },
                    alwaysShowLabel = false
                )
            }
        }
    }
}


data class BottomNavItem(val label: String, val icon: Int, val route: String)


@Preview
@Composable
fun DashboardScreenPreview() {
    MentalQTheme {
        CustomNavigationBar(
            selectedItem = 0,
            onItemSelected = { _, _ -> },
            userRole = "psychologist"
        )
    }
}

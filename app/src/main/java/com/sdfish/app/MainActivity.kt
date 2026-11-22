package com.sdfish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sdfish.app.ui.EditCatchScreen
import com.sdfish.app.ui.HomeScreen
import com.sdfish.app.ui.theme.SdFishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SdFishTheme {
                SdFishNavHost()
            }
        }
    }
}

@Composable
private fun SdFishNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAdd = { nav.navigate("edit/0") },
                onOpen = { id -> nav.navigate("edit/$id") },
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            EditCatchScreen(
                catchId = entry.arguments?.getLong("id") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }
    }
}

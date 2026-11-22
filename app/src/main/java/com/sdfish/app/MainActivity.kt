package com.sdfish.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.sdfish.app.ui.TripScreen
import com.sdfish.app.ui.theme.SdFishTheme

// AppCompatActivity applies the language picked in the app settings.
class MainActivity : AppCompatActivity() {
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
                onOpen = { id -> nav.navigate("edit/$id") },
                onStartTrip = { nav.navigate("trip/0") },
                onStartTripForFish = { nav.navigate("trip/0?fish=true") },
                onAddPastTrip = { nav.navigate("trip/0?past=true") },
                onOpenTrip = { id -> nav.navigate("trip/$id") },
                onAddFishToTrip = { tripId -> nav.navigate("edit/0?trip=$tripId") },
            )
        }
        composable(
            route = "edit/{id}?trip={trip}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("trip") { type = NavType.LongType; defaultValue = 0L },
            ),
        ) { entry ->
            EditCatchScreen(
                catchId = entry.arguments?.getLong("id") ?: 0L,
                newInTripId = entry.arguments?.getLong("trip") ?: 0L,
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            route = "trip/{id}?fish={fish}&past={past}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("fish") { type = NavType.BoolType; defaultValue = false },
                navArgument("past") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val addFishAfterStart = entry.arguments?.getBoolean("fish") ?: false
            TripScreen(
                tripId = entry.arguments?.getLong("id") ?: 0L,
                isPast = entry.arguments?.getBoolean("past") ?: false,
                onDone = { nav.popBackStack() },
                onStarted = { tripId ->
                    nav.popBackStack()
                    // Started from "add a fish": go straight on to that fish.
                    if (addFishAfterStart) nav.navigate("edit/0?trip=$tripId")
                },
                onAddFish = { tripId -> nav.navigate("edit/0?trip=$tripId") },
                onOpenCatch = { id -> nav.navigate("edit/$id") },
            )
        }
    }
}

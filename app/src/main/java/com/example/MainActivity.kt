package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(navController: androidx.navigation.NavController) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    
    LaunchedEffect(Unit) {
        delay(500)
        // Zoom in to the center of the icon
        scale.animateTo(
            targetValue = 150f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        )
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text = "♞",
            fontSize = 48.sp,
            modifier = Modifier.scale(scale.value).alpha(alpha.value),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(navController)
                        }
                        composable("home") { HomeScreen(navController) }
                        composable("game/{mode}") { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "pvc"
                            ChessScreen(navController, mode)
                        }
                        composable("settings") { SettingsScreen(navController) }
                    }
                }
            }
        }
    }
}

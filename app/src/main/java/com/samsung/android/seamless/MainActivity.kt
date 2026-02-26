package com.samsung.android.seamless

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.samsung.android.seamless.service.SpeechRecognitionService
import com.samsung.android.seamless.ui.theme.SeamlessTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_AUDIO_PERMISSION = "request_audio_permission"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fromWidget = intent.getBooleanExtra(EXTRA_REQUEST_AUDIO_PERMISSION, false)

        enableEdgeToEdge()
        setContent {
            SeamlessTheme {
                val permissionsToRequest = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()

                var hasAudioPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
                    hasAudioPermission = audioGranted
                    if (audioGranted && fromWidget) {
                        SpeechRecognitionService.startRecognition(this)
                        finish()
                    }
                }

                // If launched from widget and permission not yet granted, show dialog immediately
                LaunchedEffect(fromWidget) {
                    if (fromWidget && !hasAudioPermission) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else if (fromWidget && hasAudioPermission) {
                        SpeechRecognitionService.startRecognition(this@MainActivity)
                        finish()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Lottie animation
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.Asset("background.lottie")
                    )
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(-1f)
                    )

                    // Foreground UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasAudioPermission) {
                            Text(
                                text = "Ready",
                                color = Color(0xFF00D4AA),
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Use the widget on your home screen to start listening.",
                                color = Color(0xFF8899AA),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "Microphone permission required",
                                color = Color(0xFFFFFFFF),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

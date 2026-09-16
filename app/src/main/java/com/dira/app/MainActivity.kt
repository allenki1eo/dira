package com.dira.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dira.app.capture.ScreenCaptureService
import com.dira.app.session.GuideSessionViewModel
import com.dira.app.ui.DiraApp
import com.dira.app.ui.theme.DiraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: GuideSessionViewModel = viewModel()
                    val sessionState by vm.state.collectAsState()
                    var pendingLanguageSw by remember { mutableStateOf(false) }

                    val projectionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) { result ->
                        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                            ScreenCaptureService.start(
                                this@MainActivity,
                                result.resultCode,
                                result.data!!,
                            )
                            vm.onCaptureStarted(useSwahili = pendingLanguageSw)
                        }
                    }

                    fun launchProjection() {
                        val mgr =
                            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        projectionLauncher.launch(mgr.createScreenCaptureIntent())
                    }

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) {
                        // Continue regardless — denial only hides the FGS notification chrome.
                        launchProjection()
                    }

                    fun onHelpRequested(useSwahili: Boolean) {
                        pendingLanguageSw = useSwahili
                        if (Build.VERSION.SDK_INT >= 33) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return
                            }
                        }
                        launchProjection()
                    }

                    DiraApp(
                        sessionState = sessionState,
                        onHelp = { useSwahili -> onHelpRequested(useSwahili) },
                        onStop = { vm.stopSession(showCleared = true) },
                        onAskGuide = { useSwahili -> vm.requestStep(useSwahili) },
                        onQuestionChange = vm::onQuestionChange,
                        onDismissCleared = vm::consumeClearedFlag,
                    )
                }
            }
        }
    }
}

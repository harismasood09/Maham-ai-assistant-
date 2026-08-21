package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.models.UserPreferences
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MahamMainScreen
import com.example.ui.screens.PermissionOnboardingView
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceBgDark
import com.example.ui.viewmodel.MainViewModel

enum class MahamScreen {
    MAIN,
    SETTINGS,
    HISTORY
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpaceBgDark
                ) {
                    MahamAppContainer(
                        viewModel = viewModel,
                        checkPermissions = { checkAllPermissions() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_TRIGGER_VOICE) {
            val phrase = intent.getStringExtra("detected_phrase")
            if (!phrase.isNullOrBlank()) {
                viewModel.processUserInput(phrase)
            } else {
                viewModel.startListening()
            }
        }
    }

    private fun checkAllPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        const val ACTION_TRIGGER_VOICE = "com.example.ACTION_TRIGGER_VOICE"
    }
}

@Composable
fun MahamAppContainer(
    viewModel: MainViewModel,
    checkPermissions: () -> Boolean
) {
    var currentScreen by remember { mutableStateOf(MahamScreen.MAIN) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val userPrefs by viewModel.userPreferences.collectAsState()
    val conversations by viewModel.conversationHistory.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        showPermissionDialog = false
        val audioGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] ?: false
        if (audioGranted && userPrefs.isFirstLaunch) {
            viewModel.markFirstLaunchComplete()
            viewModel.startListening()
        }
    }

    LaunchedEffect(userPrefs.isFirstLaunch) {
        if (userPrefs.isFirstLaunch && !checkPermissions()) {
            showPermissionDialog = true
        }
    }

    fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                MahamScreen.MAIN -> {
                    MahamMainScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentScreen = MahamScreen.SETTINGS },
                        onNavigateToHistory = { currentScreen = MahamScreen.HISTORY },
                        onOpenPermissionSheet = { showPermissionDialog = true }
                    )
                }
                MahamScreen.SETTINGS -> {
                    SettingsScreen(
                        currentPreferences = userPrefs,
                        onSavePreferences = { newPrefs ->
                            viewModel.updatePreferences(newPrefs)
                        },
                        onTestVoice = { pitch, rate, lang ->
                            viewModel.ttsManager.pitch = pitch
                            viewModel.ttsManager.speechRate = rate
                            val sample = when (lang) {
                                "ps" -> "سلام! زه ماہم یم، له تاسو سره مرستې ته چمتو یم."
                                "en" -> "Hello! I am Maham, your intelligent personal voice assistant."
                                else -> "السلام علیکم! میں ماہم ہوں، آپ کی ذہین آواز کی معاون۔"
                            }
                            viewModel.ttsManager.speak(sample, lang)
                        },
                        onIncreaseVolume = {
                            val outcome = viewModel.increaseVolume(2)
                            viewModel.ttsManager.speak(outcome.userFeedback, "ur")
                        },
                        onDecreaseVolume = {
                            val outcome = viewModel.decreaseVolume(2)
                            viewModel.ttsManager.speak(outcome.userFeedback, "ur")
                        },
                        onMaxVolume = {
                            val outcome = viewModel.maxVolume()
                            viewModel.ttsManager.speak(outcome.userFeedback, "ur")
                        },
                        onMuteVolume = {
                            viewModel.muteVolume()
                        },
                        onOpenSoundSettings = {
                            viewModel.toolEngine.execute("openSoundSettings", org.json.JSONObject())
                        },
                        onToggleFlashlight = { enable ->
                            viewModel.toggleFlashlight(enable)
                        },
                        onClearHistory = {
                            viewModel.clearHistory()
                        },
                        onBack = { currentScreen = MahamScreen.MAIN }
                    )
                }
                MahamScreen.HISTORY -> {
                    HistoryScreen(
                        conversations = conversations,
                        onClearHistory = { viewModel.clearHistory() },
                        onBack = { currentScreen = MahamScreen.MAIN }
                    )
                }
            }
        }

        // Permissions Dialog
        if (showPermissionDialog) {
            Dialog(
                onDismissRequest = { showPermissionDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    PermissionOnboardingView(
                        onRequestPermissions = { requestAppPermissions() },
                        onDismiss = {
                            showPermissionDialog = false
                            if (userPrefs.isFirstLaunch) {
                                viewModel.markFirstLaunchComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}

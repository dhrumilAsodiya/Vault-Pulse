package com.vaultpulse.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaultpulse.app.ui.screens.VaultPulseRoot
import com.vaultpulse.app.ui.theme.ObsidianColors
import com.vaultpulse.app.ui.theme.ObsidianFlowTheme
import com.vaultpulse.app.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and recent-apps thumbnail leaks
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        setContent {
            ObsidianFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianColors.Canvas
                ) {
                    if (isUnlocked) {
                        val vm: VaultViewModel = viewModel()
                        VaultPulseRoot(viewModel = vm)
                    } else {
                        // Branded biometric lock screen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            ObsidianColors.PrimaryContainer.copy(alpha = 0.08f),
                                            ObsidianColors.Canvas
                                        )
                                    )
                                )
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                // Pulsing ring + shield icon
                                val pulse = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by pulse.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.12f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1200),
                                        RepeatMode.Reverse
                                    ),
                                    label = "pulseScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(ObsidianColors.PrimaryContainer.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianColors.PrimaryContainer.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Shield,
                                            contentDescription = "VaultPulse",
                                            tint = ObsidianColors.PrimaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(28.dp))
                                Text(
                                    text = "VaultPulse",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianColors.OnSurface,
                                    letterSpacing = (-0.02).sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Your encrypted offline finance vault",
                                    fontSize = 13.sp,
                                    color = ObsidianColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(48.dp))
                                // Fingerprint tap target
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = ObsidianColors.SurfaceContainerHigh,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clickable { promptBiometricUnlock() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("☝", fontSize = 32.sp)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Touch sensor to unlock",
                                    fontSize = 12.sp,
                                    color = ObsidianColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Tap anywhere to retry",
                                    fontSize = 11.sp,
                                    color = ObsidianColors.PrimaryContainer.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.clickable { promptBiometricUnlock() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-trigger fingerprint scan every time app comes to foreground if still locked
        if (!isUnlocked) {
            promptBiometricUnlock()
        }
    }

    private fun checkAndRequestSmsPermissions() {
        val hasRead = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val hasReceive = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRead || !hasReceive) {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }

    private fun promptBiometricUnlock() {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        // If running on an emulator or device without any enrolled fingerprint or screen lock PIN,
        // unlock directly so the user is not locked out.
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            isUnlocked = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If no biometrics enrolled or hardware unavailable, allow access
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                    ) {
                        isUnlocked = true
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock VaultPulse")
            .setSubtitle("Use fingerprint or device PIN to decrypt your vault")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}

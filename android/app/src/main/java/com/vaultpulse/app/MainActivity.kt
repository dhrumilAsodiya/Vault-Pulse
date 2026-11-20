package com.vaultpulse.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaultpulse.app.ui.screens.VaultPulseRoot
import com.vaultpulse.app.ui.theme.ObsidianColors
import com.vaultpulse.app.ui.theme.ObsidianFlowTheme
import com.vaultpulse.app.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and recent-apps thumbnail leaks
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        promptBiometricUnlock()

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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "VaultPulse Locked",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = ObsidianColors.OnSurface
                                )
                                Text(
                                    text = "Authenticate with device biometrics to decrypt your offline vault.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ObsidianColors.OnSurfaceVariant
                                )
                                Button(
                                    onClick = { promptBiometricUnlock() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ObsidianColors.PrimaryIndigo,
                                        contentColor = ObsidianColors.Canvas
                                    )
                                ) {
                                    Text("Unlock Vault", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun promptBiometricUnlock() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock VaultPulse")
            .setSubtitle("Access encrypted on-device accounts & ledger")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}

package com.streampanel

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class DiagnosticActivity : ComponentActivity() {
    private var lastCrash by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("diagnostic_crash", MODE_PRIVATE)
        lastCrash = prefs.getString("last_crash", "").orEmpty()
        enableEdgeToEdge()
        setContent {
            DiagnosticScreen(
                lastCrash = lastCrash,
                onOpenMain = {
                    prefs.edit().remove("last_crash").apply()
                    lastCrash = ""
                    startActivity(Intent(this, MainActivity::class.java))
                },
                onClearCrash = {
                    prefs.edit().remove("last_crash").apply()
                    lastCrash = ""
                },
            )
        }
    }
}

@Composable
private fun DiagnosticScreen(
    lastCrash: String,
    onOpenMain: () -> Unit,
    onClearCrash: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "StreamPanel diagnostic APK",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Если этот экран открылся, APK и телефон в порядке. Значит, падение внутри основного приложения.",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onOpenMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                ) {
                    Text("Открыть основное приложение")
                }
                if (lastCrash.isNotBlank()) {
                    OutlinedButton(
                        onClick = onClearCrash,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Очистить ошибку")
                    }
                    Text(
                        text = "Последняя ошибка:",
                        modifier = Modifier.padding(top = 18.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SelectionContainer {
                        Text(
                            text = lastCrash,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

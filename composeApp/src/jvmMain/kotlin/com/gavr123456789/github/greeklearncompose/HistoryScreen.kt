package com.gavr123456789.github.sovazeleboba

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

private data class HistoryEntry(
    val name: String?,
    val correct: Int?,
    val errors: Int?,
    val successRate: Int?,
    val time: String?,
    val timeSeconds: Int?,
)

@Composable
fun HistoryScreen(
    onBackToMenu: () -> Unit,
) {
    val entriesState = remember { mutableStateOf<List<HistoryEntry>?>(null) }

    LaunchedEffect(Unit) {
        entriesState.value = loadHistoryEntries()
    }

    val entries = entriesState.value

    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onBackToMenu,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("← Меню")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (entries == null) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (entries.isEmpty()) {
            Text(
                text = "No results yet",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val sortedEntries = entries.sortedWith(
                compareBy<HistoryEntry> { it.name ?: "" }
                    .thenBy { it.timeSeconds ?: Int.MAX_VALUE }
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Заголовок таблицы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f),
                    )
                    Text(
                        text = "Correct",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Errors",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Success",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                sortedEntries.forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = entry.name ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.2f),
                        )
                        Text(
                            text = "${entry.correct ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${entry.errors ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${entry.successRate ?: "-"}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = entry.time ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBackToMenu) {
            Text(text = "Back to main menu")
        }
        }
    }
}

private fun loadHistoryEntries(): List<HistoryEntry> {
    return try {
        val homeDir = System.getProperty("user.home") ?: return emptyList()
        val dir = File(homeDir, ".config/Sova")
        val file = File(dir, "results.txt")
        if (!file.exists()) return emptyList()

        file.readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@mapNotNull null

                var name: String? = null
                var correct: Int? = null
                var errors: Int? = null
                var successRate: Int? = null
                var time: String? = null
                var timeSeconds: Int? = null

                trimmed.split(";").forEach { part ->
                    val kv = part.split("=", limit = 2)
                    if (kv.size == 2) {
                        val key = kv[0].trim()
                        val value = kv[1].trim()
                        when (key) {
                            "name" -> name = value.ifBlank { null }
                            "correct" -> correct = value.toIntOrNull()
                            "errors" -> errors = value.toIntOrNull()
                            "successRate" -> {
                                val numeric = value.removeSuffix("%")
                                successRate = numeric.toIntOrNull()
                            }
                            "time" -> {
                                time = value
                                // ожидаем формат mm:ss
                                val parts = value.split(":")
                                if (parts.size == 2) {
                                    val minutes = parts[0].toIntOrNull()
                                    val seconds = parts[1].toIntOrNull()
                                    if (minutes != null && seconds != null) {
                                        timeSeconds = minutes * 60 + seconds
                                    }
                                }
                            }
                        }
                    }
                }

                HistoryEntry(
                    name = name,
                    correct = correct,
                    errors = errors,
                    successRate = successRate,
                    time = time,
                    timeSeconds = timeSeconds,
                )
            }
    } catch (_: Exception) {
        emptyList()
    }
}

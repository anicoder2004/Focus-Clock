package com.anisoft.focusclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisoft.focusclock.data.AlarmModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingNoteCard(
    alarm: AlarmModel,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    snoozeOptions: List<Int> = listOf(5, 10, 15, 30)
) {
    var expandedSnooze by remember { mutableStateOf(false) }
    val snoozeText = "Snooze"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ Focus Clock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }

            Text(
                text = alarm.note.ifEmpty { "Time to focus!" },
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { expandedSnooze = !expandedSnooze },
                        modifier = Modifier.padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(snoozeText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedSnooze,
                        onDismissRequest = { expandedSnooze = false }
                    ) {
                        snoozeOptions.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("${minutes} min", fontSize = 16.sp) },
                                onClick = {
                                    onSnooze(minutes)
                                    expandedSnooze = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Dismiss", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
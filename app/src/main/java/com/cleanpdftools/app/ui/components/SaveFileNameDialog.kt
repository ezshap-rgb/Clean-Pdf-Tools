package com.cleanpdftools.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun SaveFileNameDialog(
    initialName: String,
    extension: String = "pdf",
    title: String = "Save Document",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = AppColors.Text,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter output file name:",
                    color = AppColors.TextMuted,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = ".$extension",
                            color = AppColors.TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.Border,
                        focusedTextColor = AppColors.Text,
                        unfocusedTextColor = AppColors.Text,
                        focusedContainerColor = AppColors.SurfaceRaised,
                        unfocusedContainerColor = AppColors.SurfaceRaised
                    ),
                    shape = RoundedCornerShape(AppRadius.md),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = nameText.trim().ifBlank { initialName }
                    onConfirm(finalName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                shape = RoundedCornerShape(AppRadius.pill)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextMuted)
            }
        },
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(AppRadius.card),
        modifier = Modifier.imePadding()
    )
}


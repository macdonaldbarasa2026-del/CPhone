package com.cphone.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerSheet(
    initialDirectory: File,
    onFileSelected: (File) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    var currentDir by remember { mutableStateOf(initialDirectory) }
    var filesList by remember(currentDir) {
        mutableStateOf(
            try {
                currentDir.listFiles()?.sortedWith(
                    compareBy({ !it.isDirectory }, { it.name.lowercase() })
                ) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        )
    }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header with current directory path and navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Up button
                IconButton(
                    onClick = {
                        val parent = currentDir.parentFile
                        if (parent != null && parent.canRead()) {
                            currentDir = parent
                        }
                    },
                    enabled = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go up")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "File Explorer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = currentDir.absolutePath,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // New file button
                IconButton(onClick = { showNewFileDialog = true }) {
                    Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "New File")
                }
                // New folder button
                IconButton(onClick = { showNewFolderDialog = true }) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // File items list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filesList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Empty folder or access restricted", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                items(filesList, key = { it.absolutePath }) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else {
                                    onFileSelected(file)
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (file.isDirectory) Color(0xFFFFB300) else Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!file.isDirectory) {
                                val sizeKb = file.length() / 1024.0
                                Text(
                                    text = String.format("%.1f KB", sizeKb),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Rename & Delete actions
                        IconButton(
                            onClick = { fileToRename = file },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { fileToDelete = file },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("script.bat") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val file = File(currentDir, newFileName.trim())
                    try {
                        file.createNewFile()
                        filesList = currentDir.listFiles()?.toList() ?: emptyList()
                    } catch (_: Exception) {}
                    showNewFileDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("new_folder") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val dir = File(currentDir, folderName.trim())
                    try {
                        dir.mkdirs()
                        filesList = currentDir.listFiles()?.toList() ?: emptyList()
                    } catch (_: Exception) {}
                    showNewFolderDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Confirmation") },
            text = { Text("Are you sure you want to delete '${file.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                        filesList = currentDir.listFiles()?.toList() ?: emptyList()
                    } catch (_: Exception) {}
                    fileToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    fileToRename?.let { file ->
        var renameName by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val target = File(file.parentFile, renameName.trim())
                        file.renameTo(target)
                        filesList = currentDir.listFiles()?.toList() ?: emptyList()
                    } catch (_: Exception) {}
                    fileToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }
}

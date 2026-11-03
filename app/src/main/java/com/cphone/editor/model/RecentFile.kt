package com.cphone.editor.model

import android.net.Uri

data class RecentFile(
    val name: String,
    val path: String,
    val uri: Uri
)

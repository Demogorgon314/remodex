package com.emanueledipietro.remodex.platform.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.emanueledipietro.remodex.model.RemodexComposerAttachment

fun resolveComposerAttachments(
    context: Context,
    uris: List<Uri>,
): List<RemodexComposerAttachment> {
    return uris.map { uri ->
        val uriString = uri.toString()
        RemodexComposerAttachment(
            id = uriString,
            uriString = uriString,
            displayName = context.resolveDisplayName(uri),
        )
    }
}

private fun Context.resolveDisplayName(uri: Uri): String {
    return contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (displayNameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(displayNameIndex)
        } else {
            null
        }
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Selected image"
}

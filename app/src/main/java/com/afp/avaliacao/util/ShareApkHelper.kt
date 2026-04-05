package com.afp.avaliacao.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ShareApkHelper(private val context: Context) {
    fun shareApp() {
        try {
            val appFile = File(context.applicationInfo.publicSourceDir)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                appFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartilhar APK AFP"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

package com.samuel.retodivisas.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class MediaRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadBitmap(
        context: Context,
        bitmap: Bitmap,
        folder: String,
        prefix: String
    ): String = withContext(Dispatchers.IO) {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }

        runCatching {
            val ref = storage.reference.child("$folder/$fileName")
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            if (downloadUrl.isNotBlank()) {
                return@withContext downloadUrl
            }
        }

        val localFile = File(context.filesDir, fileName)
        localFile.writeBytes(bytes)
        localFile.absolutePath
    }

    suspend fun uploadUri(
        context: Context,
        uri: Uri,
        folder: String,
        prefix: String
    ): String? = withContext(Dispatchers.IO) {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null

        runCatching {
            val ref = storage.reference.child("$folder/$fileName")
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            if (downloadUrl.isNotBlank()) {
                return@withContext downloadUrl
            }
        }

        val localFile = File(context.filesDir, fileName)
        localFile.writeBytes(bytes)
        localFile.absolutePath
    }
}

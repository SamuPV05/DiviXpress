package com.samuel.retodivisas.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samuel.retodivisas.data.AppUser
import com.samuel.retodivisas.data.FirestoreTransactionRepository
import com.samuel.retodivisas.data.MediaRepository
import com.samuel.retodivisas.data.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val firestoreRepo = FirestoreTransactionRepository()
    private val mediaRepo = MediaRepository()
    private val uid: String? get() = SessionManager.currentUser?.uid

    val user: Flow<AppUser?> = uid?.let { firestoreRepo.observeUser(it) } ?: emptyFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    fun changePhoto(uri: Uri) {
        val userId = uid ?: return
        viewModelScope.launch {
            _uploading.value = true
            try {
                val urlOrPath = mediaRepo.uploadUri(
                    context = getApplication(),
                    uri = uri,
                    folder = "profiles",
                    prefix = "profile_$userId"
                )
                if (urlOrPath != null) {
                    firestoreRepo.updateProfilePhoto(userId, urlOrPath)
                }
            } finally {
                _uploading.value = false
            }
        }
    }

    fun changePhotoBitmap(bitmap: Bitmap) {
        val userId = uid ?: return
        viewModelScope.launch {
            _uploading.value = true
            try {
                val urlOrPath = mediaRepo.uploadBitmap(
                    context = getApplication(),
                    bitmap = bitmap,
                    folder = "profiles",
                    prefix = "profile_$userId"
                )
                firestoreRepo.updateProfilePhoto(userId, urlOrPath)
            } finally {
                _uploading.value = false
            }
        }
    }
}

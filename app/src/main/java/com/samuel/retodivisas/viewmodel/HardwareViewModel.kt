package com.samuel.retodivisas.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.samuel.retodivisas.hardware.CameraHelper
import com.samuel.retodivisas.hardware.VibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HardwareUiState(
    val hasVibrator: Boolean = false,
    val hasCameraHardware: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val capturedBitmap: Bitmap? = null
)

class HardwareViewModel(app: Application) : AndroidViewModel(app) {
    private val vibrationHelper = VibrationHelper(app)

    private val _uiState = MutableStateFlow(
        HardwareUiState(
            hasVibrator = vibrationHelper.hasVibrator(),
            hasCameraHardware = CameraHelper.hasCameraHardware(app),
            cameraPermissionGranted = CameraHelper.hasCameraPermission(app)
        )
    )
    val uiState: StateFlow<HardwareUiState> = _uiState.asStateFlow()

    fun updateCameraPermission(isGranted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = isGranted) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        vibrateSuccess()
        _uiState.update { it.copy(capturedBitmap = bitmap) }
    }

    fun vibrateClick() {
        vibrationHelper.vibrateClick()
    }

    fun vibrateSuccess() {
        vibrationHelper.vibrateSuccess()
    }

    fun vibrateError() {
        vibrationHelper.vibrateError()
    }

    fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        vibrationHelper.vibrateWaveform(timings, amplitudes)
    }
}

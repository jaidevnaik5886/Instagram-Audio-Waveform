package com.jaidev.instagramstyleaudiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.instagramstyleaudiorecorder.R
import com.example.instagramstyleaudiorecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null

    private val binding get() = _binding!!
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private var isRecording = false
    private var recordingStartTime: Long = 0L

    private var recordingJob: Job? = null
    private var waveFormJob: Job? = null

    private val recorderLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                startRecording() // Start recording if all permissions are granted
            } else {
                Toast.makeText(this, "Audio Permissions are required", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initInstagramRecorder()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording(delete = true) // Clean up recording resources if still recording
        }
        _binding = null
    }

    private fun initInstagramRecorder() {
        binding.btnRecord.setOnClickListener {
            requestAudioPermissions()
        }
        binding.ivDeleteAudio.setOnClickListener { stopRecording(delete = true) }
        binding.ivSendAudio.setOnClickListener { stopRecording(delete = false) }
    }

    private fun requestAudioPermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Only needed for API 28 and below
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (requiredPermissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startRecording()
        } else {
            recorderLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun startRecording() {
        if (isRecording) return

        try {
            setupMediaRecorder()
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            showRecordingUI()

            startUpdatingDuration()
            startWaveformUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMediaRecorder() {
        audioFilePath = "${externalCacheDir?.absolutePath}/audio_${System.currentTimeMillis()}.mp3"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
        }
    }

    private fun stopRecording(delete: Boolean) {
        if (!isRecording) return

        try {
            mediaRecorder?.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            releaseMediaRecorder()
        }

        isRecording = false
        stopUpdatingDuration()
        stopWaveformUpdates()

        if (delete) {
            deleteAudioFile()
        } else {
            sendAudio(audioFilePath)
        }

        resetRecordingUI()
    }

    private fun deleteAudioFile() {
        File(audioFilePath).delete()
        Toast.makeText(this, "Deleted : $audioFilePath", Toast.LENGTH_LONG).show()
    }

    private fun releaseMediaRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }

    private fun sendAudio(filePath: String) {
        // Handle the audio file upload/send logic here.
        Toast.makeText(this, "AudioFile : $filePath", Toast.LENGTH_LONG).show()
    }

    private fun showRecordingUI() {
        binding.clAudioRecording.visibility = View.VISIBLE
        binding.btnRecord.visibility = View.GONE
    }

    private fun resetRecordingUI() {
        binding.clAudioRecording.visibility = View.GONE
        binding.btnRecord.visibility = View.VISIBLE
        binding.txtDuration.text = "00:00"
        binding.waveformView.resetWaveform() // Reset waveform
    }

    // Start updating the duration display
    private fun startUpdatingDuration() {
        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isRecording) {
                val elapsedMillis = System.currentTimeMillis() - recordingStartTime
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / 1000) / 60
                binding.txtDuration.text = String.format("%02d:%02d", minutes, seconds)
                delay(1000L)
            }
        }
    }

    // Stop updating the duration display
    private fun stopUpdatingDuration() {
        recordingJob?.cancel()
    }

    // Start updating the waveform
    private fun startWaveformUpdates() {
        waveFormJob = lifecycleScope.launch {
            while (isRecording) {
                val amplitude = mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
                val normalizedAmplitude = amplitude / 32767f // Normalize amplitude
                binding.waveformView.updateAmplitude(normalizedAmplitude)
                delay(50) // Update every 50ms
            }
        }
    }

    // Stop updating the waveform
    private fun stopWaveformUpdates() {
        waveFormJob?.cancel()
        binding.waveformView.updateAmplitude(0f) // Reset waveform
    }

}
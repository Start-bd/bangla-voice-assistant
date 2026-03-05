package com.banglavoiceassistant.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.Base64

class VoiceRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecorder"
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }
        
        try {
            outputFile = File(context.cacheDir, "voice_recording_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e(TAG, "MediaRecorder prepare failed: ${e.message}")
                    return false
                }
                
                start()
                isRecording = true
                Log.d(TAG, "Recording started")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}")
            return false
        }
        
        return false
    }
    
    fun stopRecording(): File? {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return null
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            Log.d(TAG, "Recording stopped. File: ${outputFile?.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            return null
        }
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getAudioBase64(): String? {
        return try {
            outputFile?.let { file ->
                val bytes = file.readBytes()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getEncoder().encodeToString(bytes)
                } else {
                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to base64: ${e.message}")
            null
        }
    }
    
    fun cleanup() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up: ${e.message}")
        }
    }
}

package com.banglavoiceassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.banglavoiceassistant.R
import com.banglavoiceassistant.data.AgentResponse
import com.banglavoiceassistant.data.RetrofitClient
import com.banglavoiceassistant.data.TestRequest
import com.banglavoiceassistant.data.VoiceCommandRequest
import com.banglavoiceassistant.ui.MainActivity
import com.banglavoiceassistant.util.ActionDispatcher
import com.banglavoiceassistant.util.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingBubbleService : Service() {
    
    companion object {
        private const val TAG = "FloatingBubbleService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "voice_assistant_channel"
        
        const val ACTION_SHOW_BUBBLE = "action_show_bubble"
        const val ACTION_HIDE_BUBBLE = "action_hide_bubble"
        const val ACTION_STOP_SERVICE = "action_stop_service"
    }
    
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var resultPanelView: View? = null
    private lateinit var voiceRecorder: VoiceRecorder
    
    private var isRecording = false
    private var isPanelVisible = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        voiceRecorder = VoiceRecorder(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_BUBBLE -> showBubble()
            ACTION_HIDE_BUBBLE -> hideBubble()
            ACTION_STOP_SERVICE -> stopSelf()
            else -> showBubble()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        hideBubble()
        hideResultPanel()
        voiceRecorder.cleanup()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice assistant floating bubble service"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bangla Voice Assistant")
            .setContentText("Tap to open app")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun showBubble() {
        if (bubbleView != null) return
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
        
        bubbleView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)
        
        bubbleView?.let { view ->
            setupBubbleTouch(view, params)
            
            view.findViewById<ImageButton>(R.id.bubble_button)?.setOnClickListener {
                toggleResultPanel()
            }
            
            windowManager.addView(view, params)
        }
    }
    
    private fun setupBubbleTouch(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun hideBubble() {
        bubbleView?.let {
            windowManager.removeView(it)
            bubbleView = null
        }
    }
    
    private fun toggleResultPanel() {
        if (isPanelVisible) {
            hideResultPanel()
        } else {
            showResultPanel()
        }
    }
    
    private fun showResultPanel() {
        if (resultPanelView != null) return
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        resultPanelView = LayoutInflater.from(this).inflate(R.layout.result_panel, null)
        
        resultPanelView?.let { view ->
            setupResultPanel(view)
            windowManager.addView(view, params)
            isPanelVisible = true
        }
    }
    
    private fun hideResultPanel() {
        resultPanelView?.let {
            windowManager.removeView(it)
            resultPanelView = null
            isPanelVisible = false
        }
    }
    
    private fun setupResultPanel(view: View) {
        val micButton = view.findViewById<ImageButton>(R.id.mic_button)
        val closeButton = view.findViewById<ImageButton>(R.id.close_button)
        val copyButton = view.findViewById<ImageButton>(R.id.copy_button)
        val shareButton = view.findViewById<ImageButton>(R.id.share_button)
        val resultText = view.findViewById<TextView>(R.id.result_text)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        
        micButton?.setOnClickListener {
            if (isRecording) {
                stopRecordingAndProcess()
                micButton.setImageResource(R.drawable.ic_mic)
            } else {
                startRecording()
                micButton.setImageResource(R.drawable.ic_stop)
            }
        }
        
        closeButton?.setOnClickListener {
            hideResultPanel()
        }
        
        copyButton?.setOnClickListener {
            val text = resultText?.text?.toString() ?: ""
            if (text.isNotEmpty() && text != "Tap mic and speak in Bangla...") {
                copyToClipboard(text)
            }
        }
        
        shareButton?.setOnClickListener {
            val text = resultText?.text?.toString() ?: ""
            if (text.isNotEmpty() && text != "Tap mic and speak in Bangla...") {
                shareText(text)
            }
        }
    }
    
    private fun startRecording() {
        if (voiceRecorder.startRecording()) {
            isRecording = true
            updateResultText("Listening... Speak in Bangla")
            showProgress(true)
        } else {
            showToast("Failed to start recording")
        }
    }
    
    private fun stopRecordingAndProcess() {
        isRecording = false
        showProgress(true)
        updateResultText("Processing...")
        
        val audioFile = voiceRecorder.stopRecording()
        
        if (audioFile != null) {
            processAudio(audioFile.readBytes())
        } else {
            showProgress(false)
            updateResultText("Error recording audio")
        }
    }
    
    private fun processAudio(audioBytes: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert to base64
                val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.DEFAULT)
                
                // Send to backend
                val request = VoiceCommandRequest(
                    audioBase64 = base64Audio,
                    metadata = com.banglavoiceassistant.data.Metadata(
                        language = "bn",
                        mode = "agent"
                    )
                )
                
                val response = RetrofitClient.apiService.sendVoiceCommandJson(request)
                
                withContext(Dispatchers.Main) {
                    showProgress(false)
                    
                    if (response.isSuccessful) {
                        response.body()?.let { agentResponse ->
                            handleAgentResponse(agentResponse)
                        }
                    } else {
                        updateResultText("Error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio: ${e.message}")
                withContext(Dispatchers.Main) {
                    showProgress(false)
                    updateResultText("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun handleAgentResponse(response: AgentResponse) {
        val displayText = buildString {
            appendLine("Bangla: ${response.transcriptBn}")
            appendLine()
            appendLine("English: ${response.englishText}")
            appendLine()
            appendLine("Action: ${response.intent}")
        }
        
        updateResultText(displayText)
        
        // Execute the action
        val result = ActionDispatcher.dispatch(this, response)
        when (result) {
            is ActionDispatcher.ActionResult.Success -> {
                showToast(result.message)
            }
            is ActionDispatcher.ActionResult.Error -> {
                showToast("Error: ${result.message}")
            }
        }
    }
    
    private fun updateResultText(text: String) {
        resultPanelView?.findViewById<TextView>(R.id.result_text)?.text = text
    }
    
    private fun showProgress(show: Boolean) {
        resultPanelView?.findViewById<ProgressBar>(R.id.progress_bar)?.visibility = 
            if (show) View.VISIBLE else View.GONE
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Voice Result", text)
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")
    }
    
    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(intent, "Share via")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(shareIntent)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

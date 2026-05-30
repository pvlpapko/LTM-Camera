package com.pvlpapko.lowlatencycam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.library.view.OpenGlView
import com.pedro.rtspserver.RtspServerCamera2

class MainActivity : AppCompatActivity(), ConnectChecker {

    private lateinit var openGlView: OpenGlView
    private lateinit var rtspCamera: RtspServerCamera2
    private lateinit var statusText: TextView

    private var micEnabled = true
    private var isFrontCamera = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openGlView = OpenGlView(this)
        statusText = TextView(this)

        val startButton = Button(this).apply { text = "Start RTSP Stream" }
        val stopButton = Button(this).apply { text = "Stop Stream" }
        val switchButton = Button(this).apply { text = "Switch Camera" }
        val micButton = Button(this).apply { text = "Mic: ON" }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)

            addView(
                openGlView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            addView(statusText)
            addView(startButton)
            addView(stopButton)
            addView(switchButton)
            addView(micButton)
        }

        setContentView(root)

        statusText.text = "RTSP ready. URL: rtsp://PHONE_IP:8554"

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                1001
            )
        } else {
            setupStreamer()
        }

        startButton.setOnClickListener {
            startStream()
        }

        stopButton.setOnClickListener {
            stopStream()
        }

        switchButton.setOnClickListener {
            try {
                rtspCamera.switchCamera()
                isFrontCamera = !isFrontCamera
                statusText.text = if (isFrontCamera) {
                    "Front camera"
                } else {
                    "Back camera"
                }
            } catch (e: Exception) {
                statusText.text = "Camera switch error: ${e.message}"
            }
        }

        micButton.setOnClickListener {
            micEnabled = !micEnabled
            micButton.text = if (micEnabled) "Mic: ON" else "Mic: OFF"
            statusText.text = if (micEnabled) "Microphone enabled" else "Microphone disabled"
        }
    }

    private fun setupStreamer() {
        rtspCamera = RtspServerCamera2(openGlView, this, 8554)

        if (!rtspCamera.isOnPreview) {
            rtspCamera.startPreview()
        }
    }

    private fun startStream() {
        if (!::rtspCamera.isInitialized) {
            setupStreamer()
        }

        if (rtspCamera.isStreaming) {
            statusText.text = "Already streaming: rtsp://PHONE_IP:8554"
            return
        }

        val videoReady = rtspCamera.prepareVideo(
            1280,
            720,
            30,
            2_500_000,
            0
        )

        val audioReady = if (micEnabled) {
            rtspCamera.prepareAudio()
        } else {
            true
        }

        if (videoReady && audioReady) {
            rtspCamera.startStream()
            statusText.text = "Streaming: rtsp://PHONE_IP:8554"
        } else {
            statusText.text = "Prepare stream failed"
        }
    }

    private fun stopStream() {
        if (::rtspCamera.isInitialized && rtspCamera.isStreaming) {
            rtspCamera.stopStream()
            statusText.text = "Stream stopped"
        }
    }

    private fun hasPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val audio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return camera && audio
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && hasPermissions()) {
            setupStreamer()
        } else {
            statusText.text = "Camera/audio permissions required"
        }
    }

    override fun onConnectionStarted(url: String) {
        runOnUiThread {
            statusText.text = "Connection started"
        }
    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            statusText.text = "Streaming: rtsp://PHONE_IP:8554"
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            statusText.text = "Connection failed: $reason"
        }
    }

    override fun onNewBitrate(bitrate: Long) {}

    override fun onDisconnect() {
        runOnUiThread {
            statusText.text = "Disconnected"
        }
    }

    override fun onAuthError() {
        runOnUiThread {
            statusText.text = "Auth error"
        }
    }

    override fun onAuthSuccess() {
        runOnUiThread {
            statusText.text = "Auth success"
        }
    }
}

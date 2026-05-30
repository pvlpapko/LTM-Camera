package com.pvlpapko.lowlatencycam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.library.view.OpenGlView
import com.pedro.rtspserver.RtspServerCamera2

class MainActivity : AppCompatActivity(), ConnectChecker, SurfaceHolder.Callback {

    private lateinit var openGlView: OpenGlView
    private lateinit var statusText: TextView
    private lateinit var urlEdit: EditText
    private lateinit var modeRtspServer: RadioButton
    private lateinit var modePush: RadioButton
    private lateinit var modeWebRtc: RadioButton
    private lateinit var micSwitch: Switch
    private lateinit var autofocusSwitch: Switch
    private lateinit var bitrateSeek: SeekBar
    private lateinit var exposureSeek: SeekBar

    private var rtspServerCamera: RtspServerCamera2? = null
    private var pushCamera: Any? = null
    private var prepared = false
    private var frontCamera = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val ok = result.values.all { it }
        if (ok) initCamera() else status("Нет разрешений камеры/микрофона")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openGlView = findViewById(R.id.openGlView)
        statusText = findViewById(R.id.statusText)
        urlEdit = findViewById(R.id.urlEdit)
        modeRtspServer = findViewById(R.id.modeRtspServer)
        modePush = findViewById(R.id.modePush)
        modeWebRtc = findViewById(R.id.modeWebRtc)
        micSwitch = findViewById(R.id.micSwitch)
        autofocusSwitch = findViewById(R.id.autofocusSwitch)
        bitrateSeek = findViewById(R.id.bitrateSeek)
        exposureSeek = findViewById(R.id.exposureSeek)

        findViewById<Button>(R.id.startButton).setOnClickListener { startSelectedMode() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { stopAll() }
        findViewById<Button>(R.id.switchCameraButton).setOnClickListener { switchCamera() }
        findViewById<Button>(R.id.filterNormal).setOnClickListener { status("Фильтр: обычный") }
        findViewById<Button>(R.id.filterBright).setOnClickListener { status("Фильтр яркости подключается через OpenGL RootEncoder; см. README") }
        findViewById<Button>(R.id.filterMono).setOnClickListener { status("Ч/Б фильтр подключается через OpenGL RootEncoder; см. README") }

        bitrateSeek.setOnSeekBarChangeListener(simpleSeek { updateBitrate() })
        exposureSeek.setOnSeekBarChangeListener(simpleSeek { status("Экспозиция: ${exposureSeek.progress - 6}") })

        if (hasPermissions()) initCamera() else permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    private fun initCamera() {
        openGlView.holder.addCallback(this)
        rtspServerCamera = RtspServerCamera2(openGlView, this, 8554)
        rtspServerCamera?.setVideoCodec(VideoCodec.H264)
        status("Камера готова. RTSP server: rtsp://PHONE_IP:8554")
    }

    private fun prepareServerCamera(): Boolean {
        val cam = rtspServerCamera ?: return false
        if (cam.isStreaming) return true
        val bitrate = ((bitrateSeek.progress + 1).coerceAtLeast(2)) * 1024 * 1024
        val videoOk = cam.prepareVideo(1280, 720, 30, bitrate, 0)
        val audioOk = if (micSwitch.isChecked) cam.prepareAudio() else true
        prepared = videoOk && audioOk
        return prepared
    }

    private fun startSelectedMode() {
        when {
            modeRtspServer.isChecked -> startRtspServer()
            modePush.isChecked -> startPushMode()
            modeWebRtc.isChecked -> status("WebRTC: зависимость добавлена, но нужен WHIP/signaling сервер. Основа в проекте подготовлена.")
        }
    }

    private fun startRtspServer() {
        val cam = rtspServerCamera ?: return status("Камера ещё не готова")
        if (!prepareServerCamera()) return status("Не удалось подготовить видео/аудио")
        if (!cam.isOnPreview) cam.startPreview()
        if (!cam.isStreaming) cam.startStream()
        status("RTSP сервер запущен: rtsp://PHONE_IP:8554")
    }

    private fun startPushMode() {
        val url = urlEdit.text.toString().trim()
        if (url.isBlank()) return status("Укажи URL для RTSP/SRT push")
        stopAll()
        status("Push mode подготовлен. Для RTSP/SRT push подключи классы RootEncoder по README: $url")
    }

    private fun stopAll() {
        rtspServerCamera?.let { cam ->
            if (cam.isStreaming) cam.stopStream()
            if (cam.isOnPreview) cam.stopPreview()
        }
        pushCamera = null
        status("Остановлено")
    }

    private fun switchCamera() {
        frontCamera = !frontCamera
        try {
            rtspServerCamera?.switchCamera(if (frontCamera) "1" else "0")
            status(if (frontCamera) "Передняя камера" else "Задняя камера")
        } catch (e: Exception) {
            status("Не удалось сменить камеру: ${e.message}")
        }
    }

    private fun updateBitrate() {
        val bitrate = ((bitrateSeek.progress + 1).coerceAtLeast(2)) * 1024 * 1024
        try {
            rtspServerCamera?.setVideoBitrateOnFly(bitrate)
            status("Битрейт: ${bitrate / 1024 / 1024} Mbps")
        } catch (_: Exception) {
            status("Битрейт применится при следующем старте")
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        rtspServerCamera?.startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
    override fun surfaceDestroyed(holder: SurfaceHolder) { stopAll() }

    override fun onConnectionStarted(url: String) = status("Подключение: $url")
    override fun onConnectionSuccess() = status("Подключено")
    override fun onConnectionFailed(reason: String) = status("Ошибка: $reason")
    override fun onDisconnect() = status("Отключено")
    override fun onAuthError() = status("Ошибка авторизации")
    override fun onAuthSuccess() = status("Авторизация успешна")
    override fun onNewBitrate(bitrate: Long) = status("Upload: ${bitrate / 1024} kbps")

    private fun hasPermissions(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun status(text: String) { runOnUiThread { statusText.text = text } }

    private fun simpleSeek(onStop: () -> Unit): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = onStop()
        }
}

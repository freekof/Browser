package com.freekof.tvbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var controlPanel: LinearLayout
    private lateinit var controlScrim: View
    private lateinit var addressBar: EditText
    private lateinit var refreshButton: Button
    private val videoSniffer = VideoSniffer()
    private var lanInputServer: LanInputServer? = null
    private lateinit var socks5Store: Socks5SettingsStore
    private var loading = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        controlPanel = findViewById(R.id.controlPanel)
        controlScrim = findViewById(R.id.controlScrim)
        addressBar = findViewById(R.id.addressBar)
        refreshButton = findViewById(R.id.refreshButton)
        socks5Store = Socks5SettingsStore(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = DEFAULT_USER_AGENT
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                addressBar.setText(request.url.toString())
                return false
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                super.shouldInterceptRequest(view, request).also {
                    videoSniffer.record(request.url.toString())
                    updateVideoButton()
                }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                loading = true
                updateRefreshButton()
                addressBar.setText(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                loading = false
                updateRefreshButton()
                addressBar.setText(url)
            }
        }

        webView.setOnGenericMotionListener { _, event ->
            if (event.buttonState and MotionEvent.BUTTON_SECONDARY != 0) {
                showControls()
                true
            } else {
                false
            }
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<Button>(R.id.forwardButton).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        refreshButton.setOnClickListener {
            if (loading) {
                webView.stopLoading()
                loading = false
                updateRefreshButton()
            } else {
                loading = true
                updateRefreshButton()
                webView.reload()
            }
        }
        findViewById<Button>(R.id.qrButton).setOnClickListener { showQrInputDialog() }
        findViewById<Button>(R.id.proxyButton).setOnClickListener { showSocks5SettingsDialog() }
        findViewById<Button>(R.id.exitButton).setOnClickListener { finish() }
        controlScrim.setOnClickListener { hideControls() }

        addressBar.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                loadFromAddressBar()
                true
            } else {
                false
            }
        }

        hideSystemUi()
        loading = true
        updateRefreshButton()
        webView.loadUrl(HOME_URL)
    }

    override fun onBackPressed() {
        handleBackKey()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            handleBackKey()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleBackKey() {
        when (BackKeyPolicy.decide(controlPanel.visibility == View.VISIBLE, webView.canGoBack())) {
            BackKeyAction.ShowControls -> showControls()
            BackKeyAction.NavigateBack -> {
                hideControls()
                webView.goBack()
            }
            BackKeyAction.Exit -> super.onBackPressed()
        }
    }

    private fun loadFromAddressBar() {
        val url = UrlNormalizer.normalize(addressBar.text.toString())
        loading = true
        updateRefreshButton()
        webView.loadUrl(url)
        hideControls()
    }

    private fun showControls() {
        controlScrim.visibility = View.VISIBLE
        controlPanel.visibility = View.VISIBLE
        addressBar.requestFocus()
    }

    private fun hideControls() {
        controlScrim.visibility = View.GONE
        controlPanel.visibility = View.GONE
        hideSystemUi()
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun updateVideoButton() {
        // Video list UI will move to a later compact control after the requested toolbar change.
    }

    private fun updateRefreshButton() {
        refreshButton.text = if (loading) "S" else "R"
    }

    private fun showQrInputDialog() {
        val content = LanInputEndpoint.qrContent(findLanIpAddress(), LAN_INPUT_PORT)
        lanInputServer?.stop()
        lanInputServer = LanInputServer(LAN_INPUT_PORT) { input ->
            val url = UrlNormalizer.normalize(input)
            addressBar.setText(url)
            loading = true
            updateRefreshButton()
            webView.loadUrl(url)
            hideControls()
        }.also { it.start() }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_qr_input, null)
        view.findViewById<ImageView>(R.id.qrImage).setImageBitmap(createQrBitmap(content))
        view.findViewById<TextView>(R.id.qrAddress).text = content

        val dialog = AlertDialog.Builder(this)
            .setTitle("LAN input")
            .setView(view)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()
        dialog.setOnDismissListener {
            lanInputServer?.stop()
            lanInputServer = null
        }
        dialog.show()
    }

    private fun showSocks5SettingsDialog() {
        val settings = socks5Store.load()
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_socks5_settings, null)
        val enabled = view.findViewById<CheckBox>(R.id.proxyEnabled)
        val host = view.findViewById<EditText>(R.id.proxyHost)
        val port = view.findViewById<EditText>(R.id.proxyPort)
        val username = view.findViewById<EditText>(R.id.proxyUsername)
        val password = view.findViewById<EditText>(R.id.proxyPassword)
        val proxyDns = view.findViewById<CheckBox>(R.id.proxyDns)

        enabled.isChecked = settings.enabled
        host.setText(settings.host)
        port.setText(settings.port.toString())
        username.setText(settings.username)
        password.setText(settings.password)
        proxyDns.isChecked = settings.proxyDns

        AlertDialog.Builder(this)
            .setTitle("SOCKS5 代理")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val saved = Socks5Settings(
                    enabled = enabled.isChecked,
                    host = host.text.toString().trim(),
                    port = port.text.toString().toIntOrNull() ?: 0,
                    username = username.text.toString(),
                    password = password.text.toString(),
                    proxyDns = proxyDns.isChecked,
                )
                socks5Store.save(saved)
                Toast.makeText(this, if (saved.isUsable()) "代理设置已保存" else "代理已关闭或配置无效", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun findLanIpAddress(): String {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            ?.hostAddress ?: "0.0.0.0"
    }

    private fun createQrBitmap(content: String): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)
        val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
        for (x in 0 until QR_SIZE) {
            for (y in 0 until QR_SIZE) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun showVideoList() {
        val items = videoSniffer.items
        if (items.isEmpty()) {
            Toast.makeText(this, "No video URLs found", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Detected videos")
            .setItems(items.map { "${it.type}: ${it.url}" }.toTypedArray()) { _, index ->
                openInKodi(items[index])
            }
            .show()
    }

    private fun openInKodi(item: VideoItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(item.url), if (item.type == "m3u8") "application/vnd.apple.mpegurl" else "video/*")
            setPackage(KODI_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
        }
    }

    private companion object {
        const val HOME_URL = "https://www.google.com"
        const val KODI_PACKAGE = "org.xbmc.kodi"
        const val LAN_INPUT_PORT = 8787
        const val QR_SIZE = 512
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    }
}

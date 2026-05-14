package com.freekof.tvbrowser

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
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController

class MainActivity : AppCompatActivity() {
    private lateinit var geckoView: GeckoView
    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var geckoSession: GeckoSession
    private lateinit var controlPanel: LinearLayout
    private lateinit var controlScrim: View
    private lateinit var addressBar: EditText
    private lateinit var refreshButton: Button
    private lateinit var proxyStore: HttpProxySettingsStore
    private val videoSniffer = VideoSniffer()
    private var lanInputServer: LanInputServer? = null
    private var loading = false
    private var canGoBack = false
    private var canGoForward = false
    private var currentUrl = HOME_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        geckoView = findViewById(R.id.geckoView)
        controlPanel = findViewById(R.id.controlPanel)
        controlScrim = findViewById(R.id.controlScrim)
        addressBar = findViewById(R.id.addressBar)
        refreshButton = findViewById(R.id.refreshButton)
        proxyStore = HttpProxySettingsStore(this)

        createGeckoSession(proxyStore.load(), HOME_URL)
        setupControls()
        hideSystemUi()
        loadUrl(HOME_URL)
    }

    override fun onDestroy() {
        lanInputServer?.stop()
        geckoSession.close()
        super.onDestroy()
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

    private fun setupControls() {
        geckoView.setOnGenericMotionListener { _, event ->
            if (event.buttonState and MotionEvent.BUTTON_SECONDARY != 0) {
                showControls()
                true
            } else {
                false
            }
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            if (canGoBack) geckoSession.goBack()
        }
        findViewById<Button>(R.id.forwardButton).setOnClickListener {
            if (canGoForward) geckoSession.goForward()
        }
        refreshButton.setOnClickListener {
            if (loading) {
                geckoSession.stop()
                loading = false
                updateRefreshButton()
            } else {
                loading = true
                updateRefreshButton()
                geckoSession.reload()
            }
        }
        findViewById<Button>(R.id.qrButton).setOnClickListener { showQrInputDialog() }
        findViewById<Button>(R.id.proxyButton).setOnClickListener { showProxySettingsDialog() }
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
    }

    private fun createGeckoSession(settings: HttpProxySettings, initialUrl: String) {
        writeGeckoConfig(settings)
        geckoRuntime = GeckoRuntime.create(
            this,
            GeckoRuntimeSettings.Builder()
                .configFilePath(geckoConfigFile().absolutePath)
                .javaScriptEnabled(true)
                .consoleOutput(true)
                .build(),
        )
        geckoSession = newSession(settings)
        geckoSession.open(geckoRuntime)
        geckoView.setSession(geckoSession)
        currentUrl = initialUrl
    }

    private fun recreateGeckoSession(settings: HttpProxySettings) {
        val urlToRestore = currentUrl.takeIf { it.isNotBlank() } ?: HOME_URL
        writeGeckoConfig(settings)
        geckoSession.close()
        geckoSession = newSession(settings)
        geckoSession.open(geckoRuntime)
        geckoView.setSession(geckoSession)
        loadUrl(urlToRestore)
    }

    private fun newSession(settings: HttpProxySettings): GeckoSession {
        return GeckoSession(
            GeckoSessionSettings.Builder()
                .allowJavascript(true)
                .userAgentOverride(UserAgentSettings.effective(settings.userAgent))
                .build(),
        ).also { session ->
            session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, url: String) {
                    loading = true
                    currentUrl = url
                    addressBar.setText(url)
                    updateRefreshButton()
                }

                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    loading = false
                    updateRefreshButton()
                }
            })
            session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean,
                ) {
                    if (!url.isNullOrBlank()) {
                        currentUrl = url
                        addressBar.setText(url)
                    }
                }

                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    this@MainActivity.canGoBack = canGoBack
                }

                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    this@MainActivity.canGoForward = canGoForward
                }
            })
        }
    }

    private fun writeGeckoConfig(settings: HttpProxySettings) {
        geckoConfigFile().writeText(GeckoProxyConfig.yaml(settings), Charsets.UTF_8)
    }

    private fun geckoConfigFile(): File = File(filesDir, GECKO_CONFIG_FILE)

    private fun handleBackKey() {
        when (BackKeyPolicy.decide(controlPanel.visibility == View.VISIBLE, canGoBack)) {
            BackKeyAction.ShowControls -> showControls()
            BackKeyAction.NavigateBack -> {
                hideControls()
                geckoSession.goBack()
            }
            BackKeyAction.Exit -> super.onBackPressed()
        }
    }

    private fun loadFromAddressBar() {
        loadUrl(UrlNormalizer.normalize(addressBar.text.toString()))
        hideControls()
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        addressBar.setText(url)
        loading = true
        updateRefreshButton()
        geckoSession.loadUri(url)
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
        // GeckoView request interception will be restored in a later video-sniffing pass.
    }

    private fun updateRefreshButton() {
        refreshButton.text = if (loading) "S" else "R"
    }

    private fun showQrInputDialog() {
        val content = LanInputEndpoint.qrContent(findLanIpAddress(), LAN_INPUT_PORT)
        lanInputServer?.stop()
        lanInputServer = LanInputServer(LAN_INPUT_PORT) { input ->
            loadUrl(UrlNormalizer.normalize(input))
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

    private fun showProxySettingsDialog() {
        val settings = proxyStore.load()
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_http_proxy_settings, null)
        val enabled = view.findViewById<CheckBox>(R.id.proxyEnabled)
        val host = view.findViewById<EditText>(R.id.proxyHost)
        val port = view.findViewById<EditText>(R.id.proxyPort)
        val username = view.findViewById<EditText>(R.id.proxyUsername)
        val password = view.findViewById<EditText>(R.id.proxyPassword)
        val proxyDns = view.findViewById<CheckBox>(R.id.proxyDns)
        val userAgent = view.findViewById<EditText>(R.id.userAgent)

        enabled.isChecked = settings.enabled
        host.setText(settings.host)
        port.setText(settings.port.toString())
        username.setText(settings.username)
        password.setText(settings.password)
        proxyDns.isChecked = settings.proxyDns
        userAgent.setText(UserAgentSettings.effective(settings.userAgent))
        view.findViewById<Button>(R.id.clearCookiesButton).setOnClickListener { clearCookies() }
        view.findViewById<Button>(R.id.testProxyButton).setOnClickListener {
            testHttpProxy(dialogSettings(enabled, host, port, username, password, proxyDns, userAgent))
        }

        AlertDialog.Builder(this)
            .setTitle("代理和浏览设置")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val saved = dialogSettings(enabled, host, port, username, password, proxyDns, userAgent)
                proxyStore.save(saved)
                recreateGeckoSession(saved)
                val message = if (saved.isUsable()) "代理已保存；如未立即生效，请重启应用" else "代理已关闭，UA 已应用"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dialogSettings(
        enabled: CheckBox,
        host: EditText,
        port: EditText,
        username: EditText,
        password: EditText,
        proxyDns: CheckBox,
        userAgent: EditText,
    ): HttpProxySettings = HttpProxySettings(
        enabled = enabled.isChecked,
        host = host.text.toString().trim(),
        port = port.text.toString().toIntOrNull() ?: 0,
        username = username.text.toString(),
        password = password.text.toString(),
        proxyDns = proxyDns.isChecked,
        userAgent = UserAgentSettings.effective(userAgent.text.toString()),
    )

    private fun clearCookies() {
        geckoRuntime.storageController.clearData(StorageController.ClearFlags.COOKIES).accept(
            { Toast.makeText(this, "Cookies 已清除", Toast.LENGTH_SHORT).show() },
            { Toast.makeText(this, "Cookies 清除失败", Toast.LENGTH_SHORT).show() },
        )
    }

    private fun testHttpProxy(settings: HttpProxySettings) {
        if (!settings.isUsable()) {
            Toast.makeText(this, "HTTP 代理配置无效", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val result = runCatching {
                val client = OkHttpClient.Builder()
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(settings.host, settings.port)))
                    .build()
                val request = Request.Builder().url("https://api.ipify.org").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    response.body?.string()?.trim().orEmpty()
                }
            }
            runOnUiThread {
                val message = result.fold(
                    onSuccess = { "HTTP 代理可用，出口 IP: $it" },
                    onFailure = { "HTTP 代理测试失败：${it.message ?: it.javaClass.simpleName}" },
                )
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }.start()
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
        const val GECKO_CONFIG_FILE = "geckoview-config.yaml"
    }
}

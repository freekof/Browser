package com.freekof.tvbrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var controlPanel: LinearLayout
    private lateinit var addressBar: EditText
    private lateinit var videoButton: Button
    private val videoSniffer = VideoSniffer()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        controlPanel = findViewById(R.id.controlPanel)
        addressBar = findViewById(R.id.addressBar)
        videoButton = findViewById(R.id.videoButton)

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

            override fun onPageFinished(view: WebView, url: String) {
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
        findViewById<Button>(R.id.refreshButton).setOnClickListener { webView.reload() }
        findViewById<Button>(R.id.qrButton).setOnClickListener {
            Toast.makeText(this, "QR phone input will be added next", Toast.LENGTH_SHORT).show()
        }
        videoButton.setOnClickListener { showVideoList() }

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
        webView.loadUrl(url)
        hideControls()
    }

    private fun showControls() {
        controlPanel.visibility = View.VISIBLE
        addressBar.requestFocus()
    }

    private fun hideControls() {
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
        runOnUiThread { videoButton.text = "Video(${videoSniffer.items.size})" }
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
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    }
}

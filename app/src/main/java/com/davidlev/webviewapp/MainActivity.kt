package com.davidlev.webviewapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private var myWebView: WebView? = null
    private var progressBar: ProgressBar? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myWebView = findViewById<View>(R.id.web_view) as WebView
        progressBar = findViewById<View>(R.id.progress_bar) as ProgressBar
        val webSettings = myWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        myWebView!!.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36"
        webSettings.setSupportMultipleWindows(true)
        webSettings.pluginState = WebSettings.PluginState.ON
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webSettings.mediaPlaybackRequiresUserGesture = false
        }
        myWebView!!.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val isCameraApproved =
                        applicationContext.checkSelfPermission("android.permission.CAMERA")
                    if (isCameraApproved == PackageManager.PERMISSION_DENIED) {
                        val permissions = arrayOf(Manifest.permission.CAMERA)
                        requestPermissions(permissions, 100)
                        return
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val isMicApproved =
                        applicationContext.checkSelfPermission("android.permission.RECORD_AUDIO")
                    if (isMicApproved == PackageManager.PERMISSION_DENIED) {
                        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
                        requestPermissions(permissions, 100)
                        return
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.resources)
                }
            }

            override fun onCreateWindow(
                view: WebView,
                dialog: Boolean,
                userGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val newWebView = WebView(this@MainActivity)
                view.addView(newWebView)
                val transport = resultMsg.obj as WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        myWebView!!.loadUrl(url)
                        return true
                    }
                }
                return true
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                callback.onCustomViewHidden()
                super.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {}
        }
        myWebView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                progressBar!!.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d("MainActivity", request.url.toString())
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar!!.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }
        myWebView!!.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, l ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isApproved =
                    applicationContext.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                if (isApproved == PackageManager.PERMISSION_DENIED) {
                    val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permissions, 100)
                    return@DownloadListener
                }
            }
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-agent", userAgent)
            request.setDescription("Download start")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimeType)
            )
            val dm = applicationContext.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Download started", Toast.LENGTH_LONG).show()
        })
        myWebView!!.loadUrl("https://davidlev.me/")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(applicationContext, "grant the permissions", Toast.LENGTH_LONG).show()
        } else if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                applicationContext,
                "permissions granted. now go back and try again.",
                Toast.LENGTH_LONG
            ).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        if (myWebView!!.canGoBack()) {
            myWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
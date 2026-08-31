package com.visualtasker.wss.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private sealed interface BrowserCommand {
    data class Load(val url: String) : BrowserCommand
    data object Reload : BrowserCommand
    data object Back : BrowserCommand
    data object Forward : BrowserCommand
}

class BrowserPanelState(initialUrl: String = "https://example.org") {
    var urlInput by mutableStateOf(initialUrl)
    var currentUrl by mutableStateOf(initialUrl)
    var pageTitle by mutableStateOf("Browser")
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    private var pendingCommand: BrowserCommand? by mutableStateOf(BrowserCommand.Load(initialUrl))
    var bridgeEventsCount by mutableIntStateOf(0)
    val savedLinks = mutableStateListOf(initialUrl)

    fun loadUrl(url: String) {
        urlInput = normalizeUrl(url)
        pendingCommand = BrowserCommand.Load(urlInput)
    }

    fun reload() {
        pendingCommand = BrowserCommand.Reload
    }

    fun goBack() {
        pendingCommand = BrowserCommand.Back
    }

    fun goForward() {
        pendingCommand = BrowserCommand.Forward
    }

    fun addCurrentToSavedLinks() {
        val normalized = normalizeUrl(currentUrl.ifBlank { urlInput })
        if (savedLinks.none { it.equals(normalized, ignoreCase = true) }) {
            savedLinks.add(0, normalized)
        }
    }

    fun loadSavedLink(url: String) {
        loadUrl(url)
    }

    internal fun executePending(webView: WebView) {
        val cmd = pendingCommand
        pendingCommand = null
        when (cmd) {
            is BrowserCommand.Load -> webView.loadUrl(cmd.url)
            BrowserCommand.Reload -> webView.reload()
            BrowserCommand.Back -> if (webView.canGoBack()) webView.goBack()
            BrowserCommand.Forward -> if (webView.canGoForward()) webView.goForward()
            null -> Unit
        }
    }
}

private class DomBridge(private val onEvent: () -> Unit) {
    @JavascriptInterface
    fun postEvent(event: String, payload: String?) {
        // Bridge hook for späteres DOM-Tracking / Java-Injections.
        onEvent()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserPanel(
    state: BrowserPanelState,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                addJavascriptInterface(
                    DomBridge { state.bridgeEventsCount += 1 },
                    "VisualTaskerWssBridge"
                )
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val current = url.orEmpty()
                        if (current.isNotBlank()) {
                            state.currentUrl = current
                            state.urlInput = current
                        }
                        state.canGoBack = view?.canGoBack() == true
                        state.canGoForward = view?.canGoForward() == true
                        state.pageTitle = view?.title ?: "Browser"
                    }
                }
            }
        },
        update = { webView ->
            state.executePending(webView)
            state.canGoBack = webView.canGoBack()
            state.canGoForward = webView.canGoForward()
        }
    )
}

private fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return "https://example.org"
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

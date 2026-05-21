package com.example

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Render immersive edge-to-edge transparent system bars
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Outer web container targeting 100% canvas space
                FullScreenWebView(
                    url = "https://arbisout.netlify.app/",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun FullScreenWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var canGoBack by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Remember our WebView instance so rotation or uiMode shifts do not re-instantiate it
    val webView = remember {
        mutableStateOf<WebView?>(null)
    }

    // Capture standard system back actions and navigate backward in browsing history if available
    val currentWebView = webView.value
    BackHandler(enabled = canGoBack && currentWebView != null) {
        currentWebView?.goBack()
    }

    AndroidView(
        factory = { ctx ->
            // Instantiate the child WebView
            val web = WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Requirement 1: JavaScript Enabled
                settings.javaScriptEnabled = true

                // Requirement 2: DOM Storage Enabled
                settings.domStorageEnabled = true

                // Requirement 3: User Agent - Use a stable standard Mobile User Agent
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                // Requirement 4: Scale & Viewport configurations to scale wide table datasets
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                // Optimal performance and layout loading setups
                settings.databaseEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                webChromeClient = WebChromeClient()
            }

            // Expose the webView object to parent context
            webView.value = web

            // Requirement 7: Pull-to-refresh container
            SwipeRefreshLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Theme styling: Geometric Balance styling accents
                // Brand color matching the Geometric Balance blue indicator (#0061A4)
                setColorSchemeColors(android.graphics.Color.parseColor("#0061A4"))
                // Background of index spinner circular panel is white (#FFFFFF)
                setProgressBackgroundColorSchemeColor(android.graphics.Color.WHITE)

                // Place target WebView inside SwipeRefreshLayout
                addView(web)

                // Pull Refresh listener
                setOnRefreshListener {
                    web.reload()
                }

                // Scroll safety: only allow pull-gestures when WebView is fully scrolled to top
                setOnChildScrollUpCallback { _, _ ->
                    web.scrollY > 0
                }

                // Intercept web page updates and manage swipe refresh visibility dynamically
                web.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, u, favicon)
                        isRefreshing = true
                        canGoBack = view?.canGoBack() ?: false
                    }

                    override fun onPageFinished(view: WebView?, u: String?) {
                        super.onPageFinished(view, u)
                        isRefreshing = false
                        canGoBack = view?.canGoBack() ?: false
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, u: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, u, isReload)
                        canGoBack = view?.canGoBack() ?: false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        // Keep loading internally within this full screen viewport
                        return false
                    }
                }
            }
        },
        update = { swipeLayout ->
            val web = webView.value
            if (web != null && web.url == null) {
                web.loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

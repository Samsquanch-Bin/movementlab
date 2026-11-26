package com.movementlab.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.movementlab.app.data.Seed
import com.movementlab.app.data.Videos

private fun embedHtml(videoId: String) = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>
  html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
  iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
</style>
</head>
<body>
<iframe
  src="https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1"
  title="Demo video"
  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
  referrerpolicy="strict-origin-when-cross-origin"
  allowfullscreen></iframe>
</body>
</html>
""".trimIndent()

/**
 * Plays an exercise's demo video inside the app.
 *
 * YouTube links play in an embedded player (with fullscreen support). Anything
 * else, such as a search link or a non-YouTube site, shows a button that opens
 * it in the browser instead.
 */
@Composable
fun VideoPlayer(url: String, exerciseName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val videoId = remember(url) { Videos.youtubeId(url) }

    Column(modifier.fillMaxWidth()) {
        if (videoId == null) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = { openUrl(context, url.ifBlank { Seed.youtubeSearch(exerciseName) }) }) {
                    Icon(Icons.Filled.Search, null)
                    Text("  Find a demo on YouTube")
                }
            }
            Text(
                "Tip: copy a video's link in YouTube, then tap the pencil to make it play here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Color.Black),
            ) {
                YouTubeWebView(videoId)
            }
            TextButton(onClick = { openUrl(context, Videos.watchUrl(videoId)) }) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(" Open in YouTube")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebView(videoId: String) {
    val context = LocalContext.current
    val activity = context as? Activity

    val fullscreen = remember { FullscreenHolder(activity) }

    val webView = remember(videoId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(AndroidColor.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = object : WebViewClient() {
                // Taps on the YouTube logo/title try to leave the embed; send those to the YouTube app.
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val target = request.url.toString()
                    val isEmbed = target.contains("/embed/") || target.startsWith("https://com.movementlab.app")
                    if (request.isForMainFrame && !isEmbed) {
                        openUrl(context, target)
                        return true
                    }
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) = fullscreen.show(view, callback)
                override fun onHideCustomView() = fullscreen.hide()
            }

            // A real https base URL gives the embed a Referer, which YouTube requires.
            loadDataWithBaseURL("https://com.movementlab.app/", embedHtml(videoId), "text/html", "utf-8", null)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            fullscreen.hide()
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
}

/** Shows the player's fullscreen view over the whole app, in landscape. */
private class FullscreenHolder(private val activity: Activity?) {
    private var view: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null

    fun show(v: View, cb: WebChromeClient.CustomViewCallback) {
        val decor = activity?.window?.decorView as? FrameLayout
        if (decor == null) {
            cb.onCustomViewHidden()
            return
        }
        hide()
        view = v
        callback = cb
        v.setBackgroundColor(AndroidColor.BLACK)
        decor.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun hide() {
        val v = view ?: return
        (activity?.window?.decorView as? FrameLayout)?.removeView(v)
        view = null
        callback?.onCustomViewHidden()
        callback = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

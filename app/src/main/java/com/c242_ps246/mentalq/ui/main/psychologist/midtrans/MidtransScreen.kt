package com.c242_ps246.mentalq.ui.main.psychologist.midtrans

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.R

@Composable
fun MidtransScreen(
    orderId: String,
    onSuccess: (String) -> Unit,
    onFailed: () -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel: MidtransViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transactionStatus by viewModel.transactionStatus.collectAsStateWithLifecycle()
    val chatId by viewModel.chatId.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadPaymentResult(orderId)
    }

    LaunchedEffect(orderId, transactionStatus, chatId) {
        if (transactionStatus in SUCCESSFUL_STATUSES) {
            chatId?.takeIf(String::isNotBlank)?.let(onSuccess)
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.error != null -> PaymentMessage(
                title = if (transactionStatus in SUCCESSFUL_STATUSES) {
                    "Payment confirmed"
                } else {
                    stringResource(R.string.transaction_failed)
                },
                message = uiState.error.orEmpty(),
                buttonText = if (transactionStatus in SUCCESSFUL_STATUSES) {
                    "Check again"
                } else {
                    stringResource(R.string.back)
                },
                onClick = if (transactionStatus in SUCCESSFUL_STATUSES) {
                    { viewModel.loadPaymentResult(orderId) }
                } else {
                    onFailed
                }
            )
            transactionStatus in SUCCESSFUL_STATUSES -> CircularProgressIndicator()
            transactionStatus == null || transactionStatus in PENDING_STATUSES -> PaymentMessage(
                title = "Payment is being processed",
                message = "If you completed the payment, check its status again.",
                buttonText = "Check again",
                onClick = { viewModel.loadPaymentResult(orderId) }
            )
            else -> PaymentMessage(
                title = stringResource(R.string.transaction_failed),
                message = stringResource(R.string.transaction_failed_desc),
                buttonText = stringResource(R.string.back),
                onClick = {
                    viewModel.cancelTransaction(orderId, onFailed)
                }
            )
        }
    }
}

@Composable
private fun PaymentMessage(
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MidtransWebView(
    onBackClick: (String?) -> Unit,
    itemId: String
) {
    val viewModel: MidtransViewModel = hiltViewModel()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val orderId by viewModel.orderId.collectAsStateWithLifecycle()
    val redirectUrl by viewModel.redirectUrl.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(itemId) {
        if (orderId == null) viewModel.createTransaction(itemId)
    }

    BackHandler(enabled = !uiState.isLoading) { onBackClick(orderId) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val paymentUrl = redirectUrl
        when {
            uiState.isLoading -> CircularProgressIndicator()
            paymentUrl == null -> PaymentMessage(
                title = stringResource(R.string.transaction_failed),
                message = uiState.error ?: "The payment page could not be opened.",
                buttonText = stringResource(R.string.back),
                onClick = { onBackClick(orderId) }
            )
            else -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        webView = this
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.domStorageEnabled = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val uri = request.url ?: return false
                                if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                                    return false
                                }
                                return openExternalApp(uri)
                            }

                            private fun openExternalApp(uri: Uri): Boolean = try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                true
                            } catch (_: Exception) {
                                true
                            }
                        }
                        loadUrl(paymentUrl)
                    }
                },
                update = { currentWebView ->
                    if (currentWebView.url.isNullOrBlank()) currentWebView.loadUrl(paymentUrl)
                }
            )
        }
    }
}

private val SUCCESSFUL_STATUSES = setOf("settlement", "capture")
private val PENDING_STATUSES = setOf("pending", "authorize")

package com.ethiogpt.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int FILE_CHOOSER_REQUEST = 2001;

    private WebView webView;

    private ValueCallback<Uri[]> filePathCallback;

    private final String ETHIOGPT_URL = BuildConfig.ETHIOGPT_URL;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        configureWebView();

        requestRequiredPermissions();

        if (savedInstanceState == null) {
            webView.loadUrl(ETHIOGPT_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        setupBackButton();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // DOM storage for localStorage used by EthioGPT
        settings.setDomStorageEnabled(true);

        // Database/storage
        settings.setDatabaseEnabled(true);

        // Responsive viewport
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Zoom
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // File access
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Safe browsing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        // Keep navigation inside the app for the EthioGPT domain.
        webView.setWebViewClient(new EthioGPTWebViewClient());

        // Handle JavaScript dialogs, permissions, uploads, etc.
        webView.setWebChromeClient(new EthioGPTWebChromeClient());

        // Keep the app edge-to-edge friendly.
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Improve performance.
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void requestRequiredPermissions() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        java.util.ArrayList<String> permissions =
                new java.util.ArrayList<>();

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {

            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[0]),
                    REQUEST_PERMISSIONS
            );
        }
    }

    private void setupBackButton() {

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (webView != null && webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();

            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_PERMISSIONS) {

            boolean microphoneGranted = false;

            for (int i = 0; i < permissions.length; i++) {

                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {

                    microphoneGranted =
                            grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            if (!microphoneGranted) {

                Toast.makeText(
                        this,
                        "Microphone permission is needed for voice input.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private class EthioGPTWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
        ) {

            Uri uri = request.getUrl();

            if (uri == null) {
                return false;
            }

            String scheme = uri.getScheme();

            if ("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme)) {

                view.loadUrl(uri.toString());

                return true;
            }

            try {

                Intent intent =
                        new Intent(Intent.ACTION_VIEW, uri);

                startActivity(intent);

            } catch (Exception ignored) {
            }

            return true;
        }

        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view,
                String url
        ) {

            if (url == null) {
                return false;
            }

            if (url.startsWith("http://")
                    || url.startsWith("https://")) {

                view.loadUrl(url);

                return true;
            }

            try {

                Intent intent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                startActivity(intent);

            } catch (Exception ignored) {
            }

            return true;
        }

        @Override
        public void onPageStarted(
                WebView view,
                String url,
                Bitmap favicon
        ) {

            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(
                WebView view,
                String url
        ) {

            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
        ) {

            super.onReceivedError(view, request, error);
        }
    }

    private class EthioGPTWebChromeClient extends WebChromeClient {

        @Override
        public void onPermissionRequest(
                final PermissionRequest request
        ) {

            runOnUiThread(() -> {

                if (request == null) {
                    return;
                }

                String[] resources = request.getResources();

                java.util.ArrayList<String> allowed =
                        new java.util.ArrayList<>();

                for (String resource : resources) {

                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {

                        if (ContextCompat.checkSelfPermission(
                                MainActivity.this,
                                Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED) {

                            allowed.add(resource);
                        }
                    }

                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {

                        if (ContextCompat.checkSelfPermission(
                                MainActivity.this,
                                Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED) {

                            allowed.add(resource);
                        }
                    }
                }

                if (!allowed.isEmpty()) {

                    request.grant(
                            allowed.toArray(new String[0])
                    );

                } else {

                    request.deny();
                }
            });
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(
                String origin,
                GeolocationPermissions.Callback callback
        ) {

            callback.invoke(origin, true, false);
        }

        @Override
        public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
        ) {

            if (MainActivity.this.filePathCallback != null) {

                MainActivity.this.filePathCallback.onReceiveValue(null);
            }

            MainActivity.this.filePathCallback =
                    filePathCallback;

            Intent intent =
                    fileChooserParams.createIntent();

            try {

                startActivityForResult(
                        intent,
                        FILE_CHOOSER_REQUEST
                );

            } catch (Exception e) {

                MainActivity.this.filePathCallback = null;

                Toast.makeText(
                        MainActivity.this,
                        "Unable to open file picker.",
                        Toast.LENGTH_SHORT
                ).show();

                return false;
            }

            return true;
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != FILE_CHOOSER_REQUEST) {
            return;
        }

        if (filePathCallback == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == Activity.RESULT_OK
                && data != null) {

            Uri dataUri = data.getData();

            if (dataUri != null) {

                results = new Uri[]{dataUri};

            } else {

                android.os.Parcelable[] clipData =
                        data.getClipData() != null
                                ? getClipDataUris(data)
                                : null;

                if (clipData != null) {

                    results = new Uri[clipData.length];

                    for (int i = 0; i < clipData.length; i++) {

                        results[i] =
                                (Uri) clipData[i];
                    }
                }
            }
        }

        filePathCallback.onReceiveValue(results);

        filePathCallback = null;
    }

    private android.os.Parcelable[] getClipDataUris(Intent data) {

        int count = data.getClipData().getItemCount();

        android.os.Parcelable[] results =
                new android.os.Parcelable[count];

        for (int i = 0; i < count; i++) {

            results[i] =
                    data.getClipData().getItemAt(i).getUri();
        }

        return results;
    }
}

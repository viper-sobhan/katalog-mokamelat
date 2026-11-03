package com.example.katalogmokamelat;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingBackupJson;

    private static final int FILE_CHOOSER_REQUEST_CODE = 100;
    private static final int BACKUP_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void saveBackup(String json) {
                runOnUiThread(() -> {
                    pendingBackupJson = json;

                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "backup-catalog-mokamelat.json");

                    try {
                        startActivityForResult(intent, BACKUP_REQUEST_CODE);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "خطا در ایجاد فایل پشتیبان",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;
                Intent intent = params.createIntent();

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }

                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BACKUP_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try {
                    Uri uri = data.getData();
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);

                    if (outputStream != null) {
                        outputStream.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
                        outputStream.close();

                        Toast.makeText(this,
                                "فایل پشتیبان ذخیره شد",
Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this,
                            "ذخیره فایل ناموفق بود",
                            Toast.LENGTH_LONG).show();
                }
            }

            pendingBackupJson = null;
        }

        if (requestCode == FILE_CHOOSER_REQUEST_CODE && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

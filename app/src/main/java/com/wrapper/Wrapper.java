package com.wrapper;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class Wrapper extends Activity
{
    final Activity activity = this;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.clearCache(true);
        webView.clearHistory();

        if (Build.VERSION.SDK_INT >= 21) {
            webView.getSettings().setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }


        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress)
            {
                activity.setTitle("Loading...");
                activity.setProgress(progress * 100);

                if(progress == 100)
                    activity.setTitle(R.string.app_name);
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Ignore SSL certificate errors
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
                // Handle the error
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if (url.endsWith(".mp3")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url), "audio/*");
                }else
                    view.loadUrl(url);
                return true;
            }
        });

        webView.loadUrl("http://www.claroideias.com");

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url,
                                        String userAgent,
                                        String contentDisposition,
                                        String mimetype,
                                        long contentLength) {
                if(Build.VERSION.SDK_INT >=23){
                    Context nContext = Wrapper.this.getApplicationContext();
                    if(nContext.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        Wrapper.this.requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                }

                String fileName = "";
                url = url.replace(" ", "%20");

                AsyncTask<String, Integer, String> asyncTask = new getFileNameAsync();
                asyncTask.execute(url);

                try {
                    fileName = asyncTask.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (CancellationException e) {
                    e.printStackTrace();
                }
                if ((fileName == null) || (fileName.hashCode() == "".hashCode())) {
                    fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                }

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("Cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading File");
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    downloadManager.enqueue(request);
                }
                Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
            }
        });

    }
}
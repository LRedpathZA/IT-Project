package com.example.splashscreen; // Ensure this matches your project package

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class BubblesFragment extends Fragment implements HeaderUpdatable {

    private static final String BUBBLES_URL = "https://splashscreen-20z.pages.dev/bubbles";
    private WebView webView;
    private LinearLayout errorLayout; // The placeholder/error view

    public BubblesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.bubbles, container, false);
        WebView.setWebContentsDebuggingEnabled(true);
        webView = view.findViewById(R.id.webview_bubbles);
        errorLayout = view.findViewById(R.id.layout_error_placeholder);

        setupWebView();

        return view;
    }
    @Override
    public void updateActivityHeader() {
        if (getActivity() instanceof MainActivity) {
            String title =  "Bubbles AI";
            ((MainActivity) getActivity()).updateHeader(title, true, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateActivityHeader();
    }
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new CustomWebViewClient());
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.loadUrl(BUBBLES_URL);
    }

    private void showWebView() {
        webView.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
    }

    private void showErrorPlaceholder() {
        webView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
    }
    private class CustomWebViewClient extends WebViewClient {

        private boolean loadingError = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // Assume loading is fine initially
            loadingError = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // If no errors occurred during loading, show the webview
            if (!loadingError) {
                showWebView();
            } else {
                // If an error was recorded, show the placeholder
                showErrorPlaceholder();
            }
        }

        // For API 23 and above (Recommended for general errors)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                loadingError = true;
                // Optional: Show a toast with the error description
                Toast.makeText(getContext(), "Error loading page: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }
        }

        // For older APIs (deprecated but good for backward compatibility)
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (failingUrl.equals(BUBBLES_URL)) {
                loadingError = true;
            }
        }
    }
}
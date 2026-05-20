package com.browser.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import java.util.ArrayList;
import java.util.List;

public class BrowserActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";

    private LinearLayout tabBar;
    private HorizontalScrollView tabScroll;
    private FrameLayout webContainer;

    private static class Tab {
        WebView webView;
        String title;
        LinearLayout chip;
    }

    private final List<Tab> tabs = new ArrayList<>();
    private int activeTabIndex = -1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FullScreen.enable(getWindow());

        setContentView(R.layout.activity_browser);
        tabBar = findViewById(R.id.tab_bar);
        tabScroll = findViewById(R.id.tab_scroll);
        webContainer = findViewById(R.id.web_container);

        CookieManager.getInstance().setAcceptCookie(true);

        findViewById(R.id.home_button).setOnClickListener(v -> finish());

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url != null) {
            openTab(url);
        } else {
            finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openTab(String url) {
        Tab tab = new Tab();
        tab.title = url;
        tab.webView = new WebView(this);

        configureSettings(tab.webView);
        CookieManager.getInstance().setAcceptThirdPartyCookies(tab.webView, true);

        final int tabIndex = tabs.size();
        tabs.add(tab);

        // Add WebView to container (invisible until switched to)
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        tab.webView.setLayoutParams(lp);
        tab.webView.setVisibility(View.INVISIBLE);
        webContainer.addView(tab.webView);

        setupWebClients(tab);

        addTabChip(tab, tabIndex);
        switchTab(tabIndex);

        tab.webView.loadUrl(url);
    }

    private void setupWebClients(Tab tab) {
        tab.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                tab.title = title;
                updateChipTitle(tab);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                // Create a new tab for the popup; extract its URL via a bridge WebView
                WebView bridge = new WebView(BrowserActivity.this);
                bridge.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView v, String url, Bitmap favicon) {
                        if (url != null && !url.equals("about:blank")) {
                            bridge.stopLoading();
                            openTab(url);
                        }
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(bridge);
                resultMsg.sendToTarget();
                return true;
            }
        });

        tab.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (host != null && (host.equals("accounts.google.com") || host.endsWith(".accounts.google.com"))) {
                    try {
                        new CustomTabsIntent.Builder().build().launchUrl(BrowserActivity.this, Uri.parse(url));
                        return true;
                    } catch (Exception ignored) {
                        // No browser available — load in WebView
                    }
                }
                if (Allowlist.isAllowedNavigation(url)) return false;
                if (Allowlist.isAllowedResourceHost(host)) return false;
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                FullScreen.enable(getWindow());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // After 2FA challenge, force-navigate to aistudio if still stuck after 4s
                if (url != null && url.contains("accounts.google.com") && url.contains("challenge")) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        String current = view.getUrl();
                        if (current != null && current.contains("accounts.google.com") && current.contains("challenge")) {
                            view.loadUrl("https://aistudio.google.com/");
                        }
                    }, 4000);
                }
            }
        });
    }

    private void addTabChip(Tab tab, int index) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setBackgroundColor(0xFF111827);
        chip.setPadding(20, 0, 12, 0);

        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        chipParams.setMargins(4, 6, 4, 6);
        chip.setLayoutParams(chipParams);

        TextView title = new TextView(this);
        title.setTextColor(0xFFD1D5DB);
        title.setTextSize(13f);
        title.setMaxWidth(dpToPx(140));
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setText(tab.title);
        title.setPadding(0, 0, 12, 0);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        title.setLayoutParams(titleParams);
        chip.addView(title);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(0xFF9CA3AF);
        close.setTextSize(12f);
        close.setPadding(4, 0, 4, 0);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        close.setLayoutParams(closeParams);
        chip.addView(close);

        tab.chip = chip;

        chip.setOnClickListener(v -> switchTab(tabs.indexOf(tab)));
        close.setOnClickListener(v -> closeTab(tabs.indexOf(tab)));

        tabBar.addView(chip);
    }

    private void switchTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            t.webView.setVisibility(i == index ? View.VISIBLE : View.INVISIBLE);
            if (t.chip != null) {
                t.chip.setBackgroundColor(i == index ? 0xFF1F2937 : 0xFF111827);
            }
        }
        activeTabIndex = index;

        // Scroll chip into view
        final Tab active = tabs.get(index);
        tabScroll.post(() -> {
            if (active.chip != null) {
                int x = active.chip.getLeft();
                tabScroll.smoothScrollTo(x, 0);
            }
        });
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        Tab tab = tabs.get(index);
        tab.webView.destroy();
        webContainer.removeView(tab.webView);
        tabBar.removeView(tab.chip);
        tabs.remove(index);

        if (tabs.isEmpty()) {
            finish();
            return;
        }

        int nextIndex = Math.min(index, tabs.size() - 1);
        switchTab(nextIndex);
    }

    private void updateChipTitle(Tab tab) {
        if (tab.chip == null) return;
        TextView title = (TextView) ((LinearLayout) tab.chip).getChildAt(0);
        title.setText(tab.title);
    }

    @Override
    public void onBackPressed() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            WebView current = tabs.get(activeTabIndex).webView;
            if (current.canGoBack()) {
                current.goBack();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Tab t : tabs) t.webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Tab t : tabs) t.webView.onResume();
        FullScreen.enable(getWindow());
        // Reload after returning from Chrome Custom Tabs login
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            WebView current = tabs.get(activeTabIndex).webView;
            if (current.getUrl() != null && current.getUrl().contains("aistudio.google.com")) {
                current.reload();
            }
        }
    }

    @Override
    protected void onDestroy() {
        for (Tab t : tabs) t.webView.destroy();
        tabs.clear();
        super.onDestroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);
        s.setGeolocationEnabled(false);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

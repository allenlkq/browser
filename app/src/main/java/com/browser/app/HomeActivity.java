package com.browser.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout container;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        container = findViewById(R.id.sites_container);
        statusText = findViewById(R.id.status_text);

        findViewById(R.id.refresh_button).setOnClickListener(v -> loadData());

        loadData();
    }

    private void loadData() {
        statusText.setText("Loading…");
        statusText.setVisibility(View.VISIBLE);
        container.removeAllViews();

        Handler mainHandler = new Handler(Looper.getMainLooper());
        final List<String>[] allowlistResult = new List[1];
        final List<String>[] bookmarksResult = new List[1];
        final int[] doneCount = {0};

        Runnable checkDone = () -> {
            doneCount[0]++;
            if (doneCount[0] == 2) {
                statusText.setVisibility(View.GONE);
                if (allowlistResult[0] != null && !allowlistResult[0].isEmpty()) {
                    List<String> visible = new java.util.ArrayList<>();
                    for (String h : allowlistResult[0]) {
                        if (!Allowlist.isHidden(h)) visible.add(h);
                    }
                    if (!visible.isEmpty()) populateSection("Sites", visible, false);
                } else {
                    statusText.setText("Failed to load allowlist. Check your connection.");
                    statusText.setVisibility(View.VISIBLE);
                }
                if (bookmarksResult[0] != null && !bookmarksResult[0].isEmpty()) {
                    populateSection("Bookmarks", bookmarksResult[0], true);
                }
            }
        };

        Allowlist.fetch(mainHandler, (urls, error) -> {
            allowlistResult[0] = urls;
            checkDone.run();
        });

        Bookmarks.fetch(mainHandler, (urls, error) -> {
            bookmarksResult[0] = urls;
            checkDone.run();
        });
    }

    private void populateSection(String title, List<String> items, boolean fullUrl) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextSize(13f);
        header.setTextColor(0xFF9CA3AF);
        header.setAllCaps(true);
        header.setPadding(0, fullUrl ? 48 : 0, 0, 16);
        container.addView(header);

        for (String item : items) {
            TextView tv = new TextView(this);
            String label = fullUrl ? item.replaceAll("^https?://", "").replaceAll("/.*$", "") + "\n" + item : item;
            tv.setText(fullUrl ? item.replaceAll("^https?://", "") : item);
            tv.setTextSize(18f);
            tv.setTextColor(0xFFFFFFFF);
            tv.setPadding(48, 36, 48, 36);
            tv.setBackgroundResource(R.drawable.site_item_bg);
            tv.setClickable(true);
            tv.setFocusable(true);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 24);
            tv.setLayoutParams(params);

            final String targetUrl = fullUrl ? item : "https://" + item + "/";
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, BrowserActivity.class);
                intent.putExtra(BrowserActivity.EXTRA_URL, targetUrl);
                startActivity(intent);
            });

            container.addView(tv);
        }
    }
}

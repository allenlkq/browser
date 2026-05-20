package com.browser.app;

import android.os.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Bookmarks {

    private static final String BOOKMARKS_URL =
        "https://raw.githubusercontent.com/allenlkq/browser/main/bookmarks";

    public interface FetchCallback {
        void onDone(List<String> urls, Exception error);
    }

    public static void fetch(Handler mainHandler, FetchCallback callback) {
        new Thread(() -> {
            List<String> urls = new ArrayList<>();
            Exception fetchError = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(BOOKMARKS_URL).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                urls.add(line);
                            }
                        }
                    }
                } else {
                    fetchError = new Exception("HTTP " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                fetchError = e;
            }
            final List<String> result = urls;
            final Exception err = fetchError;
            mainHandler.post(() -> callback.onDone(result, err));
        }).start();
    }
}

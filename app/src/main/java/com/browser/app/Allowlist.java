package com.browser.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Allowlist {

    private static final String ALLOWLIST_URL =
        "https://raw.githubusercontent.com/allenlkq/browser/main/allowlist";

    // Populated at runtime — bare hostnames, e.g. "aistudio.google.com"
    private static final CopyOnWriteArrayList<String> ALLOWED_HOSTS = new CopyOnWriteArrayList<>();

    // Static supporting domains always permitted as sub-resources
    private static final List<String> STATIC_RESOURCE_HOSTS = Arrays.asList(
        "google.com",
        "accounts.google.com",
        "googleapis.com",
        "googleusercontent.com",
        "gstatic.com",
        "run.app",
        "raw.githubusercontent.com",
        "canvaslms.com",
        "instructure.com"
    );

    public interface FetchCallback {
        void onDone(List<String> hosts, Exception error);
    }

    public static void fetch(android.os.Handler mainHandler, FetchCallback callback) {
        new Thread(() -> {
            List<String> hosts = new ArrayList<>();
            Exception fetchError = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(ALLOWLIST_URL).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Strip scheme if someone accidentally includes it
                            line = line.trim().replaceAll("^https?://", "").replaceAll("/.*$", "");
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                hosts.add(line);
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

            if (!hosts.isEmpty()) {
                ALLOWED_HOSTS.clear();
                ALLOWED_HOSTS.addAll(hosts);
            }

            final List<String> result = Collections.unmodifiableList(new ArrayList<>(ALLOWED_HOSTS));
            final Exception err = fetchError;
            mainHandler.post(() -> callback.onDone(result, err));
        }).start();
    }

    public static List<String> getAllowedHosts() {
        return Collections.unmodifiableList(new ArrayList<>(ALLOWED_HOSTS));
    }

    /** True if the URL's host matches an allowlisted host or its subdomain. */
    public static boolean isAllowedNavigation(String url) {
        if (url == null) return false;
        String host = extractHost(url);
        return isAllowedHost(host, ALLOWED_HOSTS);
    }

    /** True if the host may load sub-resources (primary + static supporting domains). */
    public static boolean isAllowedResourceHost(String host) {
        if (host == null) return false;
        return isAllowedHost(host, ALLOWED_HOSTS) || isAllowedHost(host, STATIC_RESOURCE_HOSTS);
    }

    private static boolean isAllowedHost(String host, List<String> list) {
        if (host == null) return false;
        for (String allowed : list) {
            if (host.equals(allowed) || host.endsWith("." + allowed)) return true;
        }
        return false;
    }

    public static String extractHost(String url) {
        if (url == null) return null;
        try {
            String s = url;
            int schemeEnd = s.indexOf("://");
            if (schemeEnd >= 0) s = s.substring(schemeEnd + 3);
            int slash = s.indexOf('/');
            if (slash >= 0) s = s.substring(0, slash);
            int colon = s.indexOf(':');
            if (colon >= 0) s = s.substring(0, colon);
            return s;
        } catch (Exception e) {
            return null;
        }
    }
}

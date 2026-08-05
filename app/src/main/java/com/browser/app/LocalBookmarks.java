package com.browser.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class LocalBookmarks {

    private static final String PREFS = "local_bookmarks";
    private static final String KEY = "urls";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<String> get(Context ctx) {
        List<String> urls = new ArrayList<>();
        String blob = prefs(ctx).getString(KEY, "");
        for (String line : blob.split("\n")) {
            line = line.trim();
            if (!line.isEmpty()) urls.add(line);
        }
        return urls;
    }

    public static boolean add(Context ctx, String url) {
        if (url == null) return false;
        url = url.trim();
        if (url.isEmpty()) return false;
        List<String> urls = get(ctx);
        if (urls.contains(url)) return false;
        urls.add(url);
        save(ctx, urls);
        return true;
    }

    public static void remove(Context ctx, String url) {
        if (url == null) return;
        List<String> urls = get(ctx);
        if (urls.remove(url.trim())) save(ctx, urls);
    }

    public static boolean update(Context ctx, String oldUrl, String newUrl) {
        if (oldUrl == null || newUrl == null) return false;
        newUrl = newUrl.trim();
        if (newUrl.isEmpty()) return false;
        List<String> urls = get(ctx);
        int i = urls.indexOf(oldUrl.trim());
        if (i < 0) return false;
        if (urls.contains(newUrl) && !newUrl.equals(oldUrl.trim())) return false;
        urls.set(i, newUrl);
        save(ctx, urls);
        return true;
    }

    private static void save(Context ctx, List<String> urls) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(urls.get(i));
        }
        prefs(ctx).edit().putString(KEY, sb.toString()).apply();
    }
}

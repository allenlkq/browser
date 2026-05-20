# -keep rules for WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}

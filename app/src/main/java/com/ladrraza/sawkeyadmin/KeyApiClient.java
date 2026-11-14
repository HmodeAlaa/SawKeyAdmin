package com.ladrraza.sawkeyadmin;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyApiClient {

    public interface Callback<T> {
        void onResult(boolean ok, T data, String error);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void request(String baseUrl, String adminSecret, String action,
                               Map<String, String> params, Callback<JSONObject> cb) {
        EXEC.execute(() -> {
            try {
                StringBuilder urlStr = new StringBuilder(baseUrl);
                urlStr.append(baseUrl.contains("?") ? "&" : "?");
                urlStr.append("action=").append(URLEncoder.encode(action, "UTF-8"));
                urlStr.append("&admin_secret=").append(URLEncoder.encode(adminSecret, "UTF-8"));
                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        urlStr.append("&")
                              .append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                              .append("=")
                              .append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), "UTF-8"));
                    }
                }

                String body = fetchWithRedirects(urlStr.toString(), 5);
                JSONObject json = new JSONObject(body);
                boolean ok = json.optBoolean("ok", false);
                String err = json.optString("error", "");
                MAIN.post(() -> cb.onResult(ok, json, err));
            } catch (Exception e) {
                MAIN.post(() -> cb.onResult(false, null, e.getMessage() != null ? e.getMessage() : "NETWORK_ERROR"));
            }
        });
    }

    public static List<KeyItem> parseKeys(JSONObject json) {
        List<KeyItem> list = new ArrayList<>();
        if (json == null) return list;
        JSONArray arr = json.optJSONArray("keys");
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj == null) continue;
            list.add(new KeyItem(
                    obj.optString("code", ""),
                    obj.optString("expiry", ""),
                    obj.optString("status", "active"),
                    obj.optString("hwid", ""),
                    obj.optString("note", "")
            ));
        }
        return list;
    }

    private static String fetchWithRedirects(String targetUrl, int maxRedirects) throws Exception {
        String current = targetUrl;
        for (int i = 0; i < maxRedirects; i++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null || loc.isEmpty()) throw new Exception("Empty redirect");
                current = loc;
                continue;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();
            return sb.toString();
        }
        throw new Exception("Too many redirects");
    }
}

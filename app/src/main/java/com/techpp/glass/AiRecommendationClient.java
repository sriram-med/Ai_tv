package com.techpp.glass;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTPS-only GPT recommendation client; never stores or transmits the OpenAI API key. */
public final class AiRecommendationClient {
    public interface Callback {
        void onSuccess(JSONArray recommendations);
        void onError(String reason);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void recommend(String gatewayUrl, String deviceToken, String query,
                          JSONArray verifiedCatalog, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(gatewayUrl.replaceAll("/+$", "") + "/v1/recommend");
                if (!"https".equalsIgnoreCase(url.getProtocol()))
                    throw new IllegalArgumentException("HTTPS required");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(16000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + deviceToken);
                byte[] body = new JSONObject().put("query", query)
                        .put("catalog", verifiedCatalog).toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(body); }
                if (connection.getResponseCode() != 200) throw new IllegalStateException("Gateway unavailable");
                try (InputStream in = connection.getInputStream();
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[4096];
                    int n;
                    while ((n = in.read(chunk)) != -1) {
                        if (buffer.size() + n > 65536) throw new IllegalStateException("Response too large");
                        buffer.write(chunk, 0, n);
                    }
                    JSONArray recommendations = new JSONObject(buffer.toString("UTF-8"))
                            .getJSONArray("recommendations");
                    callback.onSuccess(recommendations);
                }
            } catch (Exception e) {
                callback.onError("AI suggestions unavailable");
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}

package com.skald.app.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * DeepSeek API 客户端（OpenAI 兼容接口）
 */
public class DeepSeekClient {

    private static final String TAG = "DeepSeekClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final int CONNECT_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 60;
    private static final int WRITE_TIMEOUT = 30;

    private final OkHttpClient client;
    private final Handler mainHandler;

    public DeepSeekClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 查询请求参数
     */
    public static class QueryRequest {
        public final String apiUrl;
        public final String apiKey;
        public final String model;
        public final String systemPrompt;
        public final String userMessage;
        public final float temperature;
        public final int maxTokens;

        public QueryRequest(String apiUrl, String apiKey, String model,
                            String systemPrompt, String userMessage) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.userMessage = userMessage;
            this.temperature = 0.8f;
            this.maxTokens = 1024;
        }

        /** 构建完整的 URL 端点 */
        public String getEndpoint() {
            String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
            // DeepSeek 的 OpenAI 兼容端点
            if (base.endsWith("/v1")) {
                return base + "/chat/completions";
            }
            return base + "/v1/chat/completions";
        }
    }

    /**
     * API 响应回调
     */
    public interface Callback {
        void onSuccess(String rawResponse);
        void onFailure(int statusCode, String errorMessage);
    }

    /**
     * 发送查询请求
     */
    public void query(QueryRequest request, Callback callback) {
        try {
            JSONObject json = buildRequestBody(request);
            String body = json.toString();
            Log.d(TAG, "Request body length: " + body.length());

            Request httpRequest = new Request.Builder()
                    .url(request.getEndpoint())
                    .addHeader("Authorization", "Bearer " + request.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON))
                    .build();

            client.newCall(httpRequest).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "HTTP request failed: " + e.getMessage());
                    mainHandler.post(() ->
                            callback.onFailure(-1, "网络请求失败: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int code = response.code();
                    String responseBody = response.body() != null
                            ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        try {
                            String content = parseResponse(responseBody);
                            mainHandler.post(() -> callback.onSuccess(content));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse response: " + e.getMessage());
                            mainHandler.post(() ->
                                    callback.onFailure(-2, "响应解析失败: " + e.getMessage()));
                        }
                    } else {
                        Log.e(TAG, "API error " + code + ": " + responseBody);
                        mainHandler.post(() ->
                                callback.onFailure(code, "API 返回错误 (" + code + "): " + responseBody));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to build request: " + e.getMessage());
            mainHandler.post(() ->
                    callback.onFailure(-1, "请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 同步查询（用于测试连接）
     */
    public String querySync(QueryRequest request) throws Exception {
        String json = buildRequestBody(request).toString();

        Request httpRequest = new Request.Builder()
                .url(request.getEndpoint())
                .addHeader("Authorization", "Bearer " + request.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                return parseResponse(body);
            } else {
                throw new IOException("API error: " + response.code() + " "
                        + (response.body() != null ? response.body().string() : ""));
            }
        }
    }

    /**
     * 构建 OpenAI-compatible JSON 请求体
     */
    private JSONObject buildRequestBody(QueryRequest req) throws Exception {
        JSONArray messages = new JSONArray();

        // System prompt
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", req.systemPrompt);
        messages.put(systemMsg);

        // User message
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", req.userMessage);
        messages.put(userMsg);

        JSONObject body = new JSONObject();
        body.put("model", req.model);
        body.put("messages", messages);
        body.put("temperature", req.temperature);
        body.put("max_tokens", req.maxTokens);

        return body;
    }

    /**
     * 解析 OpenAI-compatible JSON 响应
     * 提取 choices[0].message.content
     */
    private String parseResponse(String responseBody) throws Exception {
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices.length() == 0) {
            throw new Exception("Empty choices array in response");
        }
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        return message.getString("content");
    }
}

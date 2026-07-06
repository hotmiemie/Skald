package com.skald.app.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * App 配置管理（基于 SharedPreferences）
 */
public class AppConfig {

    private static final String KEY_API_URL = "api_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_MODEL = "api_model";
    private static final String KEY_CUSTOM_PROMPT = "custom_prompt";
    private static final String KEY_FLOAT_BUTTON_X = "float_button_x";
    private static final String KEY_FLOAT_BUTTON_Y = "float_button_y";
    private static final String KEY_SERVICE_RUNNING = "service_running";
    // 默认值
    private static final String DEFAULT_API_URL = "https://api.deepseek.com";
    private static final String DEFAULT_API_MODEL = "deepseek-chat";

    private final SharedPreferences prefs;

    private static AppConfig instance;

    private static final String PREFS_NAME = "skald_prefs";

    private AppConfig(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AppConfig getInstance(Context context) {
        if (instance == null) {
            instance = new AppConfig(context);
        }
        return instance;
    }

    // ---- API 配置 ----

    public String getApiUrl() {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL);
    }

    public void setApiUrl(String url) {
        prefs.edit().putString(KEY_API_URL, url).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }

    public String getApiModel() {
        return prefs.getString(KEY_API_MODEL, DEFAULT_API_MODEL);
    }

    public void setApiModel(String model) {
        prefs.edit().putString(KEY_API_MODEL, model).apply();
    }

    /** 是否已配置 API Key */
    public boolean isApiConfigured() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }

    // ---- 自定义提示词 ----

    public String getCustomPrompt() {
        return prefs.getString(KEY_CUSTOM_PROMPT, "");
    }

    public void setCustomPrompt(String prompt) {
        prefs.edit().putString(KEY_CUSTOM_PROMPT, prompt).apply();
    }

    // ---- 悬浮按钮位置 ----

    public int getFloatButtonX(int defaultX) {
        return prefs.getInt(KEY_FLOAT_BUTTON_X, defaultX);
    }

    public void setFloatButtonX(int x) {
        prefs.edit().putInt(KEY_FLOAT_BUTTON_X, x).apply();
    }

    public int getFloatButtonY(int defaultY) {
        return prefs.getInt(KEY_FLOAT_BUTTON_Y, defaultY);
    }

    public void setFloatButtonY(int y) {
        prefs.edit().putInt(KEY_FLOAT_BUTTON_Y, y).apply();
    }

    // ---- 服务状态 ----

    public boolean isServiceRunning() {
        return prefs.getBoolean(KEY_SERVICE_RUNNING, false);
    }

    public void setServiceRunning(boolean running) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply();
    }
}

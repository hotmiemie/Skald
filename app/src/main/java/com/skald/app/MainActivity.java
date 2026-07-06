package com.skald.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.skald.app.config.AppConfig;
import com.skald.app.config.PromptTemplate;
import com.skald.app.floatwindow.FloatWindowService;
import com.skald.app.permission.PermissionChecker;
import com.skald.app.permission.PermissionGuideActivity;

/**
 * 主界面：API 配置、权限状态查看、提示词编辑、服务控制
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AppConfig config;
    private TextInputEditText etApiUrl;
    private TextInputEditText etApiModel;
    private TextInputEditText etApiKey;
    private TextInputEditText etCustomPrompt;
    private TextView tvAccessibilityStatus;
    private TextView tvOverlayStatus;
    private Button btnToggleService;
    private TextView tvApiStatus;
    private TextView tvPromptStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = AppConfig.getInstance(this);

        initViews();
        loadConfig();
        initListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateServiceButton();
    }

    private void initViews() {
        etApiUrl = findViewById(R.id.et_api_url);
        etApiModel = findViewById(R.id.et_api_model);
        etApiKey = findViewById(R.id.et_api_key);
        etCustomPrompt = findViewById(R.id.et_custom_prompt);
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status);
        tvOverlayStatus = findViewById(R.id.tv_overlay_status);
        btnToggleService = findViewById(R.id.btn_toggle_service);
        tvApiStatus = findViewById(R.id.tv_api_status);
        tvPromptStatus = findViewById(R.id.tv_prompt_status);
    }

    private void loadConfig() {
        etApiUrl.setText(config.getApiUrl());
        etApiModel.setText(config.getApiModel());
        etApiKey.setText(config.getApiKey());

        String customPrompt = config.getCustomPrompt();
        if (TextUtils.isEmpty(customPrompt)) {
            etCustomPrompt.setText(PromptTemplate.DEFAULT_SYSTEM_PROMPT);
        } else {
            etCustomPrompt.setText(customPrompt);
        }
    }

    private void initListeners() {
        findViewById(R.id.btn_save_config).setOnClickListener(v -> saveConfig());
        findViewById(R.id.btn_test_connection).setOnClickListener(v -> testConnection());
        findViewById(R.id.btn_permission_guide).setOnClickListener(v -> openPermissionGuide());
        findViewById(R.id.btn_reset_prompt).setOnClickListener(v -> resetPrompt());
        findViewById(R.id.btn_save_prompt).setOnClickListener(v -> savePrompt());
        btnToggleService.setOnClickListener(v -> toggleService());
    }

    private void saveConfig() {
        String url = etApiUrl.getText() != null ? etApiUrl.getText().toString().trim() : "";
        String model = etApiModel.getText() != null ? etApiModel.getText().toString().trim() : "";
        String key = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";

        config.setApiUrl(url);
        config.setApiModel(model);
        config.setApiKey(key);

        showApiStatus("✓ 配置已保存", android.graphics.Color.parseColor("#4CAF50"));
        Log.i(TAG, "API config saved");
    }

    private void testConnection() {
        saveConfig();

        if (!config.isApiConfigured()) {
            showApiStatus("请先配置 API Key", android.graphics.Color.parseColor("#F44336"));
            return;
        }

        showApiStatus("正在测试连接...", android.graphics.Color.parseColor("#666666"));

        new Thread(() -> {
            try {
                com.skald.app.api.DeepSeekClient client = new com.skald.app.api.DeepSeekClient();
                com.skald.app.api.DeepSeekClient.QueryRequest req =
                        new com.skald.app.api.DeepSeekClient.QueryRequest(
                                config.getApiUrl(), config.getApiKey(), config.getApiModel(),
                                "你是助手。", "回复: 你好");
                String response = client.querySync(req);
                runOnUiThread(() -> showApiStatus("✓ 连接成功 — " + config.getApiModel(),
                        android.graphics.Color.parseColor("#4CAF50")));
            } catch (Exception e) {
                Log.e(TAG, "Connection test failed", e);
                runOnUiThread(() -> showApiStatus("✗ 连接失败: " + e.getMessage(),
                        android.graphics.Color.parseColor("#F44336")));
            }
        }).start();
    }

    private void openPermissionGuide() {
        startActivity(new Intent(this, PermissionGuideActivity.class));
    }

    private void resetPrompt() {
        etCustomPrompt.setText(PromptTemplate.DEFAULT_SYSTEM_PROMPT);
        config.setCustomPrompt("");
        showPromptStatus("已恢复默认提示词", android.graphics.Color.parseColor("#4CAF50"));
    }

    private void savePrompt() {
        String prompt = etCustomPrompt.getText() != null
                ? etCustomPrompt.getText().toString().trim() : "";
        config.setCustomPrompt(prompt);
        showPromptStatus("✓ 提示词已保存", android.graphics.Color.parseColor("#4CAF50"));
    }

    private void showApiStatus(String msg, int color) {
        tvApiStatus.setText(msg);
        tvApiStatus.setTextColor(color);
        tvApiStatus.setVisibility(android.view.View.VISIBLE);
    }

    private void showPromptStatus(String msg, int color) {
        tvPromptStatus.setText(msg);
        tvPromptStatus.setTextColor(color);
        tvPromptStatus.setVisibility(android.view.View.VISIBLE);
    }

    private void updatePermissionStatus() {
        boolean accessibilityOn = PermissionChecker.isAccessibilityServiceEnabled(this);
        tvAccessibilityStatus.setText(accessibilityOn ? R.string.status_granted : R.string.status_denied);
        tvAccessibilityStatus.setTextColor(accessibilityOn
                ? getColor(R.color.style_conservative)
                : getColor(R.color.style_aggressive));

        boolean overlayOn = Settings.canDrawOverlays(this);
        tvOverlayStatus.setText(overlayOn ? R.string.status_granted : R.string.status_denied);
        tvOverlayStatus.setTextColor(overlayOn
                ? getColor(R.color.style_conservative)
                : getColor(R.color.style_aggressive));
    }

    private void updateServiceButton() {
        boolean running = config.isServiceRunning();
        if (running) {
            btnToggleService.setText(R.string.float_stop);
            btnToggleService.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.style_aggressive)));
        } else {
            btnToggleService.setText(R.string.float_start);
            btnToggleService.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
        }
    }

    private void toggleService() {
        if (config.isServiceRunning()) {
            stopFloatService();
        } else {
            startFloatService();
        }
    }

    private void startFloatService() {
        if (!PermissionChecker.isAccessibilityServiceEnabled(this)) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
            openPermissionGuide();
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, FloatWindowService.class);
        intent.setAction(FloatWindowService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        config.setServiceRunning(true);
        updateServiceButton();
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void stopFloatService() {
        Intent intent = new Intent(this, FloatWindowService.class);
        intent.setAction(FloatWindowService.ACTION_STOP);
        startService(intent);

        config.setServiceRunning(false);
        updateServiceButton();
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show();
    }
}

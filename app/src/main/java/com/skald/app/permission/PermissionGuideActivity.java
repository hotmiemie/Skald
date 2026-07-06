package com.skald.app.permission;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skald.app.R;

/**
 * 权限引导界面：逐项检查并引导用户开启必要权限
 */
public class PermissionGuideActivity extends AppCompatActivity {

    private TextView tvAccessibilityStatus;
    private TextView tvOverlayStatus;
    private TextView tvNotificationStatus;
    private Button btnAllDone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_guide);

        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status);
        tvOverlayStatus = findViewById(R.id.tv_overlay_status);
        tvNotificationStatus = findViewById(R.id.tv_notification_status);
        btnAllDone = findViewById(R.id.btn_all_done);

        findViewById(R.id.btn_grant_accessibility).setOnClickListener(v -> openAccessibilitySettings());
        findViewById(R.id.btn_grant_overlay).setOnClickListener(v -> openOverlaySettings());
        findViewById(R.id.btn_grant_notification).setOnClickListener(v -> openNotificationSettings());
        btnAllDone.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllStatus();
    }

    private void updateAllStatus() {
        boolean accessibilityOn = PermissionChecker.isAccessibilityServiceEnabled(this);
        boolean overlayOn = PermissionChecker.canDrawOverlays(this);
        boolean notificationOn = PermissionChecker.hasNotificationPermission(this);

        updateStatusView(tvAccessibilityStatus, accessibilityOn);
        updateStatusView(tvOverlayStatus, overlayOn);
        updateStatusView(tvNotificationStatus, notificationOn);

        // 所有权限就绪后才能点"完成"
        boolean allGranted = accessibilityOn && overlayOn && notificationOn;
        btnAllDone.setEnabled(allGranted);

        if (allGranted) {
            Toast.makeText(this, "所有权限已就绪！", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusView(TextView tv, boolean granted) {
        if (granted) {
            tv.setText(R.string.status_granted);
            tv.setTextColor(getColor(R.color.style_conservative));
        } else {
            tv.setText(R.string.status_denied);
            tv.setTextColor(getColor(R.color.style_aggressive));
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}

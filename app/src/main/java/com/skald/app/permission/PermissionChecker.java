package com.skald.app.permission;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

/**
 * 权限检查工具类
 */
public class PermissionChecker {

    private static final String ACCESSIBILITY_SERVICE_NAME =
            "com.skald.app/com.skald.app.service.SkaldAccessibilityService";

    /**
     * 检查无障碍服务是否已开启
     */
    public static boolean isAccessibilityServiceEnabled(Context context) {
        int enabled = 0;
        try {
            enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        if (enabled == 1) {
            String services = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (services != null) {
                return services.contains("com.skald.app");
            }
        }
        return false;
    }

    /**
     * 检查悬浮窗权限
     */
    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * 检查通知权限（Android 13+）
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 以下无需授权
    }
}

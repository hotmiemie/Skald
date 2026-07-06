package com.skald.app.screenshot;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于 AccessibilityService.takeScreenshot 的截图实现（API 30+）
 *
 * 使用 takeScreenshot(Display, Executor, TakeScreenshotCallback) 重载，
 * 并通过 ScreenshotResult.getHardwareBuffer() / getColorSpace() 获取像素数据。
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public class AccessibilityScreenshot implements ScreenshotProvider {

    private static final String TAG = "A11yScreenshot";

    private final AccessibilityService accessibilityService;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public AccessibilityScreenshot(AccessibilityService service) {
        this.accessibilityService = service;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void takeScreenshot(Callback callback) {
        if (!isAvailable()) {
            String msg = "AccessibilityService.takeScreenshot not available";
            Log.e(TAG, msg);
            mainHandler.post(() -> callback.onFailure(-1, msg));
            return;
        }

        try {
            accessibilityService.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    executor,
                    new AccessibilityService.TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(@NonNull AccessibilityService.ScreenshotResult result) {
                            Bitmap bitmap = null;
                            try {
                                bitmap = Bitmap.wrapHardwareBuffer(
                                        result.getHardwareBuffer(),
                                        result.getColorSpace());
                                if (bitmap == null) {
                                    mainHandler.post(() ->
                                            callback.onFailure(-2, "wrapHardwareBuffer returned null"));
                                    return;
                                }

                                // 创建一份可修改的副本（HardwareBuffer 不可直接操作）
                                Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                                if (copyBitmap == null) {
                                    copyBitmap = bitmap;
                                } else {
                                    bitmap.recycle();
                                }

                                final Bitmap finalBitmap = copyBitmap;
                                mainHandler.post(() -> callback.onSuccess(finalBitmap));
                                Log.i(TAG, "Screenshot captured: " +
                                        finalBitmap.getWidth() + "x" + finalBitmap.getHeight());
                            } catch (Exception e) {
                                Log.e(TAG, "Screenshot processing failed", e);
                                if (bitmap != null) {
                                    bitmap.recycle();
                                }
                                mainHandler.post(() ->
                                        callback.onFailure(-3, "Screenshot processing failed: " + e.getMessage()));
                            }
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            Log.e(TAG, "takeScreenshot failed with code: " + errorCode);
                            mainHandler.post(() ->
                                    callback.onFailure(errorCode, getErrorMessage(errorCode)));
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "takeScreenshot threw exception", e);
            mainHandler.post(() -> callback.onFailure(-1, e.getMessage()));
        }
    }

    @Override
    public boolean isAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && accessibilityService != null
                && accessibilityService.getSystemService(WindowManager.class) != null;
    }

    @Override
    public void release() {
        executor.shutdownNow();
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR:
                return "系统内部错误，请重试";
            case AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT:
                return "操作太频繁，请稍后重试";
            case AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY:
                return "无效的显示器";
            case AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS:
                return "无障碍服务未授权截图权限";
            case AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW:
                return "当前界面为安全窗口，无法截图";
            default:
                return "截图失败 (错误码: " + errorCode + ")";
        }
    }
}

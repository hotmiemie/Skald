package com.skald.app.floatwindow;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.skald.app.R;
import com.skald.app.config.AppConfig;
import com.skald.app.util.ScreenUtils;

/**
 * 悬浮按钮管理器：创建、显示、隐藏、拖拽悬浮按钮
 */
public class FloatButtonManager {

    private static final String TAG = "FloatButtonMgr";

    private final Context context;
    private final WindowManager windowManager;
    private final AppConfig config;

    private View floatButtonView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;

    private float initialTouchX;
    private float initialTouchY;
    private float initialPosX;
    private float initialPosY;
    private long lastClickTime = 0;
    private static final int CLICK_THRESHOLD_DP = 10;
    private static final int CLICK_INTERVAL_MS = 300;

    private OnFloatButtonClickListener clickListener;

    public interface OnFloatButtonClickListener {
        void onFloatButtonClick();
    }

    public FloatButtonManager(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.config = AppConfig.getInstance(context);
    }

    public void setOnFloatButtonClickListener(OnFloatButtonClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 显示悬浮按钮
     */
    public void show() {
        if (isShowing) return;

        try {
            createFloatView();
            calculateDefaultPosition();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                params.type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

            params.format = PixelFormat.TRANSLUCENT;
            params.gravity = Gravity.TOP | Gravity.START;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;

            windowManager.addView(floatButtonView, params);
            isShowing = true;
            Log.i(TAG, "Float button shown");
        } catch (SecurityException e) {
            Log.e(TAG, "No overlay permission", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show float button", e);
        }
    }

    /**
     * 隐藏悬浮按钮
     */
    public void hide() {
        if (!isShowing || floatButtonView == null) return;

        try {
            windowManager.removeView(floatButtonView);
            isShowing = false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide float button", e);
        }
    }

    /**
     * 更新位置到默认位置
     */
    public void resetPosition() {
        calculateDefaultPosition();
        if (isShowing && floatButtonView != null) {
            try {
                windowManager.updateViewLayout(floatButtonView, params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update position", e);
            }
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    private void createFloatView() {
        floatButtonView = LayoutInflater.from(context)
                .inflate(R.layout.float_button, null);

        params = new WindowManager.LayoutParams();

        floatButtonView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialPosX = params.x;
                        initialPosY = params.y;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        params.x = (int) (initialPosX + deltaX);
                        params.y = (int) (initialPosY + deltaY);
                        try {
                            windowManager.updateViewLayout(floatButtonView, params);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to move float button", e);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        float totalDeltaX = Math.abs(event.getRawX() - initialTouchX);
                        float totalDeltaY = Math.abs(event.getRawY() - initialTouchY);
                        int clickThreshold = ScreenUtils.dpToPx(context, CLICK_THRESHOLD_DP);

                        // 判定为点击（移动距离很小）
                        if (totalDeltaX < clickThreshold && totalDeltaY < clickThreshold) {
                            long now = System.currentTimeMillis();
                            if (now - lastClickTime > CLICK_INTERVAL_MS) {
                                lastClickTime = now;
                                onButtonClicked();
                            }
                        } else {
                            // 拖动结束，保存位置
                            savePosition();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void onButtonClicked() {
        if (clickListener != null) {
            clickListener.onFloatButtonClick();
        }
    }

    private void calculateDefaultPosition() {
        int screenWidth = ScreenUtils.getScreenWidth(context);
        int screenHeight = ScreenUtils.getScreenHeight(context);

        // 默认放在屏幕右侧中间偏下位置
        int defaultX = screenWidth - ScreenUtils.dpToPx(context, 60);
        int defaultY = screenHeight / 2;

        params.x = config.getFloatButtonX(defaultX);
        params.y = config.getFloatButtonY(defaultY);
    }

    private void savePosition() {
        config.setFloatButtonX(params.x);
        config.setFloatButtonY(params.y);
    }
}

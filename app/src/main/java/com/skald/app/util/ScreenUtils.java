package com.skald.app.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * 屏幕工具类
 */
public class ScreenUtils {

    /** 获取屏幕宽度（像素） */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            return point.x;
        }
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    /** 获取屏幕高度（像素，含导航栏和状态栏） */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            return point.y;
        }
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    /** 获取状态栏高度 */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        // 默认回退值
        if (result <= 0) {
            result = dpToPx(context, 24);
        }
        return result;
    }

    /** 获取导航栏高度 */
    public static int getNavigationBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources()
                .getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /** dp → px */
    public static int dpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f);
    }

    /** px → dp */
    public static float pxToDp(Context context, float px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return px / metrics.density;
    }
}

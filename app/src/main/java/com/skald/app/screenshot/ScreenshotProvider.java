package com.skald.app.screenshot;

import android.graphics.Bitmap;

/**
 * 截图能力抽象接口
 */
public interface ScreenshotProvider {

    /** 截图结果回调 */
    interface Callback {
        void onSuccess(Bitmap bitmap);
        void onFailure(int errorCode, String errorMessage);
    }

    /**
     * 执行截图
     * @param callback 异步回调
     */
    void takeScreenshot(Callback callback);

    /**
     * 是否支持截图
     */
    boolean isAvailable();

    /**
     * 释放资源
     */
    void release();
}

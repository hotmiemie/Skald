package com.skald.app.ocr;

import android.graphics.Bitmap;

import com.skald.app.model.OcrResult;

import java.util.List;

/**
 * OCR 引擎抽象接口
 */
public interface OcrEngine {

    /** OCR 识别回调 */
    interface Callback {
        void onSuccess(List<OcrResult> results);
        void onFailure(int errorCode, String errorMessage);
    }

    /**
     * 对 Bitmap 执行 OCR 识别
     * @param bitmap 待识别的图片（建议 ARGB_8888 格式）
     * @param callback 异步回调
     */
    void recognize(Bitmap bitmap, Callback callback);

    /**
     * 引擎是否已就绪
     */
    boolean isReady();

    /**
     * 初始化引擎（加载模型等）
     */
    void initialize(InitCallback callback);

    interface InitCallback {
        void onReady();
        void onError(String error);
    }

    /**
     * 释放引擎资源
     */
    void release();
}

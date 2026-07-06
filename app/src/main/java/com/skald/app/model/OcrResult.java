package com.skald.app.model;

import android.graphics.Rect;

/**
 * OCR 识别结果：一段文字及其位置信息
 */
public class OcrResult {

    /** 识别出的文字内容 */
    public final String text;

    /** 文字在截图中所在的矩形区域 */
    public final Rect boundingBox;

    /** 识别置信度 (0.0 ~ 1.0) */
    public final float confidence;

    public OcrResult(String text, Rect boundingBox, float confidence) {
        this.text = text;
        this.boundingBox = boundingBox;
        this.confidence = confidence;
    }

    /** 获取矩形区域的中心 X 坐标 */
    public float getCenterX() {
        return (boundingBox.left + boundingBox.right) / 2f;
    }

    /** 获取矩形区域的中心 Y 坐标 */
    public float getCenterY() {
        return (boundingBox.top + boundingBox.bottom) / 2f;
    }

    @Override
    public String toString() {
        return "OcrResult{text='" + text + "', box=" + boundingBox + ", conf=" + confidence + "}";
    }
}

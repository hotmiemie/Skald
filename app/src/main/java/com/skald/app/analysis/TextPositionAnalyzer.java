package com.skald.app.analysis;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.skald.app.model.ChatMessage;
import com.skald.app.model.ChatMessage.Speaker;
import com.skald.app.model.OcrResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 文本位置分析器：从 OCR 结果中识别对话。
 *
 * 说话人判断优先级：
 * 1. 气泡背景色（主）—— 微信绿色气泡=我，白色/灰色气泡=对方
 * 2. 屏幕位置（辅）—— 左=对方，右=我（颜色无法判断时回退）
 *
 * 噪声过滤：
 * - 状态栏区域（电量/信号/时间）
 * - 顶部标题栏（联系人名称）
 * - 居中时间戳（"下午 3:25"、"昨天" 等）
 * - 系统 UI 碎片
 */
public class TextPositionAnalyzer {

    private static final String TAG = "TextPosAnalyzer";

    private final int screenWidth;
    private final float screenCenterX;

    /** 状态栏高度（像素） */
    private final int statusBarHeight;
    /** 顶部标题栏底部 Y 坐标 */
    private final int headerBottom;

    /** 边缘排除比例 */
    private static final float EDGE_EXCLUDE_RATIO = 0.06f;

    /** 居中判定：距离中线的最大比例 */
    private static final float CENTER_ZONE_RATIO = 0.12f;

    /** 同一气泡行间 Y 间距阈值（dp） */
    private static final float SAME_BUBBLE_Y_GAP_DP = 60f;

    /** X 方向重叠比例阈值 */
    private static final float SAME_BUBBLE_X_OVERLAP = 0.25f;

    /** 最小置信度 */
    private static final float MIN_CONFIDENCE = 0.3f;

    /** 绿色检测 */
    private static final int GREEN_DOMINANCE_MIN = 20;
    private static final int GREEN_CHANNEL_MIN = 100;

    /** 白色/灰色检测 */
    private static final int WHITE_VARIANCE_MAX = 30;
    private static final int WHITE_BRIGHTNESS_MIN = 160;

    private Bitmap screenshot;

    /**
     * @param screenWidth 屏幕宽度(px)
     * @param statusBarHeight 状态栏高度(px)
     * @param headerHeightDp 标题栏高度(dp)，默认 56
     */
    public TextPositionAnalyzer(int screenWidth, int statusBarHeight, int headerHeightDp) {
        this.screenWidth = screenWidth;
        this.screenCenterX = screenWidth / 2f;
        this.statusBarHeight = statusBarHeight;
        this.headerBottom = statusBarHeight + dpToPx(headerHeightDp);
    }

    /** 向后兼容的构造器 */
    public TextPositionAnalyzer(int screenWidth) {
        this(screenWidth, 0, 56);
    }

    public void setScreenshot(Bitmap bitmap) {
        this.screenshot = bitmap;
    }

    // ==================== 公开方法 ====================

    public List<ChatMessage> analyze(List<OcrResult> ocrResults) {
        if (ocrResults == null || ocrResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 过滤噪声（状态栏、标题栏、时间戳等）
        List<OcrResult> filtered = filterNoise(ocrResults);
        Log.d(TAG, "Filtered: " + ocrResults.size() + " → " + filtered.size());
        if (filtered.isEmpty()) return Collections.emptyList();

        // 2. 按 Y 排序
        Collections.sort(filtered, Comparator.comparingDouble(r -> r.getCenterY()));

        // 3. 气泡聚类
        List<Bubble> bubbles = clusterIntoBubbles(filtered);
        Log.d(TAG, "Bubbles: " + bubbles.size());

        // 4. 分类
        List<ChatMessage> messages = new ArrayList<>();
        for (Bubble bubble : bubbles) {
            Speaker speaker = classifyBubble(bubble);
            String text = bubble.getMergedText();
            if (!text.isEmpty()) {
                messages.add(new ChatMessage(speaker, text, bubble.getCenterY()));
            }
        }
        return messages;
    }

    // ==================== 噪声过滤 ====================

    private List<OcrResult> filterNoise(List<OcrResult> results) {
        List<OcrResult> filtered = new ArrayList<>();
        float leftEdge = screenWidth * EDGE_EXCLUDE_RATIO;
        float rightEdge = screenWidth * (1f - EDGE_EXCLUDE_RATIO);

        for (OcrResult r : results) {
            String text = r.text.trim();

            // 置信度
            if (r.confidence < MIN_CONFIDENCE) continue;
            if (text.isEmpty()) continue;

            // ❶ 状态栏区域：Y 坐标在状态栏范围内
            if (r.boundingBox.bottom <= statusBarHeight + dpToPx(8)) {
                continue;
            }

            // ❷ 顶部标题栏：Y 在状态栏和标题栏之间，且居中
            if (r.boundingBox.top <= headerBottom
                    && isInCenterZone(r.getCenterX())) {
                continue;
            }

            // ❸ 居中时间戳：X 接近屏幕中线 + 匹配时间/日期模式
            if (isInCenterZone(r.getCenterX()) && isTimestampOrSystem(text)) {
                continue;
            }

            // ❹ 纯数字/符号的短文本
            if (text.length() < 2 && text.matches("^[\\d:：.\\-/\\s]+$")) continue;

            // ❺ 系统提示文本（如"你撤回了一条消息"、"对方正在输入..."）
            if (isSystemMessage(text)) continue;

            // ❻ 边缘排除
            float cx = r.getCenterX();
            if (cx < leftEdge || cx > rightEdge) continue;

            filtered.add(r);
        }
        return filtered;
    }

    /** X 坐标是否在屏幕中线附近（±12%） */
    private boolean isInCenterZone(float cx) {
        float threshold = screenWidth * CENTER_ZONE_RATIO;
        return Math.abs(cx - screenCenterX) < threshold;
    }

    /** 是否匹配时间/日期模式 */
    private boolean isTimestampOrSystem(String text) {
        // 时间：12:30, 下午3:25, 上午 10:00, 晚上8点
        if (text.matches(".*(上午|下午|中午|晚上|凌晨|早上).*\\d.*")) return true;
        if (text.matches(".*\\d{1,2}[:：]\\d{2}.*")) return true;
        // 日期：昨天, 今天, 明天, 星期一~日, 3月15日, 2024年
        if (text.matches("^(昨天|今天|明天|星期[一二三四五六日天])$")) return true;
        if (text.matches("^\\d{1,2}月\\d{1,2}日.*")) return true;
        if (text.matches("^\\d{4}年.*")) return true;
        if (text.matches("^\\d{1,2}/\\d{1,2}.*")) return true;
        return false;
    }

    /** 是否系统提示消息 */
    private boolean isSystemMessage(String text) {
        String[] keywords = {
            "撤回了一条消息", "加入群聊", "退出群聊", "修改群名",
            "正在输入", "对方正在输入", "已读", "已发送",
            "拍了拍", "成为好友", "添加好友", "通过验证",
            "群公告", "发送了", "视频通话", "语音通话",
            "已结束", "未接听", "已拒绝", "取消"
        };
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ==================== 气泡聚类 ====================

    private List<Bubble> clusterIntoBubbles(List<OcrResult> sortedLines) {
        List<Bubble> bubbles = new ArrayList<>();
        if (sortedLines.isEmpty()) return bubbles;

        Bubble current = new Bubble();
        current.addLine(sortedLines.get(0));

        for (int i = 1; i < sortedLines.size(); i++) {
            OcrResult prev = sortedLines.get(i - 1);
            OcrResult curr = sortedLines.get(i);

            float yGap = curr.boundingBox.top - prev.boundingBox.bottom;

            if (yGap < SAME_BUBBLE_Y_GAP_DP
                    && isXOverlapping(prev.boundingBox, curr.boundingBox)) {
                current.addLine(curr);
            } else {
                bubbles.add(current);
                current = new Bubble();
                current.addLine(curr);
            }
        }
        bubbles.add(current);
        return bubbles;
    }

    private boolean isXOverlapping(Rect a, Rect b) {
        int overlapLeft = Math.max(a.left, b.left);
        int overlapRight = Math.min(a.right, b.right);
        if (overlapLeft >= overlapRight) return false;
        int overlapWidth = overlapRight - overlapLeft;
        int narrowerWidth = Math.min(a.width(), b.width());
        if (narrowerWidth <= 0) return false;
        return (float) overlapWidth / narrowerWidth >= SAME_BUBBLE_X_OVERLAP;
    }

    // ==================== 说话人分类 ====================

    private Speaker classifyBubble(Bubble bubble) {
        if (screenshot != null && !screenshot.isRecycled()) {
            Speaker colorResult = detectByColor(bubble);
            if (colorResult != null) return colorResult;
        }
        return classifyByPosition(bubble);
    }

    private Speaker detectByColor(Bubble bubble) {
        Rect box = bubble.getMergedBox();
        if (box == null || box.isEmpty()) return null;

        int sampleY = box.top + 10;
        if (sampleY >= box.bottom) sampleY = box.centerY();

        int[] sampleXs = { box.left + 10, box.centerX(), box.right - 10 };

        int greenVotes = 0, whiteVotes = 0, validSamples = 0;
        for (int sx : sampleXs) {
            if (sx < 0 || sx >= screenshot.getWidth()) continue;
            if (sampleY < 0 || sampleY >= screenshot.getHeight()) continue;

            int pixel = screenshot.getPixel(sx, sampleY);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            if (isGreenBubble(r, g, b)) { greenVotes++; validSamples++; }
            else if (isWhiteBubble(r, g, b)) { whiteVotes++; validSamples++; }
        }

        if (validSamples == 0) return null;
        if (greenVotes > whiteVotes) return Speaker.SELF;
        if (whiteVotes > greenVotes) return Speaker.OTHER;
        return null;
    }

    private boolean isGreenBubble(int r, int g, int b) {
        return g >= GREEN_CHANNEL_MIN
                && (g - r) >= GREEN_DOMINANCE_MIN
                && (g - b) >= GREEN_DOMINANCE_MIN;
    }

    private boolean isWhiteBubble(int r, int g, int b) {
        int maxC = Math.max(r, Math.max(g, b));
        int minC = Math.min(r, Math.min(g, b));
        return (maxC - minC) <= WHITE_VARIANCE_MAX && minC >= WHITE_BRIGHTNESS_MIN;
    }

    private Speaker classifyByPosition(Bubble bubble) {
        float cx = bubble.getCenterX();
        float leftEdge = bubble.getLeftEdge();

        if (cx < screenCenterX) return Speaker.OTHER;
        if (cx < screenCenterX * 1.15f && leftEdge < screenCenterX * 0.6f) return Speaker.OTHER;
        return Speaker.SELF;
    }

    // ==================== 工具 ====================

    private int dpToPx(float dp) {
        return (int) (dp * 2.75f + 0.5f); // 近似值，足够过滤使用
    }

    // ==================== 内部类 ====================

    private static class Bubble {
        private final List<OcrResult> lines = new ArrayList<>();
        private Rect mergedBox;

        void addLine(OcrResult line) { lines.add(line); mergedBox = null; }

        float getCenterX() { Rect b = getMergedBox(); return (b.left + b.right) / 2f; }
        float getCenterY() { Rect b = getMergedBox(); return (b.top + b.bottom) / 2f; }
        float getLeftEdge() { return getMergedBox().left; }

        Rect getMergedBox() {
            if (mergedBox != null) return mergedBox;
            if (lines.isEmpty()) return new Rect();
            int l = Integer.MAX_VALUE, t = Integer.MAX_VALUE;
            int r = Integer.MIN_VALUE, b = Integer.MIN_VALUE;
            for (OcrResult line : lines) {
                Rect rb = line.boundingBox;
                l = Math.min(l, rb.left);
                t = Math.min(t, rb.top);
                r = Math.max(r, rb.right);
                b = Math.max(b, rb.bottom);
            }
            mergedBox = new Rect(l, t, r, b);
            return mergedBox;
        }

        String getMergedText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(lines.get(i).text);
            }
            return sb.toString();
        }
    }
}

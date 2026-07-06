package com.skald.app.floatwindow;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.skald.app.R;
import com.skald.app.clipboard.ClipboardHelper;
import com.skald.app.model.Suggestion;
import com.skald.app.util.ScreenUtils;

import java.util.List;

/**
 * 结果悬浮窗：对话预览 + 话术建议
 */
public class FloatResultWindow {

    private static final String TAG = "FloatResultWnd";

    private final Context context;
    private final WindowManager windowManager;
    private final ClipboardHelper clipboardHelper;

    private View resultWindowView;
    private WindowManager.LayoutParams params;
    private boolean isShowing = false;

    private LinearLayout layoutLoading;
    private TextView tvError;
    private ScrollView scrollContent;
    private LinearLayout layoutConversation;
    private TextView tvConversation;
    private TextView tvSuggestionsTitle;
    private LinearLayout layoutSuggestions;

    public FloatResultWindow(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.clipboardHelper = new ClipboardHelper(context);
    }

    public void showLoading() {
        createResultWindow();
        layoutLoading.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        addToWindow();
    }

    public void showSuggestions(String conversation, List<Suggestion> suggestions) {
        createResultWindow();
        layoutLoading.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);

        // 对话预览
        if (conversation != null && !conversation.isEmpty()) {
            layoutConversation.setVisibility(View.VISIBLE);
            tvConversation.setText(buildColoredConversation(conversation));
        } else {
            layoutConversation.setVisibility(View.GONE);
        }

        tvSuggestionsTitle.setVisibility(View.VISIBLE);

        layoutSuggestions.setVisibility(View.VISIBLE);
        layoutSuggestions.removeAllViews();
        for (int i = 0; i < suggestions.size(); i++) {
            View itemView = createSuggestionItem(suggestions.get(i), i == 0);
            layoutSuggestions.addView(itemView);
        }

        addToWindow();
    }

    public void showSuggestions(List<Suggestion> suggestions) {
        showSuggestions(null, suggestions);
    }

    public void showError(String errorMessage) {
        createResultWindow();
        layoutLoading.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(errorMessage != null ? errorMessage : "出错了");
        addToWindow();
    }

    public void hide() {
        if (!isShowing || resultWindowView == null) return;
        try {
            windowManager.removeView(resultWindowView);
            isShowing = false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide", e);
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    // ==================== 内部 ====================

    private void createResultWindow() {
        if (resultWindowView != null) return;

        resultWindowView = LayoutInflater.from(context)
                .inflate(R.layout.float_result_window, null);
        layoutLoading = resultWindowView.findViewById(R.id.layout_loading);
        tvError = resultWindowView.findViewById(R.id.tv_error);
        scrollContent = resultWindowView.findViewById(R.id.scroll_content);
        layoutConversation = resultWindowView.findViewById(R.id.layout_conversation);
        tvConversation = resultWindowView.findViewById(R.id.tv_conversation);
        tvSuggestionsTitle = resultWindowView.findViewById(R.id.tv_suggestions_title);
        layoutSuggestions = resultWindowView.findViewById(R.id.layout_suggestions);

        resultWindowView.findViewById(R.id.btn_close_result)
                .setOnClickListener(v -> hide());

        params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.BOTTOM;
        params.width = (int) (ScreenUtils.getScreenWidth(context) * 0.94);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.y = ScreenUtils.dpToPx(context, 80);
    }

    private void addToWindow() {
        if (isShowing) {
            try {
                windowManager.updateViewLayout(resultWindowView, params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update", e);
            }
            return;
        }
        try {
            windowManager.addView(resultWindowView, params);
            isShowing = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to show", e);
        }
    }

    private SpannableStringBuilder buildColoredConversation(String conversation) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String[] lines = conversation.split("\n");
        int otherColor = 0xFF555555;
        int selfColor = 0xFF1B8C3E;

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append("\n");
            String line = lines[i];
            int color = line.startsWith("对方:") ? otherColor : (line.startsWith("我:") ? selfColor : 0xFF666666);
            int start = sb.length();
            sb.append(line);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    /** 创建带背景色的话术条目 */
    private View createSuggestionItem(Suggestion suggestion, boolean isFirst) {
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        int padH = ScreenUtils.dpToPx(context, 10);
        int padV = ScreenUtils.dpToPx(context, 7);
        itemLayout.setPadding(padH, padV, padH, padV);

        // 根据风格设置背景色（浅色版本）
        int bgColor = getStyleBgColor(suggestion.style.getColor());
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(ScreenUtils.dpToPx(context, 8));
        bg.setColor(bgColor);
        itemLayout.setBackground(bg);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.topMargin = isFirst ? 0 : ScreenUtils.dpToPx(context, 6);
        itemLayout.setLayoutParams(itemParams);

        // 风格标签 (小圆点 + 文字)
        LinearLayout tagRow = new LinearLayout(context);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 彩色圆点
        View dot = new View(context);
        int dotSize = ScreenUtils.dpToPx(context, 8);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotParams.rightMargin = ScreenUtils.dpToPx(context, 6);
        dot.setLayoutParams(dotParams);
        try {
            android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
            dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dotBg.setColor(android.graphics.Color.parseColor(suggestion.style.getColor()));
            dot.setBackground(dotBg);
        } catch (Exception e) {
            dot.setBackgroundColor(0xFF666666);
        }
        tagRow.addView(dot);

        TextView styleTag = new TextView(context);
        styleTag.setText(suggestion.style.getLabel());
        styleTag.setTextSize(11);
        styleTag.setTextColor(0xFF333333);
        tagRow.addView(styleTag);

        itemLayout.addView(tagRow);

        // 话术内容
        TextView contentText = new TextView(context);
        contentText.setText(suggestion.text);
        contentText.setTextSize(13);
        contentText.setTextColor(0xFF333333);
        contentText.setPadding(0, ScreenUtils.dpToPx(context, 4), 0, 0);
        contentText.setLineSpacing(ScreenUtils.dpToPx(context, 2), 1f);
        itemLayout.addView(contentText);

        // 点击复制
        itemLayout.setOnClickListener(v -> {
            clipboardHelper.copyWithToast(suggestion.text);
            hide();
        });

        return itemLayout;
    }

    /** 根据风格颜色生成浅色背景 */
    private int getStyleBgColor(String hexColor) {
        try {
            int color = android.graphics.Color.parseColor(hexColor);
            int r = android.graphics.Color.red(color);
            int g = android.graphics.Color.green(color);
            int b = android.graphics.Color.blue(color);
            // 与白色混合：新值 = 原值 * 0.15 + 255 * 0.85
            r = (int) (r * 0.15 + 255 * 0.85);
            g = (int) (g * 0.15 + 255 * 0.85);
            b = (int) (b * 0.15 + 255 * 0.85);
            return android.graphics.Color.rgb(r, g, b);
        } catch (Exception e) {
            return 0xFFF0F0F0;
        }
    }
}

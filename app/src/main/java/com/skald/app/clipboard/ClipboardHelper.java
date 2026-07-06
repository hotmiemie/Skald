package com.skald.app.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

/**
 * 系统剪贴板辅助工具
 */
public class ClipboardHelper {

    private final ClipboardManager clipboard;
    private final Context context;

    public ClipboardHelper(Context context) {
        this.context = context.getApplicationContext();
        this.clipboard = (ClipboardManager)
                this.context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    /**
     * 复制文本到剪贴板
     * @param text 要复制的文本
     * @param label 剪贴板数据的标签
     * @return 是否复制成功
     */
    public boolean copy(String text, String label) {
        if (text == null || text.isEmpty()) return false;
        if (clipboard == null) return false;

        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        return true;
    }

    /**
     * 复制文本到剪贴板（使用默认标签）
     */
    public boolean copy(String text) {
        return copy(text, "Skald Suggestion");
    }

    /**
     * 复制文本并显示 Toast 提示
     */
    public void copyWithToast(String text) {
        if (copy(text)) {
            Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从剪贴板获取文本
     */
    public String getText() {
        if (clipboard == null || !clipboard.hasPrimaryClip()) return null;
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence text = clip.getItemAt(0).getText();
        return text != null ? text.toString() : null;
    }
}

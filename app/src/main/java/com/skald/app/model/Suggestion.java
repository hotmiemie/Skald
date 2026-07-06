package com.skald.app.model;

/**
 * DeepSeek 返回的一条回复建议
 */
public class Suggestion {

    /** 风格类型 */
    public enum Style {
        CONSERVATIVE("保守", "#4CAF50"),
        AGGRESSIVE("激进", "#F44336"),
        SURPRISING("出其不意", "#FF9800"),
        UNKNOWN("未知", "#666666");

        private final String label;
        private final String color;

        Style(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        /** 根据中文标签匹配风格 */
        public static Style fromLabel(String label) {
            if (label == null) return UNKNOWN;
            for (Style style : values()) {
                if (style.label.equals(label.trim())) {
                    return style;
                }
            }
            // 模糊匹配
            if (label.contains("保守")) return CONSERVATIVE;
            if (label.contains("激进")) return AGGRESSIVE;
            if (label.contains("出其不意") || label.contains("有趣") || label.contains("幽默") || label.contains("创意")) return SURPRISING;
            return UNKNOWN;
        }
    }

    public final Style style;
    public final String text;

    public Suggestion(Style style, String text) {
        this.style = style;
        this.text = text;
    }

    public Suggestion(String text) {
        this(Style.UNKNOWN, text);
    }

    @Override
    public String toString() {
        return "【" + style.getLabel() + "】" + text;
    }
}

package com.skald.app.api;

import com.skald.app.model.Suggestion;
import com.skald.app.model.Suggestion.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek 响应解析器：从 API 返回的文本中拆分出 3 条风格建议
 */
public class SuggestionParser {

    private static final String TAG = "SuggestionParser";

    private static final Pattern STYLE_PATTERN = Pattern.compile(
            "【(保守|激进|出其不意|有趣|幽默|创意)】\\s*",
            Pattern.CASE_INSENSITIVE);

    /**
     * 解析 API 返回内容，提取 4 条建议
     */
    public static List<Suggestion> parse(String rawResponse) {
        List<Suggestion> suggestions = new ArrayList<>();

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return suggestions;
        }

        String text = rawResponse.trim();

        // 方法 1: 按 --- 分隔
        String[] parts = text.split("\\n?---\\n?");
        if (parts.length >= 3) {
            for (String part : parts) {
                Suggestion suggestion = parseSingleLine(part.trim());
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
            if (suggestions.size() >= 3) return suggestions;
        }

        // 方法 2: 按编号拆分
        suggestions.clear();
        String[] numbered = text.split("\\n(?=\\d+\\.)");
        if (numbered.length >= 3) {
            for (String part : numbered) {
                Suggestion suggestion = parseSingleLine(part.trim());
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
            if (suggestions.size() >= 3) return suggestions;
        }

        // 方法 3: 按风格标签正则拆分
        suggestions.clear();
        Matcher matcher = STYLE_PATTERN.matcher(text);
        List<Integer> positions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        while (matcher.find()) {
            positions.add(matcher.start());
            labels.add(matcher.group(1));
        }

        if (positions.size() >= 3) {
            for (int i = 0; i < positions.size(); i++) {
                int start = positions.get(i) + labels.get(i).length() + 3;
                int end = (i + 1 < positions.size()) ? positions.get(i + 1) : text.length();
                String content = text.substring(start, end).trim();
                content = content.replaceAll("\\n?---\\s*$", "").trim();

                if (!content.isEmpty()) {
                    suggestions.add(new Suggestion(Style.fromLabel(labels.get(i)), content));
                }
            }
            if (suggestions.size() >= 3) return suggestions;
        }

        // 方法 4: 回退
        suggestions.clear();
        suggestions.add(new Suggestion(text));
        return suggestions;
    }

    private static Suggestion parseSingleLine(String line) {
        if (line == null || line.isEmpty()) return null;

        Matcher matcher = STYLE_PATTERN.matcher(line);
        if (matcher.find()) {
            Style style = Style.fromLabel(matcher.group(1));
            String content = line.substring(matcher.end()).trim();
            content = content.replaceAll("\\s*---\\s*$", "").trim();
            if (!content.isEmpty()) {
                return new Suggestion(style, content);
            }
        }

        String clean = line.replaceAll("^\\d+\\.\\s*", "").trim();
        if (clean.length() > 1) {
            return new Suggestion(clean);
        }
        return null;
    }
}

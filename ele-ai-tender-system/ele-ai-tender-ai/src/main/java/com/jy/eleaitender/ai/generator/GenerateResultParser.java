package com.jy.eleaitender.ai.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI输出结果解析器
 * 从AI返回的文本中提取结构化内容（JSON或Markdown）
 */
@Slf4j
@Component
public class GenerateResultParser {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 提取AI输出中的JSON内容
     * AI返回的内容可能包含Markdown代码块标记，需要清理
     */
    public String extractJson(String aiOutput) {
        if (!StringUtils.hasText(aiOutput)) {
            return "{}";
        }

        String cleaned = aiOutput.trim();

        // 去掉Markdown代码块标记 ```json ... ```
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }

        // 验证是否为合法JSON
        try {
            objectMapper.readTree(cleaned);
            return cleaned;
        } catch (Exception e) {
            log.warn("AI输出非合法JSON，尝试容错处理: {}", e.getMessage());
            // 尝试找到第一个 { 和最后一个 }
            return tryExtractJsonBlock(cleaned);
        }
    }

    /**
     * 提取AI输出中的Markdown内容
     */
    public String extractMarkdown(String aiOutput) {
        if (!StringUtils.hasText(aiOutput)) {
            return "";
        }

        String cleaned = aiOutput.trim();

        // 去掉Markdown代码块标记 ```markdown ... ```
        if (cleaned.startsWith("```markdown") || cleaned.startsWith("```md")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }

        return cleaned.trim();
    }

    /**
     * 验证JSON结构是否包含指定字段
     */
    public boolean validateJsonField(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.has(fieldName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试从文本中提取JSON块
     */
    private String tryExtractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = text.substring(start, end + 1);
            try {
                objectMapper.readTree(candidate);
                return candidate;
            } catch (Exception e) {
                log.warn("容错提取JSON失败");
            }
        }
        // 无法提取，返回原始内容包装为JSON
        return "{\"content\": " + safeJsonString(text) + "}";
    }

    private String safeJsonString(String text) {
        try {
            return objectMapper.writeValueAsString(text);
        } catch (Exception e) {
            return "\"" + text.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }
}

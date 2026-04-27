package com.jy.eleaitender.common.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * JSON 解析辅助 工具类
 */
public class JsonUtil {

    public static String getText(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    public static String getText(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText(defaultValue) : defaultValue;
    }

    public static int getInt(JsonNode node, String field, int defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && fieldNode.isNumber() ? fieldNode.asInt() : defaultValue;
    }

    public static BigDecimal getDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && fieldNode.isNumber()) {
            return fieldNode.decimalValue();
        }
        return null;
    }

    /**
     * 读取布尔/数值字段并转为int（0/1）
     * AI返回的isRequired可能是boolean(true/false)或int(0/1)
     */
    public static int getBooleanAsInt(JsonNode node, String field, int defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (fieldNode.isBoolean()) {
            return fieldNode.asBoolean() ? 1 : 0;
        }
        if (fieldNode.isNumber()) {
            return fieldNode.asInt();
        }
        return defaultValue;
    }

}

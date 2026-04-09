package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.util.NumberChineseUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标段列表展示辅助工具。
 * 统一修正 indexOf/indexOfDesc 并按序排序，避免不同来源数据展示不一致。
 */
public final class TenderListDisplaySupport {

    private TenderListDisplaySupport() {
    }

    /**
     * 标准化标段列表字段并按标段序号排序。
     */
    public static List<Map<String, Object>> normalize(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> normalized = new ArrayList<>(source.size());
        for (Map<String, Object> item : source) {
            Map<String, Object> current = new LinkedHashMap<>();
            if (item != null) {
                current.putAll(item);
            }
            // 外部系统可能把 indexOf 传为字符串或其他数字类型，这里统一转成 Integer。
            Integer indexOf = toInteger(current.get("indexOf"));
            if (indexOf != null) {
                current.put("indexOf", indexOf);
                if (current.get("indexOfDesc") == null) {
                    current.put("indexOfDesc", getIndexOfDesc(indexOf));
                }
            }
            normalized.add(current);
        }
        normalized.sort(Comparator
                .comparing((Map<String, Object> item) -> toInteger(item.get("indexOf")), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(item -> String.valueOf(item.get("tenderId")), Comparator.nullsLast(String::compareTo)));
        return normalized;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static String getIndexOfDesc(int indexOf) {
        if (indexOf <= 0) {
            return "标段" + indexOf;
        }
        return "标段" + NumberChineseUtil.toChineseNumber(indexOf);
    }
}

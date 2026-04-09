package com.jy.eletender.tenderdocument.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 固定步骤编码
 */
public enum TenderDocumentStepCode {
    BASIC_INFO(1, "基本信息录入"),
    PURCHASE_FILE(2, "采购文件编制"),
    BID_RECORD(3, "开标标录设置"),
    EVALUATION_RULE(4, "评审规则设置"),
    CHECK_ITEMS(5, "检查项"),
    GENERATE_PACKAGE(6, "生成文件");

    private final int stepOrder;
    private final String stepName;

    TenderDocumentStepCode(int stepOrder, String stepName) {
        this.stepOrder = stepOrder;
        this.stepName = stepName;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public static List<TenderDocumentStepCode> orderedValues() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(TenderDocumentStepCode::getStepOrder))
                .toList();
    }
}

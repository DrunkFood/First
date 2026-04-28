package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 文档填充数据项，支持 TEXT/TABLE/IMAGE/MARKDOWN 四种类型
 */
@Data
public class FillData {

    private FillType type;

    private String key;

    /** TEXT→String, TABLE→TableData, IMAGE→ImageData, MARKDOWN→String */
    private Object value;

    public static FillData text(String key, String value) {
        FillData fd = new FillData();
        fd.setType(FillType.TEXT);
        fd.setKey(key);
        fd.setValue(value);
        return fd;
    }

    public static FillData table(String key, TableData value) {
        FillData fd = new FillData();
        fd.setType(FillType.TABLE);
        fd.setKey(key);
        fd.setValue(value);
        return fd;
    }

    public static FillData image(String key, ImageData value) {
        FillData fd = new FillData();
        fd.setType(FillType.IMAGE);
        fd.setKey(key);
        fd.setValue(value);
        return fd;
    }

    public static FillData markdown(String key, String value) {
        FillData fd = new FillData();
        fd.setType(FillType.MARKDOWN);
        fd.setKey(key);
        fd.setValue(value);
        return fd;
    }
}

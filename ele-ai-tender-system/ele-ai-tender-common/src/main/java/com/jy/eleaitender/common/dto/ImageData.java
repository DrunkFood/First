package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 图片填充数据
 */
@Data
public class ImageData {

    /** 图片Base64编码（与url二选一） */
    private String base64;

    /** 图片URL（与base64二选一） */
    private String url;

    /** 宽度（EMU，0=自动） */
    private int width;

    /** 高度（EMU，0=自动） */
    private int height;
}

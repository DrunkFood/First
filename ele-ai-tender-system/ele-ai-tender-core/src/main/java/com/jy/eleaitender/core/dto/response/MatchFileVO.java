package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 匹配文件VO
 */
@Data
@Schema(description = "匹配文件")
public class MatchFileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "预算金额(元)")
    private BigDecimal budget;

    @Schema(description = "匹配度百分比(0-100)")
    private Integer matchPercent;

    @Schema(description = "匹配说明")
    private String matchDesc;

    @Schema(description = "上传时间")
    private Date uploadTime;
}

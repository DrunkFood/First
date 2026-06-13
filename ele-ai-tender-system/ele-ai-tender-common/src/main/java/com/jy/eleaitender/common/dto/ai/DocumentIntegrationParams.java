package com.jy.eleaitender.common.dto.ai;

import com.jy.eleaitender.common.dto.FillData;
import lombok.Data;

import java.util.List;

/**
 * 文档集成任务参数
 */
@Data
public class DocumentIntegrationParams {

    private Long templateFileId;

    private String projectName;

    private List<FillData> fillDataList;
}

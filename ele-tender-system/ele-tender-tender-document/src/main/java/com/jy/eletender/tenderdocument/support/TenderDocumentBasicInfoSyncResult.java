package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TenderDocumentBasicInfoSyncResult {

    /**
     * 电子标系统根据 projectType 统一推导的编制粒度控制字段。
     * 取值约定：PROJECT=按项目编制，TENDER=按标段编制。
     */
    private String compileScope;

    /**
     * 业务系统原始项目类型，电子标系统据此推导 compileScope。
     */
    private InteractionProjectType projectType;
    private String projectCode;
    private String projectName;
    private String purchaseMethod;

    /**
     * 评标办法枚举，取值见 InteractionEvalMethod。
     */
    private InteractionEvalMethod evalMethod;
    private Map<String, Object> projectInfo;
    private List<Map<String, Object>> tenderList = new ArrayList<>();
}

package com.jy.eletender.common.interaction.dto;

import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目基本信息响应
 */
@Data
public class ProjectBasicInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String projectId;

    private String projectNo;

    private String projectName;

    /**
     * 项目类型，保留业务系统原始语义，例如 PUBLIC / INVITE。
     */
    private InteractionProjectType projectType;

    /**
     * 采购方式原始展示值，例如“公开招标”“邀请招标”。
     * 电子标系统会直接落库并用于页面展示，不再自行推导。
     */
    private String purchaseMethod;

    /**
     * 评标办法枚举名，例如 LOWEST_PRICE / COMPREHENSIVE_SCORE。
     * 电子标系统会直接使用该值初始化评审规则链路。
     */
    private InteractionEvalMethod evalMethod;

    /**
     * 采购单位名称。
     */
    private String purchaserName;

    /**
     * 投标截止时间，格式 yyyy-MM-dd HH:mm:ss。
     */
    private String bidEndTime;

    /**
     * 项目下全部可编制标段。
     * PROJECT 场景返回项目下全部标段，TENDER 场景通常只返回当前标段。
     */
    private List<ProjectTenderInfo> tenderList = new ArrayList<ProjectTenderInfo>();

}

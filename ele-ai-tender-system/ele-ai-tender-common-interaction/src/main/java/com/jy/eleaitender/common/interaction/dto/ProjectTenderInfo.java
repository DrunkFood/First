package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 项目下标段的基础信息。
 */
@Data
public class ProjectTenderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tenderId;

    /**
     * 标段序号，对应业务系统来源字段 indexOf。
     */
    private Integer indexOf;

    /**
     * 标段序号中文描述，对应业务系统来源字段 indexOfDesc。
     */
    private String indexOfDesc;

    private String tenderNo;

    private String tenderName;

    /**
     * 标段标的金额，对应业务系统来源字段 tender_amount。
     */
    private BigDecimal tenderAmount;

    /**
     * 标段采购内容，对应业务系统来源字段 purchaseContext。
     */
    private String purchaseContext;

}

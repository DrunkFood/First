package com.jy.eletender.tenderdocument.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentRuleTreeNode {

    private String id;

    /**
     * 同层级内从 1 开始的排序号，后端落库到 sortNo。
     */
    private Integer order;

    /**
     * 前端渲染树节点用的 key，后端不持久化，仅原样透传时保留。
     */
    private String key;

    /**
     * 前端字段名叫 name，后端持久化时映射为 itemContent。
     */
    private String name;

    @JsonProperty("isPass")
    /**
     * 前端布尔字段，表示当前节点是否按通过制处理。
     * 当前仅作前端展示兼容字段，后端不会依赖该字段做决策。
     */
    private Boolean pass;

    /**
     * 表示当前节点的得分。
     * 当前仅作前端展示兼容字段，后端不会依赖该字段做决策。
     */
    private BigDecimal score;

    /**
     * 表示当前节点的对应的pdf页码, 可以一对多.
     * 当前仅作前端展示兼容字段，后端不会依赖该字段做决策。
     */
    private List<Integer> pages;

    private BigDecimal lowest;
    private BigDecimal highest;

    /**
     * 前端评分标准文本，后端落库到 scoreStandard。
     */
    private String standard;

    @JsonProperty("isParent")
    /**
     * 前端展示字段，后端不会直接信任，而是根据 children 是否为空重新判断 leafFlag。
     */
    private Boolean parent;

    /**
     * 主客观分属性，取值见 TenderDocumentScoreAttribute。
     * 仅打分制叶子节点有意义。
     */
    private String objectiveType;

    /**
     * 前端页面树结构。后端保存时递归拆平，查询时再组装回 children。
     */
    private List<TenderDocumentRuleTreeNode> children = new ArrayList<>();
}

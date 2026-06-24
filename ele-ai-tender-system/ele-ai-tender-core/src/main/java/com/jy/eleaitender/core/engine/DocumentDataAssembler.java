package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.dto.*;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.entity.core.TbProjectTemplate;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.enums.ReviewType;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import com.jy.eleaitender.core.service.IProjectTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档数据组装器
 * 收集项目基础信息、需求内容、评审项数据，组装为文档模板所需的 FillData 列表
 */
@Slf4j
@Component
public class DocumentDataAssembler {

   private static final Map<String, String> typeLabels = Map.of(
            ReviewType.CREDIT.getCode(), "资信标",
            ReviewType.TECHNICAL.getCode(), "技术标",
            ReviewType.COMMERCIAL.getCode(), "商务标"
    );

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private TbProjectReviewItemMapper reviewItemMapper;

    @Autowired
    private IProjectTemplateService projectTemplateService;

    /**
     * 组装文档数据（新接口，返回结构化 FillData 列表）
     */
    public List<FillData> assemble(Long projectId) {
        TbProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }

        List<FillData> fillDataList = new ArrayList<>();

        // 项目基础信息（TEXT 类型）
        fillDataList.add(FillData.text("projectCode", nullSafe(project.getProjectCode()), "项目编号"));
        fillDataList.add(FillData.text("projectName", nullSafe(project.getProjectName()), "项目名称"));
        fillDataList.add(FillData.text("projectCategory", nullSafe(project.getProjectCategory()), "项目类别"));
        fillDataList.add(FillData.text("projectType", nullSafe(project.getProjectType()), "项目类型"));
        fillDataList.add(FillData.text("budget", project.getBudget() != null ? project.getBudget().toPlainString() : "", "预算金额"));
        fillDataList.add(FillData.text("tenderUnit", nullSafe(project.getTenderUnit()), "招标单位"));
        fillDataList.add(FillData.text("projectLocation", nullSafe(project.getProjectLocation()), "项目地点"));
        fillDataList.add(FillData.text("contactPerson", nullSafe(project.getContactPerson()), "联系人"));
        fillDataList.add(FillData.text("contactPhone", nullSafe(project.getContactPhone()), "联系电话"));
        fillDataList.add(FillData.text("projectDescription", nullSafe(project.getProjectDescription()), "项目基本情况描述"));
        fillDataList.add(FillData.markdown("requirementContent", nullSafe(project.getRequirementContent()), "招标需求内容"));
        fillDataList.add(FillData.text("reviewType", nullSafe(project.getReviewType()), "评审类型"));

        // 加载评审项配置（用于控制汇总表是否输出主观/客观列）
        ReviewConfig reviewConfig = null;
        try {
            TbProjectTemplate projectTemplate = projectTemplateService.getByProjectId(projectId);
            if (projectTemplate != null && projectTemplate.getReviewConfig() != null) {
                reviewConfig = ReviewConfig.fromJson(projectTemplate.getReviewConfig());
            }
        } catch (Exception e) {
            log.warn("加载reviewConfig失败，回退硬编码: {}", e.getMessage());
        }

        // 评审项（按类型分组）
        List<TbProjectReviewItem> reviewItems = reviewItemMapper.selectByProjectId(projectId);
        Map<String, List<TbProjectReviewItem>> grouped = reviewItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getReviewType() != null ? item.getReviewType() : "COMPLIANCE",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 符合性审查项列表（MARKDOWN，忽略一级节点，二级+三级列表展示）
        List<TbProjectReviewItem> complianceItems = grouped.getOrDefault("COMPLIANCE", Collections.emptyList());
        fillDataList.add(FillData.markdown("complianceItems",
                buildComplianceMarkdown(complianceItems), "符合性审查项列表"));

        // 评审项汇总表格
        fillDataList.add(FillData.table("allReviewItems",
                buildReviewSummaryTable(grouped, reviewConfig), "评审项汇总表格"));

        return fillDataList;
    }

    // ==================== TABLE 数据构建 ====================

    /**
     * 构建评审汇总表数据（方式一：资信标→技术标→商务标，含合并规则）
     */
    private TableData buildReviewSummaryTable(Map<String, List<TbProjectReviewItem>> grouped, ReviewConfig reviewConfig) {
        TableData tableData = new TableData();
        tableData.setColumns(List.of(
                new ColumnDef("categoryName", "类别"),
                new ColumnDef("reviewStandard", "评审标准"),
                new ColumnDef("maxScore", "最高分值"),
                new ColumnDef("subjectivity", "主观分/客观分属性"),
                new ColumnDef("responseFileCatalog", "响应文件中评审标准相应的资信、技术资料目录")
        ));
        tableData.setRows(toReviewSummaryList(grouped, reviewConfig));
        // 第一列（categoryName）按相同文本合并
        tableData.setMergeRules(List.of(
                new MergeRule(0, MergeStrategy.BY_SAME_TEXT)
        ));
        return tableData;
    }

    // ==================== 符合性审查 MARKDOWN 构建 ====================

    /**
     * 构建符合性审查项 Markdown（列表形式，忽略一级节点）
     * 格式：
     * - 二级项名称
     * - 三级项内容
     * - 三级项内容
     */
    private String buildComplianceMarkdown(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) return "";

        Map<Long, List<TbProjectReviewItem>> childrenMap = items.stream()
                .filter(i -> i.getParentId() != null && i.getParentId() > 0)
                .collect(Collectors.groupingBy(TbProjectReviewItem::getParentId,
                        LinkedHashMap::new, Collectors.toList()));

        List<TbProjectReviewItem> roots = items.stream()
                .filter(i -> i.getParentId() == null || i.getParentId() == 0)
                .sorted(Comparator.comparingInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0))
                .toList();

        StringBuilder sb = new StringBuilder();
        for (TbProjectReviewItem root : roots) {
            // 一级节点跳过，直接遍历其子节点（二级项）
            List<TbProjectReviewItem> level2Items = childrenMap.getOrDefault(root.getId(), Collections.emptyList());
            for (TbProjectReviewItem level2 : level2Items) {
                sb.append("- ").append(nullSafe(level2.getItemStandard()));
                sb.append("\n");
                // 三级项
                List<TbProjectReviewItem> level3Items = childrenMap.getOrDefault(level2.getId(), Collections.emptyList());
                for (TbProjectReviewItem level3 : level3Items) {
                    sb.append("  - ").append(nullSafe(level3.getItemStandard()));
                    sb.append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    // ==================== 数据转换 ====================

    List<Map<String, String>> toReviewSummaryList(Map<String, List<TbProjectReviewItem>> grouped, ReviewConfig reviewConfig) {
        List<Map<String, String>> result = new ArrayList<>();
        boolean weightMode = reviewConfig != null && reviewConfig.isWeightMode();

        for (String reviewType : typeLabels.keySet()) {
            List<TbProjectReviewItem> items = grouped.getOrDefault(reviewType, Collections.emptyList());
            if (items.isEmpty()) continue;

            boolean hasSubjectivity = reviewConfig != null
                    ? reviewConfig.isDistinguishSubjectivity(reviewType)
                    : (ReviewType.CREDIT.getCode().equals(reviewType) || ReviewType.TECHNICAL.getCode().equals(reviewType));

            // 分类总分：取所有叶子节点 score 之和（SCORE模式用于类别标签）
            Set<Long> parentIds = items.stream()
                    .map(TbProjectReviewItem::getParentId)
                    .filter(pid -> pid != null && pid > 0)
                    .collect(Collectors.toSet());
            BigDecimal categoryScore = items.stream()
                    .filter(i -> !parentIds.contains(i.getId()))
                    .map(TbProjectReviewItem::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // SCORE模式：技术标（100分）；WEIGHT模式：类别标签在roots循环里按一级节点权重%拼接
            String categoryLabel = typeLabels.get(reviewType) + "（" + categoryScore.toPlainString() + "分）";

            Map<Long, List<TbProjectReviewItem>> childrenMap = items.stream()
                    .filter(i -> i.getParentId() != null && i.getParentId() > 0)
                    .collect(Collectors.groupingBy(TbProjectReviewItem::getParentId,
                            LinkedHashMap::new, Collectors.toList()));

            List<TbProjectReviewItem> roots = items.stream()
                    .filter(i -> i.getParentId() == null || i.getParentId() == 0)
                    .sorted(Comparator.comparingInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0))
                    .toList();

            for (TbProjectReviewItem root : roots) {
                String label = weightMode && root.getWeight() != null
                        ? typeLabels.get(reviewType) + "（权重" + root.getWeight().toPlainString() + "%）"
                        : categoryLabel;
                flattenForSummary(root, label, hasSubjectivity, childrenMap, result);
            }
        }
        return result;
    }

    private void flattenForSummary(TbProjectReviewItem node, String categoryLabel,
                                   boolean hasSubjectivity,
                                   Map<Long, List<TbProjectReviewItem>> childrenMap,
                                   List<Map<String, String>> result) {
        boolean hasChildren = childrenMap.containsKey(node.getId()) && !childrenMap.get(node.getId()).isEmpty();

        if (hasChildren) {
            // 有子节点时跳过自身行，避免与类别列重复分组
            for (TbProjectReviewItem child : childrenMap.get(node.getId())) {
                flattenForSummary(child, categoryLabel, hasSubjectivity, childrenMap, result);
            }
        } else {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("categoryName", categoryLabel);
            map.put("reviewStandard", node.getItemStandard());
            map.put("maxScore", resolveMaxScore(node, false, childrenMap));
            map.put("subjectivity", hasSubjectivity ? formatSubjectivity(node.getSubjectivity()) : "");
            map.put("responseFileCatalog", "");
            result.add(map);
        }
    }

    /**
     * 解析节点的满分值：叶子节点取 score，父节点取子节点 score 之和
     */
    private String resolveMaxScore(TbProjectReviewItem node, boolean hasChildren,
                                   Map<Long, List<TbProjectReviewItem>> childrenMap) {
        if (hasChildren) {
            List<TbProjectReviewItem> children = childrenMap.get(node.getId());
            BigDecimal sum = children.stream()
                    .map(TbProjectReviewItem::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.compareTo(BigDecimal.ZERO) > 0 ? sum.toPlainString() : "";
        }
        return node.getScore() != null ? node.getScore().toPlainString() : "";
    }

    private String formatSubjectivity(String subjectivity) {
        if (subjectivity == null) return "";
        return switch (subjectivity) {
            case "OBJECTIVE" -> "客观";
            case "SUBJECTIVE" -> "主观";
            default -> "";
        };
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}

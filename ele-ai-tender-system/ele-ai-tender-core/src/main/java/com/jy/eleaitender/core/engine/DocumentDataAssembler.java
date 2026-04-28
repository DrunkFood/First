package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.dto.*;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
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

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private TbProjectReviewItemMapper reviewItemMapper;

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
        fillDataList.add(FillData.text("projectName", nullSafe(project.getProjectName())));
        fillDataList.add(FillData.text("projectCode", nullSafe(project.getProjectCode())));
        fillDataList.add(FillData.text("projectCategory", nullSafe(project.getProjectCategory())));
        fillDataList.add(FillData.text("projectType", nullSafe(project.getProjectType())));
        fillDataList.add(FillData.text("budget", project.getBudget() != null ? project.getBudget().toPlainString() : ""));
        fillDataList.add(FillData.text("tenderUnit", nullSafe(project.getTenderUnit())));
        fillDataList.add(FillData.text("projectLocation", nullSafe(project.getProjectLocation())));
        fillDataList.add(FillData.text("contactPerson", nullSafe(project.getContactPerson())));
        fillDataList.add(FillData.text("contactPhone", nullSafe(project.getContactPhone())));
        fillDataList.add(FillData.text("projectDescription", nullSafe(project.getProjectDescription())));
        fillDataList.add(FillData.markdown("requirementContent", nullSafe(project.getRequirementContent())));
        fillDataList.add(FillData.text("reviewType", nullSafe(project.getReviewType())));

        // 评审项（按类型分组）
        List<TbProjectReviewItem> reviewItems = reviewItemMapper.selectByProjectId(projectId);
        Map<String, List<TbProjectReviewItem>> grouped = reviewItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getReviewType() != null ? item.getReviewType() : "COMPLIANCE",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 各类型评审项表格（TABLE 类型）
        fillDataList.add(FillData.table("complianceItems",
                buildReviewItemTable(grouped.getOrDefault("COMPLIANCE", Collections.emptyList()))));
        fillDataList.add(FillData.table("technicalItems",
                buildReviewItemTable(grouped.getOrDefault("TECHNICAL", Collections.emptyList()))));
        fillDataList.add(FillData.table("creditItems",
                buildReviewItemTable(grouped.getOrDefault("CREDIT", Collections.emptyList()))));
        fillDataList.add(FillData.table("commercialItems",
                buildReviewItemTable(grouped.getOrDefault("COMMERCIAL", Collections.emptyList()))));

        // 方式一：评审汇总表（TABLE 类型，含单元格合并规则）
        fillDataList.add(FillData.table("allReviewItems", buildReviewSummaryTable(grouped)));

        return fillDataList;
    }

    // ==================== TABLE 数据构建 ====================

    /**
     * 构建评审项表格数据（各类型独立表格）
     */
    private TableData buildReviewItemTable(List<TbProjectReviewItem> items) {
        TableData tableData = new TableData();
        tableData.setColumns(List.of(
                new ColumnDef("serialNo", "序号"),
                new ColumnDef("itemName", "评审项名称"),
                new ColumnDef("itemContent", "评审项内容"),
                new ColumnDef("score", "分值"),
                new ColumnDef("maxScore", "满分"),
                new ColumnDef("weight", "权重"),
                new ColumnDef("subjectivity", "主观/客观"),
                new ColumnDef("isRequired", "是否必审")
        ));
        tableData.setRows(toFlatList(items));
        tableData.setMergeRules(Collections.emptyList());
        return tableData;
    }

    /**
     * 构建评审汇总表数据（方式一：资信标→技术标→商务标，含合并规则）
     */
    private TableData buildReviewSummaryTable(Map<String, List<TbProjectReviewItem>> grouped) {
        TableData tableData = new TableData();
        tableData.setColumns(List.of(
                new ColumnDef("categoryName", "类别"),
                new ColumnDef("reviewStandard", "评审标准"),
                new ColumnDef("maxScore", "分值"),
                new ColumnDef("subjectivity", "主观/客观")
        ));
        tableData.setRows(toReviewSummaryList(grouped));
        // 第一列（categoryName）按相同文本合并
        tableData.setMergeRules(List.of(
                new MergeRule(0, MergeStrategy.BY_SAME_TEXT)
        ));
        return tableData;
    }

    // ==================== 数据转换 ====================

    /**
     * 将评审项实体列表转为扁平化的Map列表，递归构建树形结构并生成多级序号
     */
    private List<Map<String, String>> toFlatList(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<TbProjectReviewItem>> childrenMap = items.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(
                        TbProjectReviewItem::getParentId,
                        LinkedHashMap::new, Collectors.toList()));

        List<TbProjectReviewItem> roots = items.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0)
                .sorted(Comparator.comparingInt(item -> item.getSortOrder() != null ? item.getSortOrder() : 0))
                .toList();

        List<Map<String, String>> result = new ArrayList<>();
        int[] rootCounter = {1};
        for (TbProjectReviewItem root : roots) {
            flattenNode(root, String.valueOf(rootCounter[0]), 1, childrenMap, result);
            rootCounter[0]++;
        }
        return result;
    }

    private void flattenNode(TbProjectReviewItem node, String serialNo, int depth,
                             Map<Long, List<TbProjectReviewItem>> childrenMap,
                             List<Map<String, String>> result) {
        boolean hasChildren = childrenMap.containsKey(node.getId())
                && !childrenMap.get(node.getId()).isEmpty();

        Map<String, String> map = new LinkedHashMap<>();
        map.put("serialNo", serialNo);
        map.put("itemName", nullSafe(node.getItemName()));

        if (hasChildren) {
            map.put("itemContent", "");
            map.put("score", "");
            // maxScore 为空时取子节点 score 之和
            String maxScoreVal = resolveMaxScore(node, hasChildren, childrenMap);
            map.put("maxScore", maxScoreVal);
            map.put("weight", node.getWeight() != null ? node.getWeight().toPlainString() + "%" : "");
            map.put("subjectivity", "");
            map.put("isRequired", "");
        } else {
            map.put("itemContent", nullSafe(node.getItemContent()));
            map.put("score", node.getScore() != null ? node.getScore().toPlainString() : "");
            // maxScore 为空时取 score 兜底
            map.put("maxScore", resolveMaxScore(node, false, childrenMap));
            map.put("weight", node.getWeight() != null ? node.getWeight().toPlainString() + "%" : "");
            map.put("subjectivity", formatSubjectivity(node.getSubjectivity()));
            map.put("isRequired", formatRequired(node.getIsRequired()));
        }
        result.add(map);

        if (hasChildren) {
            List<TbProjectReviewItem> children = childrenMap.get(node.getId());
            int childIndex = 1;
            for (TbProjectReviewItem child : children) {
                flattenNode(child, serialNo + "." + childIndex, depth + 1, childrenMap, result);
                childIndex++;
            }
        }
    }

    private List<Map<String, String>> toReviewSummaryList(Map<String, List<TbProjectReviewItem>> grouped) {
        List<Map<String, String>> result = new ArrayList<>();

        List<String> orderedTypes = List.of("CREDIT", "TECHNICAL", "COMMERCIAL");
        Map<String, String> typeLabels = Map.of(
            "CREDIT", "资信标", "TECHNICAL", "技术标", "COMMERCIAL", "商务标");

        for (String reviewType : orderedTypes) {
            List<TbProjectReviewItem> items = grouped.getOrDefault(reviewType, Collections.emptyList());
            if (items.isEmpty()) continue;

            // 分类总分：取所有叶子节点 score 之和
            Set<Long> parentIds = items.stream()
                .map(TbProjectReviewItem::getParentId)
                .filter(pid -> pid != null && pid > 0)
                .collect(Collectors.toSet());
            BigDecimal categoryScore = items.stream()
                .filter(i -> !parentIds.contains(i.getId()))
                .map(TbProjectReviewItem::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 若叶子 score 都为空，回退到根节点 maxScore 之和
            if (categoryScore.compareTo(BigDecimal.ZERO) == 0) {
                categoryScore = items.stream()
                    .filter(i -> i.getParentId() == null || i.getParentId() == 0)
                    .map(TbProjectReviewItem::getMaxScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

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
                flattenForSummary(root, categoryLabel, childrenMap, result);
            }
        }
        return result;
    }

    private void flattenForSummary(TbProjectReviewItem node, String categoryLabel,
            Map<Long, List<TbProjectReviewItem>> childrenMap,
            List<Map<String, String>> result) {
        boolean hasChildren = childrenMap.containsKey(node.getId())
            && !childrenMap.get(node.getId()).isEmpty();

        if (hasChildren) {
            // 有子节点时跳过自身行，避免与类别列重复分组
            for (TbProjectReviewItem child : childrenMap.get(node.getId())) {
                flattenForSummary(child, categoryLabel, childrenMap, result);
            }
        } else {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("categoryName", categoryLabel);
            map.put("reviewStandard", node.getItemStandard());
            map.put("maxScore", resolveMaxScore(node, false, childrenMap));
            map.put("subjectivity", formatSubjectivity(node.getSubjectivity()));
            result.add(map);
        }
    }

    /**
     * 解析节点的满分值：maxScore 有值直接取，否则叶子节点取 score，父节点取子节点 score 之和
     */
    private String resolveMaxScore(TbProjectReviewItem node, boolean hasChildren,
                                   Map<Long, List<TbProjectReviewItem>> childrenMap) {
        if (node.getMaxScore() != null) {
            return node.getMaxScore().toPlainString();
        }
        if (hasChildren) {
            List<TbProjectReviewItem> children = childrenMap.get(node.getId());
            BigDecimal sum = children.stream()
                    .map(TbProjectReviewItem::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.compareTo(BigDecimal.ZERO) > 0 ? sum.toPlainString() : "";
        }
        // 叶子节点：maxScore 为空时取 score 兜底
        return node.getScore() != null ? node.getScore().toPlainString() : "";
    }

    private String formatSubjectivity(String subjectivity) {
        if (subjectivity == null) return "";
        return switch (subjectivity) {
            case "OBJECTIVE" -> "客观";
            case "SUBJECTIVE" -> "主观";
            default -> subjectivity;
        };
    }

    private String formatRequired(Integer isRequired) {
        if (isRequired == null) return "";
        return isRequired == 1 ? "是" : "否";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}

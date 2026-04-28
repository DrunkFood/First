package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档数据组装器
 * 收集项目基础信息、需求内容、评审项数据，组装为文档模板所需的数据模型
 */
@Slf4j
@Component
public class DocumentDataAssembler {

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private TbProjectReviewItemMapper reviewItemMapper;

    /**
     * 组装文档数据
     */
    public Map<String, Object> assemble(Long projectId) {
        TbProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }

        Map<String, Object> data = new LinkedHashMap<>();

        // 项目基础信息
        data.put("projectName", project.getProjectName());
        data.put("projectCode", project.getProjectCode());
        data.put("projectCategory", project.getProjectCategory());
        data.put("projectType", project.getProjectType());
        data.put("budget", project.getBudget() != null ? project.getBudget().toPlainString() : "");
        data.put("tenderUnit", project.getTenderUnit());
        data.put("projectLocation", project.getProjectLocation());
        data.put("contactPerson", project.getContactPerson());
        data.put("contactPhone", project.getContactPhone());
        data.put("projectDescription", project.getProjectDescription());
        data.put("requirementContent", project.getRequirementContent());
        data.put("reviewType", project.getReviewType());

        // 评审项（按类型分组，列表提升到顶层以便poi-tl {{#list}} 访问）
        List<TbProjectReviewItem> reviewItems = reviewItemMapper.selectByProjectId(projectId);
        Map<String, List<TbProjectReviewItem>> grouped = reviewItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getReviewType() != null ? item.getReviewType() : "COMPLIANCE",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        data.put("complianceItems", toFlatList(grouped.getOrDefault("COMPLIANCE", Collections.emptyList())));
        data.put("technicalItems", toFlatList(grouped.getOrDefault("TECHNICAL", Collections.emptyList())));
        data.put("creditItems", toFlatList(grouped.getOrDefault("CREDIT", Collections.emptyList())));
        data.put("commercialItems", toFlatList(grouped.getOrDefault("COMMERCIAL", Collections.emptyList())));

        return data;
    }

    /**
     * 将评审项实体列表转为扁平化的Map列表，递归构建树形结构并生成多级序号
     * 序号格式: 1 → 1.1 → 1.1.1 → ...，层级越深缩进越多
     * 非叶子节点(分类行): 有maxScore/weight，无itemContent/score/subjectivity/isRequired
     * 叶子节点(具体项): 所有字段完整填充
     */
    private List<Map<String, String>> toFlatList(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 parentId -> children 映射（保持sortOrder顺序）
        Map<Long, List<TbProjectReviewItem>> childrenMap = items.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(
                        TbProjectReviewItem::getParentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 筛选根节点，按sortOrder排序
        List<TbProjectReviewItem> roots = items.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0)
                .sorted(Comparator.comparingInt(item -> item.getSortOrder() != null ? item.getSortOrder() : 0))
                .toList();

        // 递归前序遍历
        List<Map<String, String>> result = new ArrayList<>();
        int[] rootCounter = {1};
        for (TbProjectReviewItem root : roots) {
            flattenNode(root, String.valueOf(rootCounter[0]), 1, childrenMap, result);
            rootCounter[0]++;
        }
        return result;
    }

    /**
     * 递归扁平化单个节点及其子树
     *
     * @param node        当前节点
     * @param serialNo    当前节点序号（如 "1"、"1.2"、"1.2.3"）
     * @param depth       当前深度（1=根）
     * @param childrenMap parentId -> children 映射
     * @param result      累积结果列表
     */
    private void flattenNode(TbProjectReviewItem node, String serialNo, int depth,
                             Map<Long, List<TbProjectReviewItem>> childrenMap,
                             List<Map<String, String>> result) {
        boolean hasChildren = childrenMap.containsKey(node.getId())
                && !childrenMap.get(node.getId()).isEmpty();

        Map<String, String> map = new LinkedHashMap<>();
        map.put("serialNo", serialNo);
        map.put("itemName", indent(depth - 1) + (node.getItemName() != null ? node.getItemName() : ""));

        if (hasChildren) {
            // 非叶子节点(分类行): 只填充分类维度字段
            map.put("itemContent", "");
            map.put("score", "");
            map.put("maxScore", node.getMaxScore() != null ? node.getMaxScore().toPlainString() : "");
            map.put("weight", node.getWeight() != null ? node.getWeight().toPlainString() + "%" : "");
            map.put("subjectivity", "");
            map.put("isRequired", "");
        } else {
            // 叶子节点(具体项): 完整填充
            map.put("itemContent", node.getItemContent() != null ? node.getItemContent() : "");
            map.put("score", node.getScore() != null ? node.getScore().toPlainString() : "");
            map.put("maxScore", node.getMaxScore() != null ? node.getMaxScore().toPlainString() : "");
            map.put("weight", node.getWeight() != null ? node.getWeight().toPlainString() + "%" : "");
            map.put("subjectivity", formatSubjectivity(node.getSubjectivity()));
            map.put("isRequired", formatRequired(node.getIsRequired()));
        }
        result.add(map);

        // 递归处理子节点
        if (hasChildren) {
            List<TbProjectReviewItem> children = childrenMap.get(node.getId());
            int childIndex = 1;
            for (TbProjectReviewItem child : children) {
                flattenNode(child, serialNo + "." + childIndex, depth + 1, childrenMap, result);
                childIndex++;
            }
        }
    }

    private String indent(int level) {
        return "　　".repeat(Math.max(0, level));
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

}

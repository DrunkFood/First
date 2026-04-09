package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.entity.TenderRuleNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评审规则树组装器。
 * 将数据库平铺节点按层级与父子关系组装成前端可直接渲染的树结构。
 */
@Component
public class TenderDocumentRuleTreeAssembler {

    /**
     * 从平铺节点构建树形结构并按显示顺序排序。
     */
    public List<TenderDocumentRuleTreeNode> buildTree(List<TenderRuleNode> nodes) {
        Map<Long, TenderDocumentRuleTreeNode> nodeMap = new LinkedHashMap<>();
        // 第一趟先创建全部节点并保留顺序，确保第二趟挂载子节点时父节点可被直接命中。
        for (TenderRuleNode node : nodes.stream()
                .sorted(Comparator.comparing(TenderRuleNode::getLevel).thenComparing(TenderRuleNode::getSortNo))
                .toList()) {
            TenderDocumentRuleTreeNode treeNode = new TenderDocumentRuleTreeNode();
            treeNode.setId(node.getId() == null ? null : String.valueOf(node.getId()));
            treeNode.setOrder(node.getSortNo());
            treeNode.setKey(String.valueOf(node.getSortNo()));
            treeNode.setName(node.getItemContent());
            treeNode.setLowest(node.getScoreMin());
            treeNode.setHighest(node.getScoreMax());
            treeNode.setStandard(node.getScoreStandard());
            treeNode.setParent(node.getLeafFlag() != null && node.getLeafFlag() == 0);
            treeNode.setObjectiveType(node.getScoreAttribute());
            nodeMap.put(node.getId(), treeNode);
        }

        List<TenderDocumentRuleTreeNode> roots = new ArrayList<>();
        // 第二趟根据 parentId 建立父子关系，根节点单独收集用于页面首层展示。
        for (TenderRuleNode node : nodes) {
            TenderDocumentRuleTreeNode treeNode = nodeMap.get(node.getId());
            if (node.getParentId() == null) {
                roots.add(treeNode);
            } else {
                TenderDocumentRuleTreeNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(treeNode);
                }
            }
        }
        return roots.stream()
                .sorted(Comparator.comparing(TenderDocumentRuleTreeNode::getOrder))
                .collect(Collectors.toList());
    }
}

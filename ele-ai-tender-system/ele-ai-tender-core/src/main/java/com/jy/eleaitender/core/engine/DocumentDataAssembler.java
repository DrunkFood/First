package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbRequirementMapper;
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

        // 评审项（按类型分组）
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
        data.put("reviewType", project.getReviewType());

        return data;
    }

    /**
     * 将评审项实体列表转为扁平化的Map列表，兼容poi-tl模板渲染
     */
    private List<Map<String, String>> toFlatList(List<TbProjectReviewItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(item -> {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("itemName", item.getItemName() != null ? item.getItemName() : "");
            map.put("itemContent", item.getItemContent() != null ? item.getItemContent() : "");
            map.put("score", item.getScore() != null ? String.valueOf(item.getScore()) : "");
            map.put("level", item.getLevel() != null ? String.valueOf(item.getLevel()) : "1");
            return map;
        }).toList();
    }

}

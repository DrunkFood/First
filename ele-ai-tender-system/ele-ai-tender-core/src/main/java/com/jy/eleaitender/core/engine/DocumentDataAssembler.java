package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbRequirementMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档数据组装器
 * 收集项目基础信息、需求内容、评审项数据，组装为文档模板所需的数据模型
 */
@Component
public class DocumentDataAssembler {

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private TbRequirementMapper requirementMapper;

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
        data.put("budget", project.getBudget());
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

        data.put("complianceItems", grouped.getOrDefault("COMPLIANCE", Collections.emptyList()));
        data.put("technicalItems", grouped.getOrDefault("TECHNICAL", Collections.emptyList()));
        data.put("creditItems", grouped.getOrDefault("CREDIT", Collections.emptyList()));
        data.put("commercialItems", grouped.getOrDefault("COMMERCIAL", Collections.emptyList()));
        data.put("reviewType", project.getReviewType());

        return data;
    }

}

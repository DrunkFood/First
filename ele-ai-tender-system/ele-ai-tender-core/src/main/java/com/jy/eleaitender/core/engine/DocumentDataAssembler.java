package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.entity.core.AiReviewItem;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.mapper.AiReviewItemMapper;
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
    private AiProjectMapper projectMapper;

    @Autowired
    private AiRequirementMapper requirementMapper;

    @Autowired
    private AiReviewItemMapper reviewItemMapper;

    /**
     * 组装文档数据
     */
    public Map<String, Object> assemble(Long projectId) {
        AiProject project = projectMapper.selectById(projectId);
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

        // 需求内容
        String requirementContent = assembleRequirementContent(project);
        data.put("requirementContent", requirementContent);

        // 评审项（按类型分组）
        List<AiReviewItem> reviewItems = reviewItemMapper.selectByProjectId(projectId);
        Map<String, List<AiReviewItem>> grouped = reviewItems.stream()
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

    /**
     * 组装需求内容
     */
    private String assembleRequirementContent(AiProject project) {
        // 优先使用项目关联的需求
        if (project.getRequirementId() != null) {
            AiRequirement requirement = requirementMapper.selectById(project.getRequirementId());
            if (requirement != null && requirement.getContent() != null) {
                return requirement.getContent();
            }
        }
        // 回退到项目自身的需求内容
        return project.getRequirementContent() != null ? project.getRequirementContent() : "";
    }
}

package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.DocumentPreviewVO;
import com.jy.eleaitender.core.engine.DocumentDataAssembler;
import com.jy.eleaitender.core.engine.MarkdownTemplateEngine;
import com.jy.eleaitender.core.engine.WordDocumentGenerator;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.service.IDocumentIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 文档集成服务实现
 */
@Slf4j
@Service
public class DocumentIntegrationServiceImpl implements IDocumentIntegrationService {

    private static final String BIZ_TYPE_TENDER_DOC = "tender-document";

    @Autowired
    private AiProjectMapper projectMapper;

    @Autowired
    private DocumentDataAssembler dataAssembler;

    @Autowired
    private MarkdownTemplateEngine markdownEngine;

    @Autowired
    private WordDocumentGenerator wordGenerator;

    @Autowired
    private InternalFileServiceClient fileServiceClient;

    @Override
    @Transactional
    public DocumentPreviewVO integrate(Long projectId) {
        AiProject project = getProjectOrThrow(projectId);

        // 1. 组装文档数据
        Map<String, Object> data = dataAssembler.assemble(projectId);

        // 2. 构建完整的Markdown文档
        String markdown = buildMarkdownDocument(data);

        // 3. 转换为HTML预览
        String html = markdownEngine.markdownToHtml(markdown);

        // 4. 生成Word文档并上传到文件服务
        Long generatedFileId = generateAndUploadWord(project, data, html);

        // 5. 更新项目：存储Markdown内容 + generatedFileId
        project.setRequirementContent(markdown);
        project.setGeneratedFileId(generatedFileId);
        projectMapper.updateById(project);

        log.info("文档集成完成，项目ID: {}, 生成文件ID: {}", projectId, generatedFileId);

        DocumentPreviewVO vo = new DocumentPreviewVO();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getProjectName());
        vo.setHtmlContent(html);
        vo.setMarkdownContent(markdown);
        vo.setIntegrated(true);
        vo.setGeneratedFileId(generatedFileId);
        return vo;
    }

    @Override
    public DocumentPreviewVO getPreview(Long projectId) {
        AiProject project = getProjectOrThrow(projectId);

        String markdown = project.getRequirementContent();
        if (!StringUtils.hasText(markdown)) {
            DocumentPreviewVO vo = new DocumentPreviewVO();
            vo.setProjectId(projectId);
            vo.setProjectName(project.getProjectName());
            vo.setHtmlContent("");
            vo.setMarkdownContent("");
            vo.setIntegrated(false);
            vo.setGeneratedFileId(null);
            return vo;
        }

        String html = markdownEngine.markdownToHtml(markdown);
        DocumentPreviewVO vo = new DocumentPreviewVO();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getProjectName());
        vo.setHtmlContent(html);
        vo.setMarkdownContent(markdown);
        vo.setIntegrated(true);
        vo.setGeneratedFileId(project.getGeneratedFileId());
        return vo;
    }

    @Override
    @Transactional
    public void editContent(Long projectId, String markdownContent) {
        AiProject project = getProjectOrThrow(projectId);
        project.setRequirementContent(markdownContent);

        // 内容变更后重新生成Word并上传
        if (StringUtils.hasText(markdownContent)) {
            Map<String, Object> data = dataAssembler.assemble(projectId);
            String html = markdownEngine.markdownToHtml(markdownContent);
            Long generatedFileId = generateAndUploadWord(project, data, html);
            project.setGeneratedFileId(generatedFileId);
        }

        projectMapper.updateById(project);
    }

    /**
     * 生成Word文档并上传到文件服务
     *
     * @return 上传后的文件ID
     */
    private Long generateAndUploadWord(AiProject project, Map<String, Object> data, String html) {
        byte[] wordBytes = wordGenerator.generate(data, html);
        String fileName = project.getProjectName() + ".docx";

        FileUploadResponse uploadResponse = fileServiceClient.upload(wordBytes, fileName, BIZ_TYPE_TENDER_DOC);
        log.info("Word文档已上传，文件ID: {}, 文件名: {}", uploadResponse.getFileId(), fileName);
        return uploadResponse.getFileId();
    }

    private AiProject getProjectOrThrow(Long projectId) {
        AiProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    /**
     * 根据组装数据构建完整的Markdown文档
     */
    private String buildMarkdownDocument(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(data.getOrDefault("projectName", "")).append("\n\n");

        // 项目基本信息
        sb.append("## 项目基本信息\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|------|------|\n");
        appendTableRow(sb, "项目编号", data.get("projectCode"));
        appendTableRow(sb, "项目类别", data.get("projectCategory"));
        appendTableRow(sb, "项目类型", data.get("projectType"));
        appendTableRow(sb, "预算金额(元)", data.get("budget"));
        appendTableRow(sb, "招标单位", data.get("tenderUnit"));
        appendTableRow(sb, "项目地点", data.get("projectLocation"));
        appendTableRow(sb, "联系人", data.get("contactPerson"));
        appendTableRow(sb, "联系电话", data.get("contactPhone"));
        sb.append("\n");

        // 项目描述
        Object desc = data.get("projectDescription");
        if (desc != null && StringUtils.hasText(String.valueOf(desc))) {
            sb.append("## 项目描述\n\n");
            sb.append(desc).append("\n\n");
        }

        // 招标需求内容
        Object reqContent = data.get("requirementContent");
        if (reqContent != null && StringUtils.hasText(String.valueOf(reqContent))) {
            sb.append("## 招标需求\n\n");
            sb.append(reqContent).append("\n\n");
        }

        // 评审项
        sb.append("## 评审项\n\n");
        appendReviewSection(sb, "### 符合性审查\n\n", data.get("complianceItems"));
        appendReviewSection(sb, "### 技术标评审\n\n", data.get("technicalItems"));
        appendReviewSection(sb, "### 资信标评审\n\n", data.get("creditItems"));
        appendReviewSection(sb, "### 商务评审\n\n", data.get("commercialItems"));

        return sb.toString();
    }

    private void appendTableRow(StringBuilder sb, String label, Object value) {
        sb.append("| ").append(label).append(" | ")
                .append(value != null ? value : "-")
                .append(" |\n");
    }

    @SuppressWarnings("unchecked")
    private void appendReviewSection(StringBuilder sb, String header, Object items) {
        if (items instanceof java.util.List<?> list && !list.isEmpty()) {
            sb.append(header);
            for (Object item : list) {
                if (item instanceof com.jy.eleaitender.common.entity.core.AiReviewItem reviewItem) {
                    String indent = "  ".repeat(Math.max(0, (reviewItem.getLevel() != null ? reviewItem.getLevel() : 1) - 1));
                    sb.append(indent).append("- **").append(reviewItem.getItemName()).append("**");
                    if (StringUtils.hasText(reviewItem.getItemContent())) {
                        sb.append(": ").append(reviewItem.getItemContent());
                    }
                    if (reviewItem.getScore() != null) {
                        sb.append(" (").append(reviewItem.getScore()).append("分)");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
    }
}

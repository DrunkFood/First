package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectTemplate;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.DocumentPreviewVO;
import com.jy.eleaitender.core.engine.DocumentDataAssembler;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.service.IDocumentIntegrationService;
import com.jy.eleaitender.core.service.IProjectTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 文档集成服务实现
 * 通过File服务的poi-tl模板引擎生成Word文档，替代原有的Markdown生成流程
 */
@Slf4j
@Service
public class DocumentIntegrationServiceImpl implements IDocumentIntegrationService {

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private DocumentDataAssembler dataAssembler;

    @Autowired
    private IProjectTemplateService projectTemplateService;

    @Autowired
    private InternalFileServiceClient fileServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentPreviewVO integrate(Long projectId) {
        TbProject project = getProjectOrThrow(projectId);

        // 1. 获取项目模板
        TbProjectTemplate pt = projectTemplateService.getByProjectId(projectId);
        if (pt == null || pt.getFileId() == null) {
            throw new BusinessException(ResponseCode.TEMPLATE_NOT_FOUND, "项目未绑定模板或模板无文件");
        }

        // 2. 组装扁平化文档数据
        Map<String, Object> data = dataAssembler.assemble(projectId);

        // 3. 调用File服务生成Word文档
        Long generatedFileId = fileServiceClient.generateDocument(
                pt.getFileId(), data, project.getProjectName() + ".docx");

        // 4. 更新项目
        project.setGeneratedFileId(generatedFileId);
        projectMapper.updateById(project);

        log.info("文档集成完成，项目ID: {}, 生成文件ID: {}", projectId, generatedFileId);

        DocumentPreviewVO vo = new DocumentPreviewVO();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getProjectName());
        vo.setIntegrated(true);
        vo.setGeneratedFileId(generatedFileId);
        return vo;
    }

    @Override
    public DocumentPreviewVO getPreview(Long projectId) {
        TbProject project = getProjectOrThrow(projectId);

        DocumentPreviewVO vo = new DocumentPreviewVO();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getProjectName());
        vo.setGeneratedFileId(project.getGeneratedFileId());
        vo.setIntegrated(project.getGeneratedFileId() != null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editContent(Long projectId, String markdownContent) {
        throw new BusinessException(ResponseCode.OPERATION_NOT_SUPPORTED, "在线编辑暂不支持，请通过模板修改后重新生成");
    }

    private TbProject getProjectOrThrow(Long projectId) {
        TbProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        return project;
    }
}

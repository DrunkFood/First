package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private TbProjectMapper projectMapper;

    @Test
    void createRejectsDuplicateProjectCodeGlobally() {
        TbProject project = validProject("PRJ-001");
        when(projectMapper.countByProjectCodeIncludingDeleted("PRJ-001", null)).thenReturn(1L);

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("项目编号已存在");

        verify(projectMapper, never()).insert(any(TbProject.class));
    }

    @Test
    void createConvertsProjectCodeUniqueConstraintToBusinessException() {
        TbProject project = validProject("PRJ-001");
        when(projectMapper.countByProjectCodeIncludingDeleted("PRJ-001", null)).thenReturn(0L);
        when(projectMapper.selectCount(any())).thenReturn(0L);
        when(projectMapper.insert(any(TbProject.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'PRJ-001' for key 'uk_project_code'"));

        assertThatThrownBy(() -> projectService.create(project))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("项目编号已存在")
                .satisfies(exception -> assertThat(((BusinessException) exception).getCode())
                        .isEqualTo(ResponseCode.PROJECT_EXISTS.getCode()));
    }

    @Test
    void updateAllowsKeepingOwnProjectCode() {
        TbProject existing = validProject("PRJ-001");
        existing.setId(10L);
        TbProject update = validProject("PRJ-001");
        when(projectMapper.selectById(10L)).thenReturn(existing);
        when(projectMapper.selectCount(any())).thenReturn(0L);

        projectService.update(10L, update);

        ArgumentCaptor<TbProject> projectCaptor = ArgumentCaptor.forClass(TbProject.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getProjectCode()).isEqualTo("PRJ-001");
    }

    @Test
    void updateKeepsExistingProjectCodeWhenPayloadContainsDifferentCode() {
        TbProject existing = validProject("PRJ-001");
        existing.setId(10L);
        TbProject update = validProject("PRJ-002");
        when(projectMapper.selectById(10L)).thenReturn(existing);
        when(projectMapper.selectCount(any())).thenReturn(0L, 0L);

        projectService.update(10L, update);

        ArgumentCaptor<TbProject> projectCaptor = ArgumentCaptor.forClass(TbProject.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getProjectCode()).isEqualTo("PRJ-001");
    }

    @Test
    void updateDoesNotValidatePayloadProjectCode() {
        TbProject existing = validProject("PRJ-001");
        existing.setId(10L);
        TbProject update = validProject("PRJ-002");
        update.setProjectName("");
        when(projectMapper.selectById(10L)).thenReturn(existing);

        projectService.update(10L, update);

        ArgumentCaptor<TbProject> projectCaptor = ArgumentCaptor.forClass(TbProject.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        verify(projectMapper, never()).selectCount(any());
        assertThat(projectCaptor.getValue().getProjectCode()).isEqualTo("PRJ-001");
    }

    @Test
    void updateAllowsRequirementContentOnlyPayload() {
        TbProject existing = validProject("PRJ-001");
        existing.setId(10L);
        TbProject update = new TbProject();
        update.setRequirementContent("需求内容");
        when(projectMapper.selectById(10L)).thenReturn(existing);

        projectService.update(10L, update);

        ArgumentCaptor<TbProject> projectCaptor = ArgumentCaptor.forClass(TbProject.class);
        verify(projectMapper).updateById(projectCaptor.capture());
        verify(projectMapper, never()).selectCount(any());
        assertThat(projectCaptor.getValue().getProjectCode()).isEqualTo("PRJ-001");
        assertThat(projectCaptor.getValue().getRequirementContent()).isEqualTo("需求内容");
    }

    private TbProject validProject(String projectCode) {
        TbProject project = new TbProject();
        project.setProjectCode(projectCode);
        project.setProjectName("项目名称");
        project.setReviewType("MANUAL");
        return project;
    }
}

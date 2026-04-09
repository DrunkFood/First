package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.mapper.ProjectLockMapper;
import com.jy.eletender.tenderdocument.service.impl.ProjectLockServiceImpl;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectLockServiceTest {

    @Mock
    private ProjectLockMapper projectLockMapper;

    @InjectMocks
    private ProjectLockServiceImpl projectLockService;

    @Test
    void shouldCreateProjectLockWhenFirstUserEnters() {
        TenderDocumentUserContext userContext = buildContext("app-a", "u-1", "913301");
        when(projectLockMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            ProjectLock projectLock = invocation.getArgument(0);
            projectLock.setId(1L);
            return 1;
        }).when(projectLockMapper).insert(any(ProjectLock.class));

        ProjectLock projectLock = projectLockService.verifyOrCreateLock("P-100", userContext);

        assertThat(projectLock.getProjectId()).isEqualTo("P-100");
        assertThat(projectLock.getOwnerAppKey()).isEqualTo("app-a");
        assertThat(projectLock.getOwnerUserId()).isEqualTo("u-1");
        verify(projectLockMapper).insert(any(ProjectLock.class));
    }

    @Test
    void shouldAllowSameOwnerToReuseProjectLock() {
        ProjectLock existingLock = new ProjectLock();
        existingLock.setProjectId("P-100");
        existingLock.setOwnerAppKey("app-a");
        existingLock.setOwnerUserId("u-1");
        existingLock.setOwnerEnterpriseCode("913301");
        when(projectLockMapper.selectOne(any())).thenReturn(existingLock);

        ProjectLock projectLock = projectLockService.verifyOrCreateLock("P-100", buildContext("app-a", "u-1", "913301"));

        assertThat(projectLock).isSameAs(existingLock);
    }

    @Test
    void shouldRejectDifferentOwner() {
        ProjectLock existingLock = new ProjectLock();
        existingLock.setProjectId("P-100");
        existingLock.setOwnerAppKey("app-a");
        existingLock.setOwnerUserId("u-1");
        existingLock.setOwnerEnterpriseCode("913301");
        when(projectLockMapper.selectOne(any())).thenReturn(existingLock);

        assertThatThrownBy(() -> projectLockService.verifyOrCreateLock("P-100", buildContext("app-b", "u-2", "913399")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已由首创人锁定");
    }

    private TenderDocumentUserContext buildContext(String appKey, String userId, String enterpriseCode) {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey(appKey);
        context.setUserId(userId);
        context.setUserName("测试用户");
        context.setEnterpriseId("ENT-1");
        context.setEnterpriseName("测试企业");
        context.setEnterpriseCode(enterpriseCode);
        return context;
    }
}

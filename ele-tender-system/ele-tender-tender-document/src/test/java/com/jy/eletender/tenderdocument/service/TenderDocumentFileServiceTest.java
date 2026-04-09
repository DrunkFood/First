package com.jy.eletender.tenderdocument.service;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentFileBindRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFilePageResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.service.impl.TenderDocumentFileServiceImpl;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenderDocumentFileServiceTest {

    @Mock
    private TenderDocumentMapper tenderDocumentMapper;

    @Mock
    private TenderDocumentFileMapper tenderDocumentFileMapper;

    @Mock
    private IProjectLockService projectLockService;

    @InjectMocks
    private TenderDocumentFileServiceImpl tenderDocumentFileService;

    @Test
    void shouldBindProjectScopedPurchaseFileForPublicProject() {
        TenderDocument tenderDocument = buildDocument("公开招标");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);

        tenderDocumentFileService.bindPurchaseFile(100L, buildBindRequest(null), buildContext());

        verify(tenderDocumentFileMapper).insert(any(TenderDocumentFile.class));
    }

    @Test
    void shouldBindTenderScopedPurchaseFileForInvitedProject() {
        TenderDocument tenderDocument = buildDocument("邀请招标");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);

        tenderDocumentFileService.bindPurchaseFile(100L, buildBindRequest("T-01"), buildContext());

        verify(tenderDocumentFileMapper).insert(any(TenderDocumentFile.class));
    }

    @Test
    void shouldReplaceExistingActiveFileWhenRebinding() {
        TenderDocument tenderDocument = buildDocument("公开招标");
        TenderDocumentFile existingFile = new TenderDocumentFile();
        existingFile.setId(1L);
        existingFile.setActiveFlag(1);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(existingFile);

        tenderDocumentFileService.bindSignedFile(100L, buildBindRequest(null), buildContext());

        verify(tenderDocumentFileMapper).updateById(existingFile);
        verify(tenderDocumentFileMapper).insert(any(TenderDocumentFile.class));
    }

    @Test
    void shouldAppendUploadTimestampWhenBindingSignedFile() {
        TenderDocument tenderDocument = buildDocument("公开招标");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(null);

        TenderDocumentFileBindRequest request = buildBindRequest(null);
        request.setFileName("采购文件签章稿.pdf");
        tenderDocumentFileService.bindSignedFile(100L, request, buildContext());

        ArgumentCaptor<TenderDocumentFile> captor = ArgumentCaptor.forClass(TenderDocumentFile.class);
        verify(tenderDocumentFileMapper).insert(captor.capture());
        assertThat(captor.getValue().getFileName()).matches("采购文件签章稿\\d{14}\\.pdf");
    }

    @Test
    void shouldDeleteCurrentFileBinding() {
        TenderDocument tenderDocument = buildDocument("公开招标");
        TenderDocumentFile existingFile = new TenderDocumentFile();
        existingFile.setId(1L);
        existingFile.setActiveFlag(1);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectOne(any())).thenReturn(existingFile);

        tenderDocumentFileService.removePurchaseFile(100L, null, buildContext());

        verify(tenderDocumentFileMapper).updateById(existingFile);
    }

    @Test
    void shouldReturnCreateTimeInPurchaseFilePage() {
        TenderDocument tenderDocument = buildDocument("公开招标");
        Date createTime = new Date(1_730_000_000_000L);
        TenderDocumentFile file = new TenderDocumentFile();
        file.setTenderId("T-01");
        file.setScopeType(TenderDocumentScopeType.TENDER.name());
        file.setFileRole(TenderDocumentFileType.PURCHASE_SOURCE_PDF.name());
        file.setFileId(123L);
        file.setFileName("采购文件.pdf");
        file.setCreatedTime(createTime);
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());
        when(tenderDocumentFileMapper.selectList(any())).thenReturn(List.of(file));

        TenderDocumentFilePageResponse response = tenderDocumentFileService.getPurchaseFilePage(100L, buildContext());

        assertThat(response.getFileList()).hasSize(1);
        assertThat(response.getFileList().get(0).getCreateTime()).isEqualTo(createTime);
    }

    @Test
    void shouldRejectInvitedProjectBindingWithoutTenderId() {
        TenderDocument tenderDocument = buildDocument("邀请招标");
        when(tenderDocumentMapper.selectById(100L)).thenReturn(tenderDocument);
        when(projectLockService.verifyOrCreateLock(any(), any())).thenReturn(new ProjectLock());

        assertThatThrownBy(() -> tenderDocumentFileService.bindPurchaseFile(100L, buildBindRequest(null), buildContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标段ID");

        verify(tenderDocumentFileMapper, never()).insert(any(TenderDocumentFile.class));
    }

    private TenderDocument buildDocument(String purchaseMethod) {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setStatus(TenderDocumentStatus.DRAFT.name());
        tenderDocument.setPurchaseMethod(purchaseMethod);
        tenderDocument.setCompileScope(purchaseMethod.contains("邀请")
                ? TenderDocumentScopeType.TENDER.name()
                : TenderDocumentScopeType.PROJECT.name());
        return tenderDocument;
    }

    private TenderDocumentFileBindRequest buildBindRequest(String tenderId) {
        TenderDocumentFileBindRequest request = new TenderDocumentFileBindRequest();
        request.setTenderId(tenderId);
        request.setFileId(123L);
        request.setFileName("采购文件.pdf");
        request.setFileSize(1024L);
        request.setContentType("application/pdf");
        request.setFileSha256("abc");
        return request;
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseCode("913301");
        return context;
    }
}

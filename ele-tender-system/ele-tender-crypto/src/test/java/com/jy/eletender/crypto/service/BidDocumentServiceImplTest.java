package com.jy.eletender.crypto.service;

import com.jy.eletender.common.entity.crypto.BdcBidDocument;
import com.jy.eletender.common.entity.file.FileInfo;
import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcBidDocumentMapper;
import com.jy.eletender.crypto.mapper.SysAccessSystemReadMapper;
import com.jy.eletender.crypto.model.CallbackInvokeResult;
import com.jy.eletender.crypto.service.impl.BidDocumentServiceImpl;
import com.jy.eletender.crypto.support.CryptoUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidDocumentServiceImplTest {

    @Mock
    private BdcBidDocumentMapper bidDocumentMapper;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ICallbackService callbackService;
    @Mock
    private SysAccessSystemReadMapper sysAccessSystemReadMapper;

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistEncryptedFileIntoThreeLevelSha256Directories() throws Exception {
        CryptoProperties properties = new CryptoProperties();
        properties.setBidDocumentPath(tempDir.toString());
        properties.setFileServiceBaseUrl("http://file-service");

        BidDocumentServiceImpl service = new BidDocumentServiceImpl(
                bidDocumentMapper,
                restTemplate,
                properties,
                callbackService,
                sysAccessSystemReadMapper
        );

        CryptoUserContext userContext = new CryptoUserContext();
        userContext.setAppKey("app-a");
        userContext.setUserId("u1");

        BidDocumentPushRequest request = new BidDocumentPushRequest();
        request.setFileId(1001L);
        request.setFileSha256("833183e24cabe9f5330eb37ab449543c4071217e490f7dd54a391923e676ab11");

        byte[] encryptedFile = "encrypted-content".getBytes();
        when(restTemplate.getForEntity("http://file-service/api/file/download/1001", byte[].class))
                .thenReturn(ResponseEntity.ok(encryptedFile));

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("投标文件.HzctTbs");
        Result<FileInfo> fileInfoResult = Result.success(fileInfo);
        when(restTemplate.exchange(
                eq("http://file-service/api/file/info/1001"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(fileInfoResult));

        when(bidDocumentMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            BdcBidDocument bidDocument = invocation.getArgument(0);
            bidDocument.setId(1L);
            return 1;
        }).when(bidDocumentMapper).insert(any(BdcBidDocument.class));

        when(sysAccessSystemReadMapper.selectByAppKey("app-a")).thenReturn(null);
        when(callbackService.callbackBidDocumentResult("app-a", 1001L, "SUCCESS", null))
                .thenReturn(CallbackInvokeResult.success("200", "ok"));

        BidDocumentPushResponse response = service.pushBidDocument(userContext, request);

        assertThat(response.getFileId()).isEqualTo(1001L);

        ArgumentCaptor<BdcBidDocument> bidDocumentCaptor = ArgumentCaptor.forClass(BdcBidDocument.class);
        verify(bidDocumentMapper).insert(bidDocumentCaptor.capture());
        assertThat(bidDocumentCaptor.getValue().getFileId()).isEqualTo(1001L);

        Path expectedPath = tempDir.resolve("app-a")
                .resolve("83")
                .resolve("31")
                .resolve("83")
                .resolve("833183e24cabe9f5330eb37ab449543c4071217e490f7dd54a391923e676ab11.HzctTbs");
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(bidDocumentCaptor.getValue().getFileStoragePath()).isEqualTo(expectedPath.toString());
    }
}

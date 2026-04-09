package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.logging.TraceContext;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentCallbackGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentCallbackGatewayResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 对接业务系统的回传网关实现。
 * 负责把签章文件和数据包文件按交互协议回传给来源业务系统。
 */
@Primary
@Component
public class InteractionTenderDocumentCallbackGateway implements TenderDocumentCallbackGateway {

    private final BusinessSystemRemoteClient remoteClient;

    public InteractionTenderDocumentCallbackGateway(BusinessSystemRemoteClient remoteClient) {
        this.remoteClient = remoteClient;
    }

    /**
     * 回传签章文件到业务系统。
     */
    @Override
    public TenderDocumentCallbackGatewayResult callbackSignedFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                                  String tenderId, TenderDocumentUserContext userContext) {
        com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest request = new com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest();
        request.setBizType(tenderDocument.getBizType());
        request.setBizId(tenderDocument.getBizId());
        request.setProjectId(tenderDocument.getProjectId());
        request.setTenderId(tenderId);
        request.setFileId(file.getFileId());
        request.setFileName(file.getFileName());
        remoteClient.callbackTenderPdf(userContext.getAppKey(), TraceContext.getTraceId(), request);
        return new TenderDocumentCallbackGatewayResult(true, "200", "签章文件回传成功");
    }

    /**
     * 回传数据包文件到业务系统。
     */
    @Override
    public TenderDocumentCallbackGatewayResult callbackPackageFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                                   String tenderId, TenderDocumentUserContext userContext) {
        com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest request = new com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest();
        request.setBizType(tenderDocument.getBizType());
        request.setBizId(tenderDocument.getBizId());
        request.setProjectId(tenderDocument.getProjectId());
        request.setTenderId(tenderId);
        request.setFileId(file.getFileId());
        request.setFileName(file.getFileName());
        remoteClient.callbackTenderPackage(userContext.getAppKey(), TraceContext.getTraceId(), request);
        return new TenderDocumentCallbackGatewayResult(true, "200", "数据包回传成功");
    }
}

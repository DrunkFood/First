package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eleaitender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eleaitender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eleaitender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eleaitender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eleaitender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eleaitender.common.interaction.dto.EnvelopeJoinResponse;
import com.jy.eleaitender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eleaitender.common.interaction.dto.ExternalTokenResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eleaitender.common.interaction.dto.ExternalUserInfoResponse;
import com.jy.eleaitender.common.interaction.dto.TenderEntryContext;

import java.nio.file.Path;

/**
 * 电子标交互统一客户端
 */
public class EleAiTenderInteractionClient {

    private final ExternalAuthClient externalAuthClient;
    private final ExternalUserInfoClient externalUserInfoClient;
    private final FileClient fileClient;
    private final BidDocumentPushClient bidDocumentPushClient;
    private final BidDecryptClient bidDecryptClient;
    private final EnvelopeClient envelopeClient;
    private final TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder;

    public EleAiTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                        ExternalUserInfoClient externalUserInfoClient,
                                        FileClient fileClient,
                                        TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        this(externalAuthClient,
                externalUserInfoClient,
                fileClient,
                null,
                null,
                null,
                tenderDocumentEntryUrlBuilder);
    }

    public EleAiTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                        ExternalUserInfoClient externalUserInfoClient,
                                        FileClient fileClient,
                                        BidDocumentPushClient bidDocumentPushClient,
                                        BidDecryptClient bidDecryptClient,
                                        EnvelopeClient envelopeClient,
                                        TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        this.externalAuthClient = externalAuthClient;
        this.externalUserInfoClient = externalUserInfoClient;
        this.fileClient = fileClient;
        this.bidDocumentPushClient = bidDocumentPushClient;
        this.bidDecryptClient = bidDecryptClient;
        this.envelopeClient = envelopeClient;
        this.tenderDocumentEntryUrlBuilder = tenderDocumentEntryUrlBuilder;
    }

    public ExternalTokenResponse getExternalToken(ExternalTokenRequest request) {
        return externalAuthClient.getExternalToken(request);
    }

    public ExternalUserInfoResponse getCurrentExternalUser(String authorization) {
        return externalUserInfoClient.getCurrentExternalUser(authorization);
    }

    public InteractionFileInfoResponse getFileInfo(Long fileId) {
        return fileClient.getFileInfo(fileId);
    }

    public InteractionFileDownloadResponse downloadFile(Long fileId) {
        return fileClient.downloadFile(fileId);
    }

    public InteractionFileUploadResponse uploadFile(byte[] content, String fileName, String bizType) {
        return fileClient.uploadFile(content, fileName, bizType);
    }

    public InteractionFileUploadResponse uploadFile(Path filePath, String bizType) {
        return fileClient.uploadFile(filePath, bizType);
    }

    public BidDocumentPushResponse pushBidDocument(String authorization, BidDocumentPushRequest request) {
        requireClient(bidDocumentPushClient, "BidDocumentPushClient");
        return bidDocumentPushClient.push(authorization, request);
    }

    public BidDecryptSubmitResponse submitDecrypt(String authorization, BidDecryptSubmitRequest request) {
        requireClient(bidDecryptClient, "BidDecryptClient");
        return bidDecryptClient.submit(authorization, request);
    }

    public BidDecryptStatusResponse queryDecryptStatus(String authorization, String recordId) {
        requireClient(bidDecryptClient, "BidDecryptClient");
        return bidDecryptClient.queryStatus(authorization, recordId);
    }

    public EnvelopeJoinResponse joinEnvelope(String authorization, EnvelopeJoinRequest request) {
        requireClient(envelopeClient, "EnvelopeClient");
        return envelopeClient.join(authorization, request);
    }

    public String buildTenderDocumentEntryUrl(TenderEntryContext context) {
        return tenderDocumentEntryUrlBuilder.build(context);
    }

    private void requireClient(Object client, String clientName) {
        if (client == null) {
            throw new IllegalStateException(clientName + " is not configured");
        }
    }
}

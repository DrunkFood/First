package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.support.interaction.DownloadedFileInfo;
import com.jy.eletender.tenderdocument.support.interaction.FileTransferClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Component
public class SignedPdfContentLoader {

    private final FileTransferClient fileTransferClient;

    public SignedPdfContentLoader(FileTransferClient fileTransferClient) {
        this.fileTransferClient = fileTransferClient;
    }

    public FinalPackagePayload.TenderDocumentSignPdf load(TenderDocumentFile signedPdfFile) {
        // 从统一文件服务拉取签章文件内容，避免本模块自建文件存储。
        DownloadedFileInfo downloadedFile = fileTransferClient.download(signedPdfFile.getFileId());
        FinalPackagePayload.TenderDocumentSignPdf signPdf = new FinalPackagePayload.TenderDocumentSignPdf();
        signPdf.setFileName(StringUtils.hasText(downloadedFile.getFileName()) ? downloadedFile.getFileName() : signedPdfFile.getFileName());
        signPdf.setSize(downloadedFile.getContent() == null ? 0L : (long) downloadedFile.getContent().length);
        // 数据包协议要求内嵌签章 PDF 的 Base64 内容。
        signPdf.setSignedFileBase64(Base64.getEncoder().encodeToString(downloadedFile.getContent()));
        signPdf.setSha256(signedPdfFile.getFileSha256());
        return signPdf;
    }
}

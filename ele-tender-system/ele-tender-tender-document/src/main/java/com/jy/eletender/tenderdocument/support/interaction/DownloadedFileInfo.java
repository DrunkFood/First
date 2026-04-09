package com.jy.eletender.tenderdocument.support.interaction;

public class DownloadedFileInfo {

    private final Long fileId;
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public DownloadedFileInfo(Long fileId, String fileName, String contentType, byte[] content) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}

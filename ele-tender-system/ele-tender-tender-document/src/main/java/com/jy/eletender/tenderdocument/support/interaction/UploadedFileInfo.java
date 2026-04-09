package com.jy.eletender.tenderdocument.support.interaction;

public class UploadedFileInfo {

    private final Long fileId;
    private final String fileName;
    private final Long fileSize;
    private final String fileSha256;

    public UploadedFileInfo(Long fileId, String fileName, Long fileSize, String fileSha256) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileSha256 = fileSha256;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileSha256() {
        return fileSha256;
    }
}

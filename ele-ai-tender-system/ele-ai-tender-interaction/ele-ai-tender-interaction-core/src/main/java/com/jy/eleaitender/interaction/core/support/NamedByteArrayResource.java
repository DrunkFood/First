package com.jy.eleaitender.interaction.core.support;

import org.springframework.core.io.ByteArrayResource;

/**
 * 为 multipart 上传提供文件名的 ByteArrayResource 子类。
 */
public final class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    public NamedByteArrayResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }
}

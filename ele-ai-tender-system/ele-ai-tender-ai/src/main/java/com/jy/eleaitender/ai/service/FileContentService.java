package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.entity.file.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件内容服务
 * 通过 InternalFileServiceClient 获取文件元数据和内容，供AI调用使用
 */
@Slf4j
@Service
public class FileContentService {

    private final InternalFileServiceClient fileServiceClient;

    public FileContentService(InternalFileServiceClient fileServiceClient) {
        this.fileServiceClient = fileServiceClient;
    }

    /**
     * 根据文件ID列表，拼接所有文件内容供AI使用
     *
     * @param fileIds 文件ID列表（字符串形式）
     * @return 拼接后的文档内容，格式: 【文件名】\n内容
     */
    public String resolveFileContents(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return "";
        }

        List<String> contentList = new ArrayList<>();
        for (String fileIdStr : fileIds) {
            try {
                Long fileId = Long.parseLong(fileIdStr);
                FileInfo fileInfo = fileServiceClient.info(fileId);
                if (fileInfo == null) {
                    continue;
                }
                String content = extractContent(fileId);
                if (content == null) {
                    continue;
                }
                contentList.add(String.format("\n\n【%s】\n%s", fileInfo.getFileName(), content));
            } catch (NumberFormatException e) {
                log.warn("无效的文件ID格式: {}", fileIdStr);
            }
        }
        if (contentList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n【文档内容】");
        contentList.forEach(sb::append);
        return sb.toString();
    }

    /**
     * 通过文件服务下载文件并用Tika解析文本内容
     */
    private String extractContent(Long fileId) {
        try {
            byte[] fileBytes = fileServiceClient.download(fileId);
            if (fileBytes == null || fileBytes.length == 0) {
                log.warn("文件内容为空: fileId={}", fileId);
                return null;
            }

            ByteArrayResource resource = new ByteArrayResource(fileBytes);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();
            return documents.stream()
                    .map(Document::getContent)
                    .collect(StringBuilder::new, (sb, s) -> {
                        if (!sb.isEmpty()) {
                            sb.append("\n\n");
                        }
                        sb.append(s);
                    }, (sb1, sb2) -> {
                        if (!sb1.isEmpty() && !sb2.isEmpty()) {
                            sb1.append("\n\n");
                        }
                        sb1.append(sb2);
                    }).toString();
        } catch (Exception e) {
            log.error("文件解析失败: fileId={}", fileId, e);
            return null;
        }
    }
}

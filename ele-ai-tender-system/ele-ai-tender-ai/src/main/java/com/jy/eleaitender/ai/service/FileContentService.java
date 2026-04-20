package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.ai.mapper.FileInfoMapper;
import com.jy.eleaitender.common.entity.file.FileInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件内容服务
 * 根据文件ID获取文件元数据和内容，供AI调用使用
 */
@Slf4j
@Service
public class FileContentService {

    private final FileInfoMapper fileInfoMapper;

    @Value("${file.storage.base-path:/data/ele-ai-tender/files}")
    private String basePath;

    public FileContentService(FileInfoMapper fileInfoMapper) {
        this.fileInfoMapper = fileInfoMapper;
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
                FileInfo fileInfo = fileInfoMapper.selectById(fileId);
                if (fileInfo == null) {
                    // contentList.add(String.format("\n\n【文件ID:%s】\n文件不存在", fileIdStr));
                    continue;
                }

                String content = extractContent(fileInfo);
                contentList.add(String.format("\n\n【%s】\n%s", fileInfo.getFileName(), content));
            } catch (NumberFormatException e) {
                log.warn("无效的文件ID格式: {}", fileIdStr);
                //contentList.add(String.format("\n\n【文件ID:%s】\n文件ID格式无效", fileIdStr));
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
     * 从磁盘读取文件并用Tika解析文本内容
     */
    private String extractContent(FileInfo fileInfo) {
        File file = new File(basePath, fileInfo.getFilePath());
        if (!file.exists()) {
            log.warn("文件不存在: {}", file.getAbsolutePath());
            return "[文件不存在: " + fileInfo.getFileName() + "]";
        }

        try {
            FileSystemResource resource = new FileSystemResource(file);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();
            return documents.stream()
                    .map(Document::getContent)
                    .collect(StringBuilder::new, (sb, s) -> {
                        if (!sb.isEmpty()) sb.append("\n\n");
                        sb.append(s);
                    }, (sb1, sb2) -> {
                        if (!sb1.isEmpty() && !sb2.isEmpty()) sb1.append("\n\n");
                        sb1.append(sb2);
                    })
                    .toString();
        } catch (Exception e) {
            log.error("文件解析失败: fileId={}, fileName={}", fileInfo.getId(), fileInfo.getFileName(), e);
            return "[" + fileInfo.getFileName() + ": 解析失败 - " + e.getMessage() + "]";
        }
    }
}

package com.jy.eleaitender.ai.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件解析
 */
public class FileUtils {

    /**
     * 从上传的文件中提取文本内容
     *
     * @param fileId 文件ID
     * @return 提取的文本内容
     */
    public static String extractContent(String fileId) {
        // TODO 通过fileId获取文件对象
        MultipartFile file = null;
        try {
            // 将 MultipartFile 转换为 Resource
            Resource resource = file.getResource();
            // 使用 TikaDocumentReader 读取文档
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();
            // 提取所有文档的内容并合并
            return documents.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            // 如果解析失败，返回错误信息
            return "[文档解析失败: " + e.getMessage() + "]";
        }
    }

}

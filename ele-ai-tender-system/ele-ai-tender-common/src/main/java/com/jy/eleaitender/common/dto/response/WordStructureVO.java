package com.jy.eleaitender.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Word文档结构DTO
 */
@Data
@Schema(description = "Word文档结构")
public class WordStructureVO {

    @Schema(description = "章节列表")
    private List<Chapter> chapters;

    @Schema(description = "占位符列表({{变量名}}格式)")
    private List<String> placeholders;

    @Schema(description = "书签列表")
    private List<String> bookmarks;

    /**
     * Word章节
     */
    @Data
    @Schema(description = "Word章节")
    public static class Chapter {

        @Schema(description = "标题级别(1-6)")
        private int level;

        @Schema(description = "标题文本")
        private String title;

        @Schema(description = "章节内书签列表")
        private List<String> bookmarks;
    }
}

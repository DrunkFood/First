package com.jy.eleaitender.common.entity.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_document")
@Schema(description = "知识库文档")
public class AiKnowledgeDocument extends BaseEntity {
    @Schema(description = "文档名称")
    private String docName;
    @Schema(description = "文档类别")
    private String docCategory;
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "文档文本内容")
    private String content;
    @Schema(description = "向量集合名称")
    private String vectorCollection;
    @Schema(description = "向量ID列表JSON")
    private String vectorIds;
    @Schema(description = "状态")
    private String status;
}

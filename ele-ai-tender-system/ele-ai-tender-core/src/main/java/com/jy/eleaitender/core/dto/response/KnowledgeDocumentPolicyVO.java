package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 知识库政策文档响应VO
 */
@Data
@Schema(description = "知识库政策文档")
public class KnowledgeDocumentPolicyVO {

    @Schema(description = "知识库文档ID")
    private Long id;

    @Schema(description = "文档名称")
    private String docName;

    @Schema(description = "关联file_info的id")
    private Long fileId;

    @Schema(description = "文件格式")
    private String fileType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private Date createTime;
}

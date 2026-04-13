package com.jy.eleaitender.common.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_info")
@Schema(description = "文件信息")
public class FileInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "存储路径")
    private String filePath;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件SHA-256")
    private String fileSha256;

    @Schema(description = "业务类型(main-version/plugin)")
    private String bizType;
}

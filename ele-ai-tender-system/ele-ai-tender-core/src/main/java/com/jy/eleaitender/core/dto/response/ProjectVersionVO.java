package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "项目版本信息")
public class ProjectVersionVO {
    @Schema(description = "版本ID")
    private Long id;
    @Schema(description = "版本号")
    private Integer versionNo;
    @Schema(description = "变更说明")
    private String changeDescription;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "创建人")
    private String createName;
}

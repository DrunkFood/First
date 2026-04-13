package com.jy.eleaitender.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_detection_record")
@Schema(description = "检测记录")
public class AiDetectionRecord extends BaseEntity {
    @Schema(description = "项目ID")
    private Long projectId;
    @Schema(description = "检测类型")
    private String detectionType;
    @Schema(description = "检测内容快照")
    private String contentSnapshot;
    @Schema(description = "检测结果JSON")
    private String result;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;
    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}

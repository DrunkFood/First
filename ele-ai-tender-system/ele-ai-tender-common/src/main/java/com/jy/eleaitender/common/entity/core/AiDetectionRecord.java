package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检测记录实体（Core模块视图）
 * 对应表: ai_detection_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_detection_record")
@Schema(description = "检测记录")
public class AiDetectionRecord extends BaseEntity {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "检测类型: SENSITIVE_WORD/TYPO/POLICY_REVIEW/FORMAT_CHECK")
    private String detectionType;

    @Schema(description = "检测内容快照")
    private String contentSnapshot;

    @Schema(description = "检测结果JSON")
    private String result;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "关联的AI任务ID")
    private Long taskId;

    @Schema(description = "关联的政策文件ID列表(逗号分隔)")
    private String policyFileIds;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}

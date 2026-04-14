package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_sys_parameter")
@Schema(description = "系统参数")
public class SysParameter extends BaseEntity {

    @Schema(description = "参数分组: SYSTEM/SWITCH/AI_RULE")
    private String paramGroup;

    @Schema(description = "参数键")
    private String paramKey;

    @Schema(description = "参数值")
    private String paramValue;

    @Schema(description = "值类型: STRING/NUMBER/BOOLEAN/JSON")
    private String paramType;

    @Schema(description = "参数中文名")
    private String paramName;

    @Schema(description = "参数说明")
    private String description;

    @Schema(description = "组内排序")
    private Integer sortOrder;
}

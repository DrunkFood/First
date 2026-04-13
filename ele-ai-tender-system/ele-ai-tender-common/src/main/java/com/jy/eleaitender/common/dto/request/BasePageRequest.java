package com.jy.eleaitender.common.dto.request;

import com.jy.eleaitender.common.constant.CommonConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求基类
 */
@Data
@Schema(description = "分页请求基类")
public class BasePageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = CommonConstant.DEFAULT_PAGE_SIZE;

    /**
     * 获取当前页码（确保最小为1）
     */
    public Integer getCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    /**
     * 获取每页大小（确保在合理范围内）
     */
    public Integer getSize() {
        if (size == null || size < 1) {
            return CommonConstant.DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, CommonConstant.MAX_PAGE_SIZE);
    }
}

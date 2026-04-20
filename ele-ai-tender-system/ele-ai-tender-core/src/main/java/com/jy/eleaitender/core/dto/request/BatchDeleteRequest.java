package com.jy.eleaitender.core.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "请选择要删除的记录")
    private List<Long> ids;
}

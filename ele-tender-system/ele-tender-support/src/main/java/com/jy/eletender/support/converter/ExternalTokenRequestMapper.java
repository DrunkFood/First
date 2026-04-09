package com.jy.eletender.support.converter;

import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eletender.support.model.external.ExternalTokenIssueCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 外部认证请求 DTO 与内部命令对象的显式映射器。
 */
@Component
public class ExternalTokenRequestMapper {

    public ExternalTokenIssueCommand toIssueCommand(ExternalTokenRequest request) {
        if (request == null) {
            throw new AuthException(ResponseCode.PARAM_ERROR, "请求体不能为空");
        }

        validateRequiredField(request.getUserName(), "userName", "用户名称不能为空");
        validateRequiredField(request.getUserId(), "userId", "用户ID不能为空");
        validateRequiredField(request.getEnterpriseName(), "enterpriseName", "企业名称不能为空");
        validateRequiredField(request.getEnterpriseId(), "enterpriseId", "企业ID不能为空");
        validateRequiredField(request.getEnterpriseCode(), "enterpriseCode", "企业社会统一信用代码不能为空");

        ExternalTokenIssueCommand command = new ExternalTokenIssueCommand();
        command.setUserName(request.getUserName());
        command.setUserId(request.getUserId());
        command.setEnterpriseName(request.getEnterpriseName());
        command.setEnterpriseId(request.getEnterpriseId());
        command.setEnterpriseCode(request.getEnterpriseCode());
        return command;
    }

    private void validateRequiredField(String value, String field, String message) {
        if (!StringUtils.isBlank(value)) {
            return;
        }
        throw new AuthException(ResponseCode.PARAM_ERROR, field + ": " + message);
    }
}

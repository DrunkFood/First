package com.jy.eletender.support.converter;

import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eletender.support.model.external.ExternalTokenIssueCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalTokenRequestMapperTest {

    private final ExternalTokenRequestMapper mapper = new ExternalTokenRequestMapper();

    @Test
    void shouldMapExternalRequestToInternalCommand() {
        ExternalTokenRequest request = new ExternalTokenRequest();
        request.setUserName("张三");
        request.setUserId("U1");
        request.setEnterpriseName("企业A");
        request.setEnterpriseId("E1");
        request.setEnterpriseCode("QY001");

        ExternalTokenIssueCommand command = mapper.toIssueCommand(request);

        assertEquals("张三", command.getUserName());
        assertEquals("U1", command.getUserId());
        assertEquals("企业A", command.getEnterpriseName());
        assertEquals("E1", command.getEnterpriseId());
        assertEquals("QY001", command.getEnterpriseCode());
    }

    @Test
    void shouldThrowAuthExceptionWhenRequiredFieldIsBlank() {
        ExternalTokenRequest request = new ExternalTokenRequest();
        request.setUserName(" ");
        request.setUserId("U1");
        request.setEnterpriseName("企业A");
        request.setEnterpriseId("E1");
        request.setEnterpriseCode("QY001");

        AuthException exception = assertThrows(AuthException.class, () -> mapper.toIssueCommand(request));
        assertEquals("userName: 用户名称不能为空", exception.getMessage());
    }
}

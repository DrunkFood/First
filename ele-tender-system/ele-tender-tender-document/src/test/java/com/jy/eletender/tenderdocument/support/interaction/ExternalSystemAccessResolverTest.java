package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.mapper.ExternalSystemAccessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalSystemAccessResolverTest {

    @Mock
    private ExternalSystemAccessMapper externalSystemAccessMapper;

    @InjectMocks
    private ExternalSystemAccessResolver resolver;

    @Test
    void shouldResolveAvailableSystemByAppKey() {
        SysAccessSystem system = new SysAccessSystem();
        system.setAppKey("demo-app");
        system.setAppSecret("demo-secret");
        system.setSystemUrl("http://localhost:18080");
        system.setStatus(1);
        system.setExpireTime(new Date(System.currentTimeMillis() + 60_000));
        when(externalSystemAccessMapper.selectByAppKey("demo-app")).thenReturn(system);

        ResolvedExternalSystem resolved = resolver.resolve("demo-app");

        assertThat(resolved.getAppKey()).isEqualTo("demo-app");
        assertThat(resolved.getAppSecret()).isEqualTo("demo-secret");
        assertThat(resolved.getBaseUrl()).isEqualTo("http://localhost:18080");
    }

    @Test
    void shouldRejectDisabledSystem() {
        SysAccessSystem system = new SysAccessSystem();
        system.setAppKey("demo-app");
        system.setStatus(0);
        when(externalSystemAccessMapper.selectByAppKey("demo-app")).thenReturn(system);

        assertThatThrownBy(() -> resolver.resolve("demo-app"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");
    }
}

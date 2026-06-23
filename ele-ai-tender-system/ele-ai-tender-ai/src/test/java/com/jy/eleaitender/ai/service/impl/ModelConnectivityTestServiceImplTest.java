package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelConnectivityTestServiceImplTest {

    @Test
    void testDeepSeekShouldUseFixedModelId8() {
        ModelConnectivityTestServiceImpl service = new ModelConnectivityTestServiceImpl();
        ModelRouter modelRouter = mock(ModelRouter.class);
        ReflectionTestUtils.setField(service, "modelRouter", modelRouter);
        when(modelRouter.routeModel(8L)).thenThrow(new BusinessException("router-called"));

        var response = service.testDeepSeek();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getProvider()).isEqualTo("OPENAI");
        assertThat(response.getModelConfigId()).isEqualTo(8L);
        assertThat(response.getErrorMessage()).contains("router-called");
        verify(modelRouter).routeModel(8L);
    }

    @Test
    void testZhiPuShouldUseFixedModelId10() {
        ModelConnectivityTestServiceImpl service = new ModelConnectivityTestServiceImpl();
        ModelRouter modelRouter = mock(ModelRouter.class);
        ReflectionTestUtils.setField(service, "modelRouter", modelRouter);
        when(modelRouter.routeModel(10L)).thenThrow(new BusinessException("router-called"));

        var response = service.testZhiPu();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getProvider()).isEqualTo("ZHIPU");
        assertThat(response.getModelConfigId()).isEqualTo(10L);
        assertThat(response.getErrorMessage()).contains("router-called");
        verify(modelRouter).routeModel(10L);
    }
}

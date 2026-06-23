package com.jy.eleaitender.ai.processor.model;

import com.jy.eleaitender.common.entity.support.SupModelConfig;
import com.jy.eleaitender.common.exception.AiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelRouterTest {

    @Test
    void routeModelShouldCreateClientFromModelId() {
        ModelRouter router = new ModelRouter();
        ModelConfigCacheService cacheService = mock(ModelConfigCacheService.class);
        DynamicChatClientFactory clientFactory = mock(DynamicChatClientFactory.class);
        ChatClient client = mock(ChatClient.class);
        SupModelConfig config = new SupModelConfig();
        config.setId(9L);
        config.setModelName("TEST_DeepSeek_ModelConnectivity");
        config.setIsActive(1);

        ReflectionTestUtils.setField(router, "cacheService", cacheService);
        ReflectionTestUtils.setField(router, "clientFactory", clientFactory);
        when(cacheService.getModelConfig(9L)).thenReturn(config);
        when(clientFactory.getOrCreateChatClient(config)).thenReturn(client);

        ChatClient routedClient = router.routeModel(9L);

        assertThat(routedClient).isSameAs(client);
        verify(cacheService).getModelConfig(9L);
        verify(clientFactory).getOrCreateChatClient(config);
    }

    @Test
    void routeModelShouldThrowWhenModelUnavailable() {
        ModelRouter router = new ModelRouter();
        ModelConfigCacheService cacheService = mock(ModelConfigCacheService.class);
        ReflectionTestUtils.setField(router, "cacheService", cacheService);
        when(cacheService.getModelConfig(99L)).thenReturn(null);

        assertThatThrownBy(() -> router.routeModel(99L))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("模型不可用");
    }
}

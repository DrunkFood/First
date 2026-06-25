package com.jy.eleaitender.ai.processor.recorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.service.FileContentService;
import com.jy.eleaitender.ai.service.IAiResponseLogService;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCallRecorderTest {

    @Mock
    private IAiResponseLogService responseLogService;

    @Mock
    private FileContentService fileContentService;

    private AiCallRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AiCallRecorder();
        ReflectionTestUtils.setField(recorder, "responseLogService", responseLogService);
        ReflectionTestUtils.setField(recorder, "fileContentService", fileContentService);
        ReflectionTestUtils.setField(recorder, "objectMapper", new ObjectMapper());
    }

    @Test
    void callAndRecordShouldSendRawBracesWithoutSpringPromptTemplateRendering() {
        when(fileContentService.resolveFileContents(isNull())).thenReturn("");
        CapturingChatModel chatModel = new CapturingChatModel();
        ChatClient client = ChatClient.create(chatModel);

        assertThatCode(() -> recorder.callAndRecord(client, SystemPromptTemplates.REVIEW_ITEM_GENERATE,
                "生成评审项，示例：{\"demo\":true}", "GENERATION", 7001L, 9L, null))
                .doesNotThrowAnyException();

        assertThat(chatModel.prompt.getContents())
                .contains("\"reviewItems\"")
                .contains("{\"demo\":true}")
                .doesNotContain("\\{")
                .doesNotContain("\\}");
        verify(responseLogService).record(any(AiResponseLog.class));
    }

    @Test
    void recordShouldUseRoutedModelNameWhenResponseMetadataDoesNotContainModel() {
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation("content")));

        recorder.record(chatResponse, "content", "system", "user", new Date(0),
                "GENERATION", 7001L, null, 9L, "glm-4-plus");

        ArgumentCaptor<AiResponseLog> captor = ArgumentCaptor.forClass(AiResponseLog.class);
        verify(responseLogService).record(captor.capture());
        assertThat(captor.getValue().getModel()).isEqualTo("glm-4-plus");
    }

    private static final class CapturingChatModel implements ChatModel {

        private Prompt prompt;

        @Override
        public String call(String message) {
            return call(new Prompt(message)).getResult().getOutput().getContent();
        }

        @Override
        public String call(Message... messages) {
            return call(new Prompt(Arrays.asList(messages))).getResult().getOutput().getContent();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.prompt = prompt;
            return new ChatResponse(List.of(new Generation("{\"reviewItems\":[]}")));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return null;
        }
    }
}

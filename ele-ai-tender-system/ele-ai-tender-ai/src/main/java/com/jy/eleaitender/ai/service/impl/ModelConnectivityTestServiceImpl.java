package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.response.ModelConnectivityTestResponse;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.service.IModelConnectivityTestService;
import com.jy.eleaitender.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Model connectivity test service.
 */
@Slf4j
@Service
public class ModelConnectivityTestServiceImpl implements IModelConnectivityTestService {

    private static final String PROVIDER_OPENAI = "OPENAI";
    private static final String PROVIDER_ZHIPU = "ZHIPU";
    private static final Long DEEPSEEK_TEST_MODEL_ID = 8L;
    private static final Long ZHIPU_TEST_MODEL_ID = 10L;
    private static final String DEFAULT_PROMPT = "你是的模型版本是什么？";
    private static final String SYSTEM_PROMPT = "你是一个全能的编码组手，精通C++、java、php、node.js的所有主流编程语言";

    @Autowired
    private ModelRouter modelRouter;

    @Override
    public ModelConnectivityTestResponse testDeepSeek() {
        return testProvider(PROVIDER_OPENAI, DEEPSEEK_TEST_MODEL_ID);
    }

    @Override
    public ModelConnectivityTestResponse testZhiPu() {
        return testProvider(PROVIDER_ZHIPU, ZHIPU_TEST_MODEL_ID);
    }

    ModelConnectivityTestResponse testProvider(String provider, Long modelConfigId) {
        long start = System.currentTimeMillis();
        ModelConnectivityTestResponse response = buildBaseResponse(provider, modelConfigId);

        try {
            ChatClient client = modelRouter.routeModel(modelConfigId);
            ChatResponse chatResponse = client.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(DEFAULT_PROMPT)
                    .call()
                    .chatResponse();
            response.setContent(extractContent(chatResponse));
            response.setSuccess(true);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                log.debug("Model connectivity test rejected: provider={}, modelConfigId={}, message={}",
                        provider, modelConfigId, e.getMessage());
            } else {
                log.warn("Model connectivity test failed: provider={}, modelConfigId={}",
                        provider, modelConfigId, e);
            }
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage() == null ? "" : e.getMessage());
        } finally {
            response.setElapsedMs(System.currentTimeMillis() - start);
        }

        return response;
    }

    private ModelConnectivityTestResponse buildBaseResponse(String provider, Long modelConfigId) {
        ModelConnectivityTestResponse response = new ModelConnectivityTestResponse();
        response.setProvider(provider);
        response.setModelConfigId(modelConfigId);
        return response;
    }

    private String extractContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getContent();
    }
}

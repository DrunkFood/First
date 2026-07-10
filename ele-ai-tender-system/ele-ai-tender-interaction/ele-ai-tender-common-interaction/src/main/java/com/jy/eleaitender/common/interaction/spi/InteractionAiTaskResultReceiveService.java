package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.AiTaskResultCallbackRequest;

/**
 * AI任务结果接收SPI
 * <p>外部系统实现此接口，接收AI任务终态结果回调。</p>
 */
public interface InteractionAiTaskResultReceiveService {

    void receive(AiTaskResultCallbackRequest request);
}

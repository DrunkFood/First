package com.jy.eletender.common.interaction.spi;

/**
 * 交互事件日志扩展点
 */
public interface InteractionEventLogger {

    void logInbound(String apiName, Object request, Object response, Throwable error);

    void logOutbound(String apiName, Object request, Object response, Throwable error);
}

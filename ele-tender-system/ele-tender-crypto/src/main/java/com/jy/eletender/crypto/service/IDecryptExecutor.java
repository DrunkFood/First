package com.jy.eletender.crypto.service;

/**
 * 异步解密执行器，消费工件任务并驱动 `bdc_decrypt_artifact` 进入终态。
 */
public interface IDecryptExecutor {

    /**
     * 执行指定工件的解密任务。
     */
    void execute(Long artifactId);
}

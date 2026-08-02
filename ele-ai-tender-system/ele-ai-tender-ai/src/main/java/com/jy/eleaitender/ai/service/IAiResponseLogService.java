package com.jy.eleaitender.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;

/**
 * AI响应记录 Service 接口
 */
public interface IAiResponseLogService extends IService<AiResponseLog> {

    /**
     * 记录AI响应日志
     */
    void record(AiResponseLog log);
}

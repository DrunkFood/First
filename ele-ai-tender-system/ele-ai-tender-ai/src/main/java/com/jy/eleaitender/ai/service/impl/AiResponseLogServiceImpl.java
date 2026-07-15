package com.jy.eleaitender.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.ai.mapper.AiResponseLogMapper;
import com.jy.eleaitender.ai.service.IAiResponseLogService;
import com.jy.eleaitender.common.entity.ai.AiResponseLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI响应记录 Service 实现
 */
@Slf4j
@Service
public class AiResponseLogServiceImpl extends ServiceImpl<AiResponseLogMapper, AiResponseLog> implements IAiResponseLogService {

    @Autowired
    private AiResponseLogMapper aiResponseLogMapper;

    @Override
    public void record(AiResponseLog responseLog) {
        try {
            aiResponseLogMapper.insert(responseLog);
        } catch (Exception e) {
            log.error("保存AI响应记录失败: model={}, role={}, taskId={}",
                    responseLog.getModel(), responseLog.getRole(), responseLog.getTaskId(), e);
        }
    }
}

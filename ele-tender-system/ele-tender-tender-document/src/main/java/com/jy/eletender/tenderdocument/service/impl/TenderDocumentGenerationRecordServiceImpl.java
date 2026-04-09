package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentGenerationRecord;
import com.jy.eletender.tenderdocument.enums.TenderDocumentGenerationStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentGenerationRecordMapper;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationRecordService;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class TenderDocumentGenerationRecordServiceImpl implements ITenderDocumentGenerationRecordService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    private final TenderDocumentGenerationRecordMapper tenderDocumentGenerationRecordMapper;

    public TenderDocumentGenerationRecordServiceImpl(TenderDocumentGenerationRecordMapper tenderDocumentGenerationRecordMapper) {
        this.tenderDocumentGenerationRecordMapper = tenderDocumentGenerationRecordMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TenderDocumentGenerationRecord createProcessingRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        // 生成记录单独事务提交，避免外层生成事务回滚后丢失审计轨迹。
        TenderDocumentGenerationRecord record = new TenderDocumentGenerationRecord();
        record.setTenderDocumentId(tenderDocument.getId());
        record.setVersionNo(tenderDocument.getVersionNo());
        record.setProjectId(tenderDocument.getProjectId());
        record.setTenderId(tenderDocument.getTenderId());
        record.setGenerateStatus(TenderDocumentGenerationStatus.PROCESSING.name());
        record.setStartTime(new Date());
        record.setTraceId(MDC.get("traceId"));
        if (userContext != null) {
            record.setOperatorId(userContext.getUserId());
            record.setOperatorName(userContext.getUserName());
        }
        tenderDocumentGenerationRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSuccess(Long recordId, Date endTime) {
        if (recordId == null) {
            return;
        }
        TenderDocumentGenerationRecord record = tenderDocumentGenerationRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        record.setGenerateStatus(TenderDocumentGenerationStatus.SUCCESS.name());
        record.setEndTime(endTime);
        record.setDurationMs(calculateDuration(record.getStartTime(), endTime));
        record.setErrorCode(null);
        record.setErrorMessage(null);
        tenderDocumentGenerationRecordMapper.updateById(record);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFail(Long recordId, String errorCode, String errorMessage, Date endTime) {
        if (recordId == null) {
            return;
        }
        TenderDocumentGenerationRecord record = tenderDocumentGenerationRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        record.setGenerateStatus(TenderDocumentGenerationStatus.FAIL.name());
        record.setEndTime(endTime);
        record.setDurationMs(calculateDuration(record.getStartTime(), endTime));
        record.setErrorCode(errorCode);
        record.setErrorMessage(truncate(errorMessage));
        tenderDocumentGenerationRecordMapper.updateById(record);
    }

    @Override
    @Transactional(readOnly = true)
    public TenderDocumentGenerationRecord findLatestByTenderDocumentId(Long tenderDocumentId) {
        return tenderDocumentGenerationRecordMapper.selectOne(Wrappers.<TenderDocumentGenerationRecord>lambdaQuery()
                .eq(TenderDocumentGenerationRecord::getTenderDocumentId, tenderDocumentId)
                .orderByDesc(TenderDocumentGenerationRecord::getId)
                .last("limit 1"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenderDocumentGenerationRecord> listByTenderDocumentId(Long tenderDocumentId) {
        return tenderDocumentGenerationRecordMapper.selectList(Wrappers.<TenderDocumentGenerationRecord>lambdaQuery()
                .eq(TenderDocumentGenerationRecord::getTenderDocumentId, tenderDocumentId)
                .orderByDesc(TenderDocumentGenerationRecord::getId));
    }

    private Long calculateDuration(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Math.max(endTime.getTime() - startTime.getTime(), 0L);
    }

    private String truncate(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        if (message.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}

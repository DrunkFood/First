package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.DetectionRequest;
import com.jy.eleaitender.ai.dto.response.DetectionResultVO;
import com.jy.eleaitender.common.entity.core.AiDetectionRecord;
import com.jy.eleaitender.ai.mapper.AiDetectionRecordMapper;
import com.jy.eleaitender.ai.service.IDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DetectionServiceImpl implements IDetectionService {

    @Autowired
    private AiDetectionRecordMapper detectionRecordMapper;

    @Override
    @Transactional
    public AiDetectionRecord startDetection(DetectionRequest request) {
        // Phase 2: 创建PENDING状态的记录，Phase 3实现实际检测逻辑
        AiDetectionRecord record = new AiDetectionRecord();
        record.setProjectId(request.getProjectId());
        record.setDetectionType(request.getDetectionType());
        record.setContentSnapshot(request.getContentSnapshot());
        record.setStatus("PENDING");
        record.setStartedAt(LocalDateTime.now());
        detectionRecordMapper.insert(record);
        return record;
    }

    @Override
    public DetectionResultVO getResult(Long id) {
        AiDetectionRecord record = detectionRecordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        DetectionResultVO vo = new DetectionResultVO();
        vo.setId(record.getId());
        vo.setDetectionType(record.getDetectionType());
        vo.setStatus(record.getStatus());
        vo.setResult(record.getResult());
        vo.setStartedAt(record.getStartedAt());
        vo.setCompletedAt(record.getCompletedAt());
        return vo;
    }

    @Override
    @Transactional
    public void confirmResult(Long id) {
        AiDetectionRecord record = detectionRecordMapper.selectById(id);
        if (record != null) {
            record.setCompletedAt(LocalDateTime.now());
            detectionRecordMapper.updateById(record);
        }
    }
}

package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.ai.dto.request.DetectionRequest;
import com.jy.eleaitender.ai.dto.response.DetectionResultVO;
import com.jy.eleaitender.ai.entity.AiDetectionRecord;

public interface IDetectionService {
    AiDetectionRecord startDetection(DetectionRequest request);
    DetectionResultVO getResult(Long id);
    void confirmResult(Long id);
}

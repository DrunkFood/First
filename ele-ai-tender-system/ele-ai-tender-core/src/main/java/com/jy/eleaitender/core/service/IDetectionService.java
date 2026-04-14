package com.jy.eleaitender.core.service;

import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.dto.response.DetectionProgressVO;
import com.jy.eleaitender.core.dto.response.DetectionReportVO;

import java.util.Map;

/**
 * 检测服务接口
 */
public interface IDetectionService {

    /**
     * 提交最终文档检测（创建4个AI任务）
     */
    Map<String, Long> submit(Long projectId, DetectionSubmitRequest request);

    /**
     * 获取检测进度
     */
    DetectionProgressVO getProgress(Long projectId);

    /**
     * 获取检测报告
     */
    DetectionReportVO getReport(Long projectId);

    /**
     * 接受检测建议
     */
    void acceptIssue(Long recordId);

    /**
     * 拒绝检测建议
     */
    void rejectIssue(Long recordId);

    /**
     * 一键接受所有建议
     */
    void acceptAll(Long projectId);

    /**
     * 跳过检测
     */
    void skip(Long projectId);

    /**
     * 重新检测
     */
    Map<String, Long> retry(Long projectId);
}

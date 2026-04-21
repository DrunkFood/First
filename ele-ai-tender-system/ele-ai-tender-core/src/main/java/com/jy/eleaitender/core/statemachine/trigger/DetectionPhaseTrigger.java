package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.service.IDetectionService;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能检测阶段触发器
 * 进入时自动提交检测（携带政策文件ID）
 */
@Slf4j
@Component
public class DetectionPhaseTrigger implements PhaseTrigger {

    @Autowired
    private IDetectionService detectionService;

    @Override
    public void onEnter(AiProject project, Map<String, Object> context) {
        // 从上下文中提取政策文件ID列表
        List<Long> policyFileIds = null;
        if (context != null && context.containsKey("policyFileIds")) {
            Object raw = context.get("policyFileIds");
            if (raw instanceof List<?> list) {
                policyFileIds = list.stream()
                        .map(item -> {
                            if (item instanceof Number num) {
                                return num.longValue();
                            }
                            return Long.valueOf(item.toString());
                        })
                        .collect(Collectors.toList());
            }
        }

        // 构建检测提交请求
        DetectionSubmitRequest request = new DetectionSubmitRequest();
        request.setPolicyFileIds(policyFileIds);

        // 自动提交检测
        try {
            detectionService.submit(project.getId(), request);
            log.info("自动提交智能检测: projectId={}, policyFileCount={}",
                    project.getId(), policyFileIds != null ? policyFileIds.size() : 0);
        } catch (Exception e) {
            log.warn("自动提交检测失败: projectId={}, error={}", project.getId(), e.getMessage());
        }
    }

    @Override
    public boolean canComplete(AiProject project) {
        String status = project.getStatus();
        return ProjectStatus.DETECTION_PASSED.getCode().equals(status)
                || ProjectStatus.DETECTION_SKIPPED.getCode().equals(status);
    }

    @Override
    public String getIncompleteMessage() {
        return "请等待智能检测完成或跳过检测";
    }
}

package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiDetectionRecord;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.core.dto.response.MatchFileVO;

import java.util.List;
import java.util.Map;

/**
 * 业务需求服务接口
 */
public interface IRequirementService {

    /**
     * 分页查询需求列表
     */
    Page<AiRequirement> getPage(Integer pageNum, Integer pageSize, String requirementName, String status, Long projectId);

    /**
     * 根据ID获取需求详情
     */
    AiRequirement getById(Long id);

    /**
     * 创建需求
     */
    AiRequirement create(AiRequirement requirement);

    /**
     * 更新需求
     */
    void update(Long id, AiRequirement requirement);

    /**
     * 删除需求
     */
    void deleteById(Long id);

    /**
     * 匹配历史模板
     */
    AiRequirement matchTemplate(Long id, Long matchedFileId, String matchMode);

    /**
     * 提交AI生成需求任务
     */
    AiTask submitGenerate(Long requirementId, Map<String, Object> params);

    /**
     * 自动保存草稿
     */
    void autoSave(Long requirementId, String content);

    /**
     * 获取自动保存内容
     */
    String getAutoSaveContent(Long requirementId);

    /**
     * 清除自动保存内容
     */
    void clearAutoSave(Long requirementId);

    /**
     * 提交需求检测（敏感词+错别字，2项）
     */
    Map<String, Long> submitDetection(Long requirementId);

    /**
     * 接受需求检测建议（更新handleStatus + 自动修正内容）
     */
    void acceptDetectionIssue(Long requirementId, Long recordId, Integer issueIndex);

    /**
     * 拒绝需求检测建议（更新handleStatus）
     */
    void rejectDetectionIssue(Long requirementId, Long recordId, Integer issueIndex);

    /**
     * 获取需求检测记录列表
     */
    List<AiDetectionRecord> getDetectionRecords(Long requirementId);

    /**
     * 完成需求检测（更新状态为COMPLETED）
     */
    void finishDetection(Long requirementId);

    /**
     * 获取匹配文件列表
     */
    List<MatchFileVO> getMatchFiles(Long requirementId, String keyword);
}

package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.MatchRequest;
import com.jy.eleaitender.ai.dto.response.MatchResultVO;
import com.jy.eleaitender.ai.mapper.AiRequirementMatchMapper;
import com.jy.eleaitender.ai.service.IDocumentMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 文档匹配服务实现
 * 基于数据库关键字匹配 + 规则评分
 */
@Slf4j
@Service
public class DocumentMatchServiceImpl implements IDocumentMatchService {

    @Autowired
    private AiRequirementMatchMapper matchMapper;

    @Override
    public List<MatchResultVO> autoMatch(MatchRequest request) {
        // 查询候选需求（按项目类型匹配）
        List<MatchResultVO> candidates = matchMapper.selectCandidateRequirements(
                request.getProjectType(), request.getProjectCategory(), 50);

        // 计算匹配度评分
        candidates.forEach(c -> c.setSimilarity(calculateSimilarity(c, request)));

        // 按匹配度降序排序，取Top5
        return candidates.stream()
                .sorted(Comparator.comparingInt(MatchResultVO::getSimilarity).reversed())
                .limit(5)
                .toList();
    }

    @Override
    public List<MatchResultVO> manualMatch(MatchRequest request) {
        // 手动模式：返回更多候选项，不强制Top5限制
        List<MatchResultVO> candidates = matchMapper.selectCandidateRequirements(
                request.getProjectType(), null, 20);

        candidates.forEach(c -> c.setSimilarity(calculateSimilarity(c, request)));

        return candidates.stream()
                .sorted(Comparator.comparingInt(MatchResultVO::getSimilarity).reversed())
                .toList();
    }

    /**
     * 计算匹配度评分 (0-100)
     *
     * 评分维度：
     * - 项目类型匹配: 40%
     * - 项目类别匹配: 20%
     * - 预算范围相近: 20%
     * - 关键词匹配: 20%
     */
    private int calculateSimilarity(MatchResultVO candidate, MatchRequest request) {
        int score = 0;

        // 1. 项目类型匹配 (40分)
        if (request.getProjectType() != null && request.getProjectType().equals(candidate.getProjectType())) {
            score += 40;
        }

        // 2. 项目类别匹配 (20分)
        if (request.getProjectCategory() != null && request.getProjectCategory().equals(candidate.getProjectCategory())) {
            score += 20;
        }

        // 3. 预算范围匹配 (20分) — 需要通过VO扩展budget字段（暂用简化逻辑）
        // 由于MatchResultVO没有budget字段用于比对，此处给予基础分
        score += 10;

        // 4. 关键词匹配 (20分)
        if (request.getProjectName() != null && candidate.getRequirementName() != null) {
            score += calculateKeywordScore(request.getProjectName(), candidate.getRequirementName());
        }
        if (request.getDescription() != null && candidate.getContentPreview() != null) {
            score += calculateKeywordScore(request.getDescription(), candidate.getContentPreview()) / 2;
        }

        return Math.min(score, 100);
    }

    /**
     * 简单关键词匹配评分
     * 计算两个文本中共有词的占比
     */
    private int calculateKeywordScore(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0;
        }
        // 简单的字符级包含检查
        String[] keywords = text1.split("[\\s,，、。；;]+");
        int matchCount = 0;
        for (String keyword : keywords) {
            if (keyword.length() >= 2 && text2.contains(keyword)) {
                matchCount++;
            }
        }
        if (keywords.length == 0) return 0;
        double ratio = (double) matchCount / keywords.length;
        return (int) (ratio * 20);
    }
}

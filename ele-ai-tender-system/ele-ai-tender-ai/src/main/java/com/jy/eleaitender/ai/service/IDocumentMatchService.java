package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.ai.dto.request.MatchRequest;
import com.jy.eleaitender.ai.dto.response.MatchResultVO;

import java.util.List;

/**
 * 文档匹配服务接口
 */
public interface IDocumentMatchService {

    /**
     * 自动匹配历史需求（返回Top5）
     */
    List<MatchResultVO> autoMatch(MatchRequest request);

    /**
     * 手动选择匹配（返回候选列表）
     */
    List<MatchResultVO> manualMatch(MatchRequest request);
}

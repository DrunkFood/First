package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleCopyRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentScoreTypeSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEvaluationRulesPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentRuleResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentScoreTypeResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleNode;
import com.jy.eletender.tenderdocument.entity.TenderRuleScoreConfig;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentEvalMethod;
import com.jy.eletender.tenderdocument.enums.TenderDocumentReviewMode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentScoreType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentRuleCategory;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleNodeMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleScoreConfigMapper;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentRuleService;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleTreeAssembler;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleValidator;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import com.jy.eletender.tenderdocument.support.TenderListDisplaySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评审规则服务实现。
 * 职责：负责评审规则树的查询、保存、复制覆盖，以及按评标办法执行节点分类约束。
 */
@Service
public class TenderDocumentRuleServiceImpl implements ITenderDocumentRuleService {

    private static final Logger log = LoggerFactory.getLogger(TenderDocumentRuleServiceImpl.class);

    private final TenderDocumentMapper tenderDocumentMapper;
    private final TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;
    private final TenderRuleHeaderMapper tenderRuleHeaderMapper;
    private final TenderRuleNodeMapper tenderRuleNodeMapper;
    private final TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper;
    private final IProjectLockService projectLockService;
    private final TenderDocumentRuleValidator validator;
    private final TenderDocumentRuleTreeAssembler treeAssembler;
    private final ObjectMapper objectMapper;

    public TenderDocumentRuleServiceImpl(TenderDocumentMapper tenderDocumentMapper,
                                         TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper,
                                         TenderRuleHeaderMapper tenderRuleHeaderMapper,
                                         TenderRuleNodeMapper tenderRuleNodeMapper,
                                         TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper,
                                         IProjectLockService projectLockService,
                                         TenderDocumentRuleValidator validator,
                                         TenderDocumentRuleTreeAssembler treeAssembler,
                                         ObjectMapper objectMapper) {
        this.tenderDocumentMapper = tenderDocumentMapper;
        this.tenderDocumentSnapshotMapper = tenderDocumentSnapshotMapper;
        this.tenderRuleHeaderMapper = tenderRuleHeaderMapper;
        this.tenderRuleNodeMapper = tenderRuleNodeMapper;
        this.tenderRuleScoreConfigMapper = tenderRuleScoreConfigMapper;
        this.projectLockService = projectLockService;
        this.validator = validator;
        this.treeAssembler = treeAssembler;
        this.objectMapper = objectMapper;
    }

    /**
     * 返回评审规则页面的概览数据：评标办法、可操作标段和节点分类列表。
     */
    @Override
    public TenderDocumentEvaluationRulesPageResponse getRulePage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentEvaluationRulesPageResponse response = new TenderDocumentEvaluationRulesPageResponse();
        response.setEvalMethod(tenderDocument.getEvalMethod());
        response.setNodeCategoryList(resolveNodeCategories(tenderDocument.getEvalMethod()));
        response.setTenderList(resolveTenderList(tenderDocument));
        return response;
    }

    /**
     * 保存某个标段某个节点分类的评审规则树。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRuleTree(Long tenderDocumentId, String tenderId, TenderDocumentRuleSaveRequest request, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireEditableDocument(tenderDocumentId, userContext);
        request.setEvalMethod(tenderDocument.getEvalMethod());
        normalizeRuleRequest(request);
        Map<String, List<String>> errors = validator.validateSingleCategory(request);
        if (!errors.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    String.join(";", errors.values().stream().flatMap(List::stream).toList()));
        }

        // 头表按“编制单 + 标段 + 节点分类”唯一命中，节点保存前先清空旧树再整体重建。
        TenderRuleHeader header = tenderRuleHeaderMapper.selectOne(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocumentId)
                .eq(TenderRuleHeader::getTenderId, tenderId)
                .eq(TenderRuleHeader::getNodeCategory, request.getNodeCategory())
                .last("limit 1"));
        if (header == null) {
            header = new TenderRuleHeader();
            header.setTenderDocumentId(tenderDocumentId);
            header.setProjectId(tenderDocument.getProjectId());
            header.setTenderId(tenderId);
            header.setTenderName(request.getTenderName());
            header.setEvalMethod(request.getEvalMethod());
            header.setNodeCategory(request.getNodeCategory());
            header.setReviewMode(request.getReviewMode());
            header.setTotalScore(request.getTotalScore());
            header.setWeightRate(request.getWeightRate());
            tenderRuleHeaderMapper.insert(header);
        } else {
            header.setTenderName(request.getTenderName());
            header.setEvalMethod(request.getEvalMethod());
            header.setReviewMode(request.getReviewMode());
            header.setTotalScore(request.getTotalScore());
            header.setWeightRate(request.getWeightRate());
            tenderRuleHeaderMapper.updateById(header);
            tenderRuleNodeMapper.delete(Wrappers.<TenderRuleNode>lambdaQuery()
                    .eq(TenderRuleNode::getRuleHeaderId, header.getId()));
        }

        saveNodes(tenderDocument, header.getId(), tenderId, request.getTreeToPageData(), null, 1);
    }

    /**
     * 查询某个标段某个节点分类的树形规则。
     */
    @Override
    public TenderDocumentRuleResponse getRuleTree(Long tenderDocumentId, String tenderId, String nodeCategory,
                                                  TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentRuleCategory category = requireSupportedCategory(tenderDocument.getEvalMethod(), nodeCategory);
        TenderRuleHeader header = tenderRuleHeaderMapper.selectOne(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocumentId)
                .eq(TenderRuleHeader::getTenderId, tenderId)
                .eq(TenderRuleHeader::getNodeCategory, nodeCategory)
                .last("limit 1"));
        if (header == null) {
            return buildEmptyRuleResponse(tenderDocument, tenderId, category);
        }

        List<TenderRuleNode> nodes = tenderRuleNodeMapper.selectList(Wrappers.<TenderRuleNode>lambdaQuery()
                .eq(TenderRuleNode::getRuleHeaderId, header.getId())
                .orderByAsc(TenderRuleNode::getLevel, TenderRuleNode::getSortNo));

        TenderDocumentRuleResponse response = new TenderDocumentRuleResponse();
        response.setTenderId(header.getTenderId());
        response.setTenderName(header.getTenderName());
        response.setEvalMethod(header.getEvalMethod());
        response.setNodeCategory(header.getNodeCategory());
        response.setReviewMode(header.getReviewMode());
        response.setScoreType(resolveProjectScoreTypeValue(tenderDocument));
        response.setTotalScore(header.getTotalScore());
        response.setWeightRate(header.getWeightRate());
        response.setTreeToPageData(treeAssembler.buildTree(nodes));
        return response;
    }

    /**
     * 复制来源标段的整套评审规则到目标标段，并完全覆盖目标标段原有规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyRuleTree(Long tenderDocumentId, TenderDocumentRuleCopyRequest request, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireEditableDocument(tenderDocumentId, userContext);
        List<TenderRuleHeader> sourceHeaders = tenderRuleHeaderMapper.selectList(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocumentId)
                .eq(TenderRuleHeader::getTenderId, request.getSourceTenderId())
                .orderByAsc(TenderRuleHeader::getId));
        if (sourceHeaders == null || sourceHeaders.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_NOT_FOUND.getCode(), "来源标段评审规则不存在");
        }

        List<TenderRuleHeader> targetHeaders = tenderRuleHeaderMapper.selectList(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocumentId)
                .eq(TenderRuleHeader::getTenderId, request.getTargetTenderId()));
        Map<String, TenderRuleHeader> targetHeaderMap = targetHeaders.stream()
                .collect(Collectors.toMap(TenderRuleHeader::getNodeCategory, header -> header, (left, right) -> left));
        Set<String> sourceCategories = sourceHeaders.stream()
                .map(TenderRuleHeader::getNodeCategory)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 目标标段中“来源不存在的分类”直接删除，确保复制语义是完整覆盖而不是增量合并。
        for (TenderRuleHeader targetHeader : targetHeaders) {
            if (sourceCategories.contains(targetHeader.getNodeCategory())) {
                continue;
            }
            tenderRuleNodeMapper.delete(Wrappers.<TenderRuleNode>lambdaQuery()
                    .eq(TenderRuleNode::getRuleHeaderId, targetHeader.getId()));
            tenderRuleHeaderMapper.deleteById(targetHeader.getId());
        }

        for (TenderRuleHeader sourceHeader : sourceHeaders) {
            TenderRuleHeader targetHeader = targetHeaderMap.get(sourceHeader.getNodeCategory());
            boolean existingTarget = targetHeader != null;
            if (!existingTarget) {
                targetHeader = new TenderRuleHeader();
                targetHeader.setTenderDocumentId(tenderDocumentId);
                targetHeader.setProjectId(tenderDocument.getProjectId());
                targetHeader.setTenderId(request.getTargetTenderId());
                targetHeader.setNodeCategory(sourceHeader.getNodeCategory());
            }
            targetHeader.setTenderName(StringUtils.hasText(request.getTargetTenderName())
                    ? request.getTargetTenderName()
                    : sourceHeader.getTenderName());
            targetHeader.setEvalMethod(sourceHeader.getEvalMethod());
            targetHeader.setReviewMode(sourceHeader.getReviewMode());
            targetHeader.setTotalScore(sourceHeader.getTotalScore());
            targetHeader.setWeightRate(sourceHeader.getWeightRate());

            if (existingTarget) {
                // 先清空目标旧节点，再按来源树重建，避免遗留孤儿节点和层级错位。
                tenderRuleNodeMapper.delete(Wrappers.<TenderRuleNode>lambdaQuery()
                        .eq(TenderRuleNode::getRuleHeaderId, targetHeader.getId()));
                tenderRuleHeaderMapper.updateById(targetHeader);
            } else {
                tenderRuleHeaderMapper.insert(targetHeader);
            }

            List<TenderRuleNode> sourceNodes = tenderRuleNodeMapper.selectList(Wrappers.<TenderRuleNode>lambdaQuery()
                    .eq(TenderRuleNode::getRuleHeaderId, sourceHeader.getId())
                    .orderByAsc(TenderRuleNode::getLevel, TenderRuleNode::getSortNo, TenderRuleNode::getId));
            Map<Long, Long> idMapping = new HashMap<>();
            for (TenderRuleNode sourceNode : sourceNodes) {
                TenderRuleNode targetNode = new TenderRuleNode();
                targetNode.setTenderDocumentId(tenderDocumentId);
                targetNode.setRuleHeaderId(targetHeader.getId());
                targetNode.setProjectId(tenderDocument.getProjectId());
                targetNode.setTenderId(request.getTargetTenderId());
                // 复制后主键重建，父子关系必须通过“来源ID -> 新ID”映射恢复。
                targetNode.setParentId(sourceNode.getParentId() == null ? null : idMapping.get(sourceNode.getParentId()));
                targetNode.setLevel(sourceNode.getLevel());
                targetNode.setSortNo(sourceNode.getSortNo());
                targetNode.setItemContent(sourceNode.getItemContent());
                targetNode.setScoreMin(sourceNode.getScoreMin());
                targetNode.setScoreMax(sourceNode.getScoreMax());
                targetNode.setScoreStandard(sourceNode.getScoreStandard());
                targetNode.setScoreAttribute(sourceNode.getScoreAttribute());
                targetNode.setLeafFlag(sourceNode.getLeafFlag());
                tenderRuleNodeMapper.insert(targetNode);
                idMapping.put(sourceNode.getId(), targetNode.getId());
            }
        }
    }

    @Override
    public TenderDocumentScoreTypeResponse getScoreType(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentScoreTypeResponse response = new TenderDocumentScoreTypeResponse();
        response.setScoreType(resolveProjectScoreTypeValue(tenderDocument));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScoreType(Long tenderDocumentId, TenderDocumentScoreTypeSaveRequest request, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireEditableDocument(tenderDocumentId, userContext);
        String normalizedScoreType = normalizeScoreTypeValue(tenderDocument, request == null ? null : request.getScoreType());
        TenderRuleScoreConfig existing = tenderRuleScoreConfigMapper.selectOne(Wrappers.<TenderRuleScoreConfig>lambdaQuery()
                .eq(TenderRuleScoreConfig::getTenderDocumentId, tenderDocumentId)
                .last("limit 1"));
        if (!StringUtils.hasText(normalizedScoreType)) {
            if (existing != null) {
                tenderRuleScoreConfigMapper.deleteById(existing.getId());
            }
            return;
        }

        if (existing == null) {
            existing = new TenderRuleScoreConfig();
            existing.setTenderDocumentId(tenderDocumentId);
            existing.setScoreType(normalizedScoreType);
            tenderRuleScoreConfigMapper.insert(existing);
            return;
        }
        existing.setScoreType(normalizedScoreType);
        tenderRuleScoreConfigMapper.updateById(existing);
    }

    /**
     * 统一做编制单存在和项目锁校验。
     */
    private TenderDocument requireDocument(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = tenderDocumentMapper.selectById(tenderDocumentId);
        if (tenderDocument == null) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_FOUND.getMessage());
        }
        ProjectLock ignored = projectLockService.verifyOrCreateLock(tenderDocument.getProjectId(), userContext);
        return tenderDocument;
    }

    /**
     * 写操作前校验编制单存在 + 项目锁 + 仍可编辑（DRAFT 状态）。
     */
    private TenderDocument requireEditableDocument(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        if (!TenderDocumentStatus.DRAFT.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getMessage());
        }
        return tenderDocument;
    }

    /**
     * 评审规则页的标段列表优先从基本信息快照取，快照缺失时再退化为当前编制单信息。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveTenderList(TenderDocument tenderDocument) {
        TenderDocumentSnapshot snapshot = tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocument.getId())
                .eq(TenderDocumentSnapshot::getStepCode, TenderDocumentStepCode.BASIC_INFO.name())
                .last("limit 1"));
        if (snapshot != null && StringUtils.hasText(snapshot.getSnapshotJson())) {
            try {
                Map<String, Object> payload = objectMapper.readValue(snapshot.getSnapshotJson(), Map.class);
                Object tenderList = payload.get("tenderList");
                if (tenderList instanceof List<?> list) {
                    return TenderListDisplaySupport.normalize((List<Map<String, Object>>) list);
                }
            } catch (Exception e) {
                log.warn("解析编制单 {} 基本信息快照失败，使用默认标段信息", tenderDocument.getId(), e);
            }
        }
        Map<String, Object> tender = new HashMap<>();
        tender.put("tenderId", tenderDocument.getTenderId());
        tender.put("tenderName", tenderDocument.getTenderId());
        tender.put("indexOf", tenderDocument.getIndexOf());
        return TenderListDisplaySupport.normalize(List.of(tender));
    }

    /**
     * 节点分类列表直接由评标办法推导，保持页面展示和校验规则一致。
     */
    private List<String> resolveNodeCategories(String evalMethod) {
        Set<String> categories = TenderDocumentRuleCategory.categoriesOf(TenderDocumentEvalMethod.fromString(evalMethod)).stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(categories);
    }

    private void normalizeRuleRequest(TenderDocumentRuleSaveRequest request) {
        TenderDocumentEvalMethod evalMethod = TenderDocumentEvalMethod.fromString(request.getEvalMethod());
        TenderDocumentRuleCategory category = requireSupportedCategory(evalMethod.name(), request.getNodeCategory());
        // reviewMode 以后端规则为准：PASS 类清空分值字段，SCORE 类保留分值配置。
        TenderDocumentReviewMode reviewMode = category.fixedReviewMode();
        request.setReviewMode(reviewMode.name());
        if (reviewMode == TenderDocumentReviewMode.PASS) {
            request.setTotalScore(null);
            request.setWeightRate(null);
        }
    }

    private TenderDocumentRuleCategory requireSupportedCategory(String evalMethod, String nodeCategory) {
        TenderDocumentEvalMethod resolvedEvalMethod = TenderDocumentEvalMethod.fromString(evalMethod);
        TenderDocumentRuleCategory category;
        try {
            category = TenderDocumentRuleCategory.valueOf(nodeCategory);
        } catch (Exception ex) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    "非法的评审规则分类: " + nodeCategory);
        }
        if (!TenderDocumentRuleCategory.supports(resolvedEvalMethod, category)) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    "评标办法 " + resolvedEvalMethod.name() + " 不支持评审分类 " + category.name());
        }
        return category;
    }

    private TenderDocumentRuleResponse buildEmptyRuleResponse(TenderDocument tenderDocument, String tenderId,
                                                              TenderDocumentRuleCategory category) {
        TenderDocumentRuleResponse response = new TenderDocumentRuleResponse();
        response.setTenderId(tenderId);
        response.setTenderName(resolveTenderName(tenderDocument, tenderId));
        response.setEvalMethod(tenderDocument.getEvalMethod());
        response.setNodeCategory(category.name());
        response.setReviewMode(category.fixedReviewMode().name());
        response.setScoreType(resolveProjectScoreTypeValue(tenderDocument));
        response.setTreeToPageData(List.of(new TenderDocumentRuleTreeNode()));
        return response;
    }

    private String resolveTenderName(TenderDocument tenderDocument, String tenderId) {
        return resolveTenderList(tenderDocument).stream()
                .filter(item -> tenderId.equals(item.get("tenderId")))
                .map(item -> item.get("tenderName"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * 前端传树、后端落平表：通过 parentId + level + sortNo 保持层级关系。
     */
    private void saveNodes(TenderDocument tenderDocument, Long headerId, String tenderId,
                           List<TenderDocumentRuleTreeNode> nodes, Long parentId, int level) {
        if (nodes == null) {
            return;
        }
        for (TenderDocumentRuleTreeNode treeNode : nodes) {
            TenderRuleNode entity = new TenderRuleNode();
            entity.setTenderDocumentId(tenderDocument.getId());
            entity.setRuleHeaderId(headerId);
            entity.setProjectId(tenderDocument.getProjectId());
            entity.setTenderId(tenderId);
            entity.setParentId(parentId);
            entity.setLevel(level);
            entity.setSortNo(treeNode.getOrder());
            entity.setItemContent(treeNode.getName());
            entity.setScoreMin(treeNode.getLowest());
            entity.setScoreMax(treeNode.getHighest());
            entity.setScoreStandard(treeNode.getStandard());
            entity.setScoreAttribute(treeNode.getObjectiveType());
            entity.setLeafFlag(treeNode.getChildren() == null || treeNode.getChildren().isEmpty() ? 1 : 0);
            tenderRuleNodeMapper.insert(entity);
            saveNodes(tenderDocument, headerId, tenderId, treeNode.getChildren(), entity.getId(), level + 1);
        }
    }

    private String resolveProjectScoreTypeValue(TenderDocument tenderDocument) {
        if (TenderDocumentEvalMethod.fromString(tenderDocument.getEvalMethod()) == TenderDocumentEvalMethod.LOWEST_PRICE) {
            return null;
        }
        TenderRuleScoreConfig scoreConfig = tenderRuleScoreConfigMapper.selectOne(Wrappers.<TenderRuleScoreConfig>lambdaQuery()
                .eq(TenderRuleScoreConfig::getTenderDocumentId, tenderDocument.getId())
                .last("limit 1"));
        return scoreConfig == null ? null : scoreConfig.getScoreType();
    }

    private String normalizeScoreTypeValue(TenderDocument tenderDocument, String scoreType) {
        if (!StringUtils.hasText(scoreType)) {
            return null;
        }
        String normalized = scoreType.trim().toUpperCase();
        if (TenderDocumentEvalMethod.fromString(tenderDocument.getEvalMethod()) == TenderDocumentEvalMethod.LOWEST_PRICE) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    "当前评标办法下scoreType必须为空");
        }
        try {
            return TenderDocumentScoreType.valueOf(normalized).name();
        } catch (Exception ex) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    "非法的分值类型: " + scoreType);
        }
    }
}

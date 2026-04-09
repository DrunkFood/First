package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBasicInfoPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentBidRecordPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCheckItemView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentCheckItemsResponse;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderDocumentStep;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleScoreConfig;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentEvalMethod;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentReviewMode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentRuleCategory;
import com.jy.eletender.tenderdocument.enums.TenderDocumentScoreType;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepStatus;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentStepMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleScoreConfigMapper;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.service.ITenderDocumentStepService;
import com.jy.eletender.tenderdocument.support.TenderDocumentBasicInfoSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentBidRecordSyncResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentCompileScopeResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleValidator;
import com.jy.eletender.tenderdocument.support.TenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import com.jy.eletender.tenderdocument.support.TenderListDisplaySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 招标文件步骤推进服务实现。
 * 职责：管理各步骤同步快照、完成校验与状态流转，并在关键节点执行规则完整性检查。
 */
@Slf4j
@Service
public class TenderDocumentStepServiceImpl implements ITenderDocumentStepService {

    private final TenderDocumentMapper tenderDocumentMapper;
    private final TenderDocumentStepMapper tenderDocumentStepMapper;
    private final TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;
    private final TenderDocumentFileMapper tenderDocumentFileMapper;
    private final TenderRuleHeaderMapper tenderRuleHeaderMapper;
    private final TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper;
    private final TenderDocumentSyncGateway tenderDocumentSyncGateway;
    private final ObjectMapper objectMapper;
    private final IProjectLockService projectLockService;
    private final TenderDocumentRuleValidator tenderDocumentRuleValidator;

    public TenderDocumentStepServiceImpl(TenderDocumentMapper tenderDocumentMapper,
                                         TenderDocumentStepMapper tenderDocumentStepMapper,
                                         TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper,
                                         TenderDocumentFileMapper tenderDocumentFileMapper,
                                         TenderRuleHeaderMapper tenderRuleHeaderMapper,
                                         TenderRuleScoreConfigMapper tenderRuleScoreConfigMapper,
                                         TenderDocumentSyncGateway tenderDocumentSyncGateway,
                                         ObjectMapper objectMapper,
                                         IProjectLockService projectLockService,
                                         TenderDocumentRuleValidator tenderDocumentRuleValidator) {
        this.tenderDocumentMapper = tenderDocumentMapper;
        this.tenderDocumentStepMapper = tenderDocumentStepMapper;
        this.tenderDocumentSnapshotMapper = tenderDocumentSnapshotMapper;
        this.tenderDocumentFileMapper = tenderDocumentFileMapper;
        this.tenderRuleHeaderMapper = tenderRuleHeaderMapper;
        this.tenderRuleScoreConfigMapper = tenderRuleScoreConfigMapper;
        this.tenderDocumentSyncGateway = tenderDocumentSyncGateway;
        this.objectMapper = objectMapper;
        this.projectLockService = projectLockService;
        this.tenderDocumentRuleValidator = tenderDocumentRuleValidator;
    }

    /**
     * 返回基本信息页展示数据，实际内容来自步骤快照。
     */
    @Override
    public TenderDocumentBasicInfoPageResponse getBasicInfoPage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentSnapshot snapshot = getSnapshot(tenderDocumentId, TenderDocumentStepCode.BASIC_INFO);
        return toBasicInfoPageResponse(tenderDocument, snapshot);
    }

    /**
     * 同步项目基本信息并刷新快照，同时把项目基础上下文回写到编制单主表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentBasicInfoPageResponse syncBasicInfo(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        requireEditableDocument(tenderDocument);
        TenderDocumentBasicInfoSyncResult payload = tenderDocumentSyncGateway.syncBasicInfo(tenderDocument, userContext);
        Date now = new Date();
        updateDocumentFromBasicInfoSync(tenderDocument, payload);
        upsertSnapshot(tenderDocumentId, TenderDocumentStepCode.BASIC_INFO, payload, now);
        tenderDocument.setLatestBasicInfoSyncTime(now);
        tenderDocumentMapper.updateById(tenderDocument);
        return getBasicInfoPage(tenderDocumentId, userContext);
    }

    /**
     * 返回开标标录页展示数据。
     */
    @Override
    public TenderDocumentBidRecordPageResponse getBidRecordPage(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentSnapshot snapshot = getSnapshot(tenderDocumentId, TenderDocumentStepCode.BID_RECORD);
        return toBidRecordPageResponse(tenderDocument, snapshot);
    }

    /**
     * 同步标录信息并刷新标录快照。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenderDocumentBidRecordPageResponse syncBidRecord(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        requireEditableDocument(tenderDocument);
        TenderDocumentBidRecordSyncResult payload = tenderDocumentSyncGateway.syncBidRecord(tenderDocument, userContext);
        Date now = new Date();
        upsertSnapshot(tenderDocumentId, TenderDocumentStepCode.BID_RECORD, payload, now);
        tenderDocument.setLatestBidRecordSyncTime(now);
        tenderDocumentMapper.updateById(tenderDocument);
        return getBidRecordPage(tenderDocumentId, userContext);
    }

    /**
     * 完成当前步骤并推进到下一步。页面保存和步骤完成是两套动作，这里只负责完整校验与推进。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeStep(Long tenderDocumentId, TenderDocumentStepCode stepCode, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        requireEditableDocument(tenderDocument);
        TenderDocumentStep currentStep = requireCurrentOrCompletedStep(tenderDocumentId, stepCode);
        validateStepBeforeComplete(tenderDocument, stepCode);

        currentStep.setStepStatus(TenderDocumentStepStatus.COMPLETED.name());
        currentStep.setCompleteTime(new Date());
        tenderDocumentStepMapper.updateById(currentStep);

        List<TenderDocumentStepCode> orderedSteps = TenderDocumentStepCode.orderedValues();
        int currentIndex = orderedSteps.indexOf(stepCode);
        if (currentIndex >= 0 && currentIndex < orderedSteps.size() - 1) {
            TenderDocumentStepCode nextStepCode = orderedSteps.get(currentIndex + 1);
            TenderDocumentStep nextStep = tenderDocumentStepMapper.selectOne(Wrappers.<TenderDocumentStep>lambdaQuery()
                    .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId)
                    .eq(TenderDocumentStep::getStepCode, nextStepCode.name())
                    .last("limit 1"));
            if (nextStep != null) {
                // 步骤推进采用“当前完成 -> 下一步进行中”的单向状态切换。
                nextStep.setStepStatus(TenderDocumentStepStatus.IN_PROGRESS.name());
                tenderDocumentStepMapper.updateById(nextStep);
                tenderDocument.setCurrentStepCode(nextStepCode.name());
                tenderDocumentMapper.updateById(tenderDocument);
            }
        }
    }

    @Override
    public TenderDocumentCheckItemsResponse getCheckItems(Long tenderDocumentId, TenderDocumentUserContext userContext) {
        TenderDocument tenderDocument = requireDocument(tenderDocumentId, userContext);
        TenderDocumentCheckItemsResponse response = new TenderDocumentCheckItemsResponse();
        response.getItemList().add(checkBasicInfo(tenderDocument, userContext));
        response.getItemList().add(checkPurchaseFile(tenderDocument));
        response.getItemList().add(checkBidRecord(tenderDocument, userContext));
        response.getItemList().add(checkEvaluationRule(tenderDocument));
        return response;
    }

    /**
     * BASIC_INFO 检查：重新拉取业务系统最新数据，比对 hash 判断是否有更新。
     */
    private TenderDocumentCheckItemView checkBasicInfo(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        TenderDocumentCheckItemView view = buildCheckItemView(TenderDocumentStepCode.BASIC_INFO);
        try {
            TenderDocumentSnapshot snapshot = getSnapshot(tenderDocument.getId(), TenderDocumentStepCode.BASIC_INFO);
            if (snapshot == null || snapshot.getSnapshotJson() == null) {
                view.setPassed(Boolean.FALSE);
                view.setMessage("项目基本信息尚未同步");
                return view;
            }
            TenderDocumentBasicInfoSyncResult latest = tenderDocumentSyncGateway.syncBasicInfo(tenderDocument, userContext);
            String latestHash = DigestUtil.sha256Hex(objectMapper.writeValueAsString(latest));
            if (latestHash.equals(snapshot.getSourceHash())) {
                view.setPassed(Boolean.TRUE);
                view.setMessage("项目基本信息已是最新");
            } else {
                view.setPassed(Boolean.FALSE);
                view.setMessage("项目基本信息已更新，请重新同步");
            }
        } catch (Exception e) {
            view.setPassed(Boolean.FALSE);
            view.setMessage("项目基本信息同步检查失败: " + e.getMessage());
        }
        return view;
    }

    /**
     * PURCHASE_FILE 检查：检查是否存在 active 的签章 PDF 文件。
     */
    private TenderDocumentCheckItemView checkPurchaseFile(TenderDocument tenderDocument) {
        TenderDocumentCheckItemView view = buildCheckItemView(TenderDocumentStepCode.PURCHASE_FILE);
        try {
            TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
            TenderDocumentFile file = tenderDocumentFileMapper.selectOne(Wrappers.<TenderDocumentFile>lambdaQuery()
                    .eq(TenderDocumentFile::getTenderDocumentId, tenderDocument.getId())
                    .eq(TenderDocumentFile::getScopeType, scopeType.name())
                    .eq(TenderDocumentFile::getFileRole, TenderDocumentFileType.SIGNED_PDF.name())
                    .eq(TenderDocumentFile::getActiveFlag, 1)
                    .last("limit 1"));
            if (file != null) {
                view.setPassed(Boolean.TRUE);
                view.setMessage("签章文件已上传");
            } else {
                view.setPassed(Boolean.FALSE);
                view.setMessage("签章文件尚未上传");
            }
        } catch (Exception e) {
            view.setPassed(Boolean.FALSE);
            view.setMessage("签章文件检查失败: " + e.getMessage());
        }
        return view;
    }

    /**
     * BID_RECORD 检查：重新拉取业务系统最新标录数据，比对 hash 判断是否有更新。
     */
    private TenderDocumentCheckItemView checkBidRecord(TenderDocument tenderDocument, TenderDocumentUserContext userContext) {
        TenderDocumentCheckItemView view = buildCheckItemView(TenderDocumentStepCode.BID_RECORD);
        try {
            TenderDocumentSnapshot snapshot = getSnapshot(tenderDocument.getId(), TenderDocumentStepCode.BID_RECORD);
            if (snapshot == null || snapshot.getSnapshotJson() == null) {
                view.setPassed(Boolean.FALSE);
                view.setMessage("开标标录信息尚未同步");
                return view;
            }
            TenderDocumentBidRecordSyncResult latest = tenderDocumentSyncGateway.syncBidRecord(tenderDocument, userContext);
            String latestHash = DigestUtil.sha256Hex(objectMapper.writeValueAsString(latest));
            if (latestHash.equals(snapshot.getSourceHash())) {
                view.setPassed(Boolean.TRUE);
                view.setMessage("开标标录信息已是最新");
            } else {
                view.setPassed(Boolean.FALSE);
                view.setMessage("开标标录信息已更新，请重新同步");
            }
        } catch (Exception e) {
            view.setPassed(Boolean.FALSE);
            view.setMessage("开标标录信息同步检查失败: " + e.getMessage());
        }
        return view;
    }

    /**
     * EVALUATION_RULE 检查：复用已有的评审规则完整校验逻辑。
     */
    private TenderDocumentCheckItemView checkEvaluationRule(TenderDocument tenderDocument) {
        TenderDocumentCheckItemView view = buildCheckItemView(TenderDocumentStepCode.EVALUATION_RULE);
        try {
            validateEvaluationRules(tenderDocument.getId(), tenderDocument.getEvalMethod());
            view.setPassed(Boolean.TRUE);
            view.setMessage("评审规则校验通过");
        } catch (BusinessException e) {
            view.setPassed(Boolean.FALSE);
            view.setMessage(e.getMessage());
        } catch (Exception e) {
            view.setPassed(Boolean.FALSE);
            view.setMessage("评审规则校验失败: " + e.getMessage());
        }
        return view;
    }

    private TenderDocumentCheckItemView buildCheckItemView(TenderDocumentStepCode stepCode) {
        TenderDocumentCheckItemView view = new TenderDocumentCheckItemView();
        view.setItemCode(stepCode.name());
        view.setItemName(stepCode.getStepName());
        return view;
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
     * 只有 DRAFT 状态的编制单允许继续推进步骤。
     */
    private void requireEditableDocument(TenderDocument tenderDocument) {
        if (!TenderDocumentStatus.DRAFT.name().equals(tenderDocument.getStatus())) {
            throw new BusinessException(TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getCode(),
                    TenderDocumentErrorCode.TENDER_DOCUMENT_NOT_EDITABLE.getMessage());
        }
    }

    /**
     * 页面允许查看已完成步骤，因此这里放宽为“当前步骤或已完成步骤”都能命中。
     */
    private TenderDocumentStep requireCurrentOrCompletedStep(Long tenderDocumentId, TenderDocumentStepCode stepCode) {
        TenderDocumentStep step = tenderDocumentStepMapper.selectOne(Wrappers.<TenderDocumentStep>lambdaQuery()
                .eq(TenderDocumentStep::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentStep::getStepCode, stepCode.name())
                .last("limit 1"));
        if (step == null) {
            throw new BusinessException(TenderDocumentErrorCode.STEP_NOT_FOUND.getCode(),
                    TenderDocumentErrorCode.STEP_NOT_FOUND.getMessage());
        }
        return step;
    }

    /**
     * 步骤完成前按不同页面执行对应的业务校验。
     */
    private void validateStepBeforeComplete(TenderDocument tenderDocument, TenderDocumentStepCode stepCode) {
        Long tenderDocumentId = tenderDocument.getId();
        if (stepCode == TenderDocumentStepCode.BASIC_INFO || stepCode == TenderDocumentStepCode.BID_RECORD) {
            TenderDocumentSnapshot snapshot = tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                    .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocumentId)
                    .eq(TenderDocumentSnapshot::getStepCode, stepCode.name())
                    .last("limit 1"));
            if (snapshot == null || snapshot.getSnapshotJson() == null) {
                throw new BusinessException(stepCode == TenderDocumentStepCode.BASIC_INFO
                        ? TenderDocumentErrorCode.BASIC_INFO_NOT_SYNCED.getCode()
                        : TenderDocumentErrorCode.BID_RECORD_NOT_SYNCED.getCode(),
                        stepCode == TenderDocumentStepCode.BASIC_INFO
                                ? TenderDocumentErrorCode.BASIC_INFO_NOT_SYNCED.getMessage()
                                : TenderDocumentErrorCode.BID_RECORD_NOT_SYNCED.getMessage());
            }
            return;
        }
        if (stepCode == TenderDocumentStepCode.PURCHASE_FILE) {
            requireActiveFile(tenderDocument, TenderDocumentFileType.PURCHASE_SOURCE_PDF, "采购文件尚未上传，不能完成当前步骤");
            requireActiveFile(tenderDocument, TenderDocumentFileType.SIGNED_PDF, "签章文件尚未上传，不能完成当前步骤");
            return;
        }
        if (stepCode == TenderDocumentStepCode.EVALUATION_RULE) {
            validateEvaluationRules(tenderDocumentId, tenderDocument.getEvalMethod());
        }
    }

    /**
     * 采购文件相关文件都通过文件绑定关系判断是否完成，不直接访问文件服务。
     */
    private void requireActiveFile(TenderDocument tenderDocument, TenderDocumentFileType fileRole, String message) {
        TenderDocumentScopeType scopeType = resolveScopeType(tenderDocument);
        TenderDocumentFile file = tenderDocumentFileMapper.selectOne(Wrappers.<TenderDocumentFile>lambdaQuery()
                .eq(TenderDocumentFile::getTenderDocumentId, tenderDocument.getId())
                .eq(TenderDocumentFile::getScopeType, scopeType.name())
                .eq(TenderDocumentFile::getFileRole, fileRole.name())
                .eq(TenderDocumentFile::getActiveFlag, 1)
                .last("limit 1"));
        if (file == null) {
            throw new BusinessException(TenderDocumentErrorCode.FILE_BINDING_NOT_FOUND.getCode(), message);
        }
    }

    private TenderDocumentScopeType resolveScopeType(TenderDocument tenderDocument) {
        return TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())
                ? TenderDocumentScopeType.TENDER
                : TenderDocumentScopeType.PROJECT;
    }

    /**
     * 评审规则完成校验既校验分类完整性，也校验综合评分法下的跨分类约束。
     * 多标段场景下，所有定义的标段都必须配齐规则，不能遗漏。
     */
    private void validateEvaluationRules(Long tenderDocumentId, String evalMethod) {
        List<TenderRuleHeader> headers = tenderRuleHeaderMapper.selectList(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocumentId));
        if (headers == null || headers.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_NOT_FOUND.getCode(), "评审规则尚未配置，不能完成当前步骤");
        }

        // 按标段分组逐一校验，保证每个标段都满足分类完整性和评分法约束。
        Map<String, List<TenderRuleHeader>> headersByTenderId = headers.stream()
                .collect(Collectors.groupingBy(TenderRuleHeader::getTenderId));

        // 从基本信息快照获取标段列表，确保所有定义的标段都被校验（包括尚未配置任何规则的标段）。
        Map<String, String> tenderNameMap = resolveDefinedTenderNameMap(tenderDocumentId);
        Set<String> definedTenderIds = tenderNameMap.keySet();
        if (!definedTenderIds.isEmpty()) {
            for (String definedTenderId : definedTenderIds) {
                if (!headersByTenderId.containsKey(definedTenderId)) {
                    String tenderName = tenderNameMap.getOrDefault(definedTenderId, definedTenderId);
                    throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                            tenderName + " 尚未配置评审规则");
                }
            }
        }

        for (Map.Entry<String, List<TenderRuleHeader>> entry : headersByTenderId.entrySet()) {
            String tenderName = tenderNameMap.getOrDefault(entry.getKey(), entry.getKey());
            Set<String> categories = entry.getValue().stream()
                    .map(TenderRuleHeader::getNodeCategory)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            validateRequiredCategories(evalMethod, categories, tenderName);
            if (TenderDocumentEvalMethod.fromString(evalMethod) == TenderDocumentEvalMethod.COMPREHENSIVE_SCORE) {
                validateComprehensiveRuleHeaders(entry.getValue());
            }
        }
    }

    /**
     * 从基本信息快照中获取标段 ID → 标段名称的映射。
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> resolveDefinedTenderNameMap(Long tenderDocumentId) {
        TenderDocumentSnapshot snapshot = tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentSnapshot::getStepCode, TenderDocumentStepCode.BASIC_INFO.name())
                .last("limit 1"));
        if (snapshot == null || snapshot.getSnapshotJson() == null) {
            return Map.of();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(snapshot.getSnapshotJson(), Map.class);
            Object tenderList = payload.get("tenderList");
            if (tenderList instanceof List<?> list) {
                Map<String, String> result = new LinkedHashMap<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> tender) {
                        Object id = tender.get("tenderId");
                        Object name = tender.get("tenderName");
                        if (id != null) {
                            result.put(String.valueOf(id),
                                    name != null ? String.valueOf(name) : String.valueOf(id));
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("解析编制单 {} 基本信息快照失败，退化为不校验标段完整性", tenderDocumentId, e);
        }
        return Map.of();
    }

    /**
     * 必填分类由评标办法决定，统一通过枚举推导，避免 service 里重复硬编码。
     */
    private void validateRequiredCategories(String evalMethod, Set<String> categories, String tenderName) {
        List<TenderDocumentRuleCategory> requiredList = TenderDocumentRuleCategory.categoriesOf(TenderDocumentEvalMethod.fromString(evalMethod));
        List<String> missingDisplayNames = new ArrayList<>();
        for (TenderDocumentRuleCategory required : requiredList) {
            if (!categories.contains(required.name())) {
                missingDisplayNames.add(required.getDisplayName());
            }
        }
        if (!missingDisplayNames.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    tenderName + " 评审规则分类不完整，缺少: " + String.join("、", missingDisplayNames));
        }
    }

    /**
     * 综合评分法下只抽取打分制头信息做跨分类联合校验。
     */
    private void validateComprehensiveRuleHeaders(List<TenderRuleHeader> headers) {
        List<TenderDocumentRuleSaveRequest> scoreRequests = headers.stream()
                .filter(header -> TenderDocumentReviewMode.SCORE.name().equals(header.getReviewMode()))
                .map(header -> {
                    TenderDocumentRuleSaveRequest request = new TenderDocumentRuleSaveRequest();
                    request.setTenderId(header.getTenderId());
                    request.setNodeCategory(header.getNodeCategory());
                    request.setReviewMode(header.getReviewMode());
                    request.setTotalScore(header.getTotalScore());
                    request.setWeightRate(header.getWeightRate());
                    return request;
                })
                .toList();
        String scoreType = resolveProjectScoreType(headers.isEmpty() ? null : headers.get(0).getTenderDocumentId());
        Map<String, List<String>> errors = tenderDocumentRuleValidator.validateCrossCategory(scoreRequests, scoreType);
        if (!errors.isEmpty()) {
            throw new BusinessException(TenderDocumentErrorCode.RULE_VALIDATION_FAILED.getCode(),
                    String.join(";", errors.values().stream().flatMap(List::stream).toList()));
        }
    }

    private String resolveProjectScoreType(Long tenderDocumentId) {
        if (tenderDocumentId == null) {
            return null;
        }
        TenderRuleScoreConfig scoreConfig = tenderRuleScoreConfigMapper.selectOne(Wrappers.<TenderRuleScoreConfig>lambdaQuery()
                .eq(TenderRuleScoreConfig::getTenderDocumentId, tenderDocumentId)
                .last("limit 1"));
        return scoreConfig == null ? null : normalizeScoreType(scoreConfig.getScoreType());
    }

    private String normalizeScoreType(String scoreType) {
        if (scoreType == null || scoreType.isBlank()) {
            return null;
        }
        return TenderDocumentScoreType.valueOf(scoreType.trim().toUpperCase()).name();
    }

    /**
     * 统一保存步骤快照。基本信息和标录都走相同的快照持久化模型。
     */
    private void upsertSnapshot(Long tenderDocumentId, TenderDocumentStepCode stepCode, Object payload, Date sourceSyncTime) {
        try {
            String snapshotJson = objectMapper.writeValueAsString(payload);
            String sourceHash = DigestUtil.sha256Hex(snapshotJson);
            TenderDocumentSnapshot snapshot = tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                    .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocumentId)
                    .eq(TenderDocumentSnapshot::getStepCode, stepCode.name())
                    .last("limit 1"));
            if (snapshot == null) {
                // 首次同步直接落新快照。
                snapshot = new TenderDocumentSnapshot();
                snapshot.setTenderDocumentId(tenderDocumentId);
                snapshot.setStepCode(stepCode.name());
                snapshot.setSnapshotJson(snapshotJson);
                snapshot.setSourceHash(sourceHash);
                snapshot.setSourceSyncTime(sourceSyncTime);
                tenderDocumentSnapshotMapper.insert(snapshot);
            } else {
                // 二次同步覆盖快照内容并刷新来源哈希，保留同一步骤单份快照语义。
                snapshot.setSnapshotJson(snapshotJson);
                snapshot.setSourceHash(sourceHash);
                if (sourceSyncTime != null) {
                    snapshot.setSourceSyncTime(sourceSyncTime);
                }
                tenderDocumentSnapshotMapper.updateById(snapshot);
            }
        } catch (Exception e) {
            throw new BusinessException("步骤数据保存失败: " + e.getMessage());
        }
    }

    private TenderDocumentSnapshot getSnapshot(Long tenderDocumentId, TenderDocumentStepCode stepCode) {
        return tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentSnapshot::getStepCode, stepCode.name())
                .last("limit 1"));
    }

    /**
     * 解析基本信息快照为页面对象。
     */
    @SuppressWarnings("unchecked")
    private TenderDocumentBasicInfoPageResponse toBasicInfoPageResponse(TenderDocument tenderDocument, TenderDocumentSnapshot snapshot) {
        TenderDocumentBasicInfoPageResponse response = new TenderDocumentBasicInfoPageResponse();
        if (snapshot == null || snapshot.getSnapshotJson() == null) {
            response.setLastSyncTime(tenderDocument.getLatestBasicInfoSyncTime());
            return response;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(snapshot.getSnapshotJson(), Map.class);
            Object projectInfo = payload.get("projectInfo");
            Object tenderList = payload.get("tenderList");
            if (projectInfo instanceof Map<?, ?> map) {
                response.setProjectInfo((Map<String, Object>) map);
            }
            if (tenderList instanceof List<?> list) {
                response.setTenderList(TenderListDisplaySupport.normalize((List<Map<String, Object>>) list));
            }
            response.setLastSyncTime(snapshot.getSourceSyncTime());
            return response;
        } catch (Exception ex) {
            throw new BusinessException("基本信息快照解析失败: " + ex.getMessage());
        }
    }

    /**
     * 解析标录快照为页面对象。
     */
    @SuppressWarnings("unchecked")
    private TenderDocumentBidRecordPageResponse toBidRecordPageResponse(TenderDocument tenderDocument, TenderDocumentSnapshot snapshot) {
        TenderDocumentBidRecordPageResponse response = new TenderDocumentBidRecordPageResponse();
        if (snapshot == null || snapshot.getSnapshotJson() == null) {
            response.setLastSyncTime(tenderDocument.getLatestBidRecordSyncTime());
            return response;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(snapshot.getSnapshotJson(), Map.class);
            Object projectInfo = payload.get("projectInfo");
            Object tenderList = payload.get("tenderList");
            Object topLevelBidRecord = payload.get("bidRecord");
            if (projectInfo instanceof Map<?, ?> map) {
                response.setProjectInfo((Map<String, Object>) map);
            }
            if (tenderList instanceof List<?> list) {
                List<Map<String, Object>> mergedTenderList = new ArrayList<>();
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> rawTender)) {
                        continue;
                    }
                    Map<String, Object> tenderItem = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : rawTender.entrySet()) {
                        if (entry.getKey() != null) {
                            tenderItem.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                    if (!tenderItem.containsKey("bidRecord")) {
                        tenderItem.put("bidRecord", topLevelBidRecord);
                    }
                    mergedTenderList.add(tenderItem);
                }
                response.setTenderList(TenderListDisplaySupport.normalize(mergedTenderList));
            }
            response.setLastSyncTime(snapshot.getSourceSyncTime());
            return response;
        } catch (Exception ex) {
            throw new BusinessException("标录快照解析失败: " + ex.getMessage());
        }
    }

    private void updateDocumentFromBasicInfoSync(TenderDocument tenderDocument, TenderDocumentBasicInfoSyncResult payload) {
        if (payload == null) {
            return;
        }
        try {
            tenderDocument.setCompileScope(TenderDocumentCompileScopeResolver.resolve(payload.getProjectType()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
        tenderDocument.setProjectCode(payload.getProjectCode());
        tenderDocument.setProjectName(payload.getProjectName());
        tenderDocument.setPurchaseMethod(payload.getPurchaseMethod());
        tenderDocument.setEvalMethod(payload.getEvalMethod() == null ? null : payload.getEvalMethod().name());
        tenderDocument.setIndexOf(resolveTenderIndexOf(payload, tenderDocument.getTenderId()));
    }

    private Integer resolveTenderIndexOf(TenderDocumentBasicInfoSyncResult payload, String tenderId) {
        if (payload == null || tenderId == null || payload.getTenderList() == null) {
            return null;
        }
        for (Map<String, Object> tender : payload.getTenderList()) {
            if (Objects.equals(tenderId, tender.get("tenderId"))) {
                return toInteger(tender.get("indexOf"));
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

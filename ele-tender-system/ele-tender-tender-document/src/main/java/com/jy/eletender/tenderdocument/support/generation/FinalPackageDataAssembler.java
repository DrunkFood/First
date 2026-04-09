package com.jy.eletender.tenderdocument.support.generation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentSnapshot;
import com.jy.eletender.tenderdocument.entity.TenderRuleHeader;
import com.jy.eletender.tenderdocument.entity.TenderRuleNode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentRuleCategory;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.mapper.TenderDocumentSnapshotMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleHeaderMapper;
import com.jy.eletender.tenderdocument.mapper.TenderRuleNodeMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageRulePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageTenderPayload;
import com.jy.eletender.tenderdocument.support.TenderDocumentRuleTreeAssembler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FinalPackageDataAssembler {

    private final TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper;
    private final TenderRuleHeaderMapper tenderRuleHeaderMapper;
    private final TenderRuleNodeMapper tenderRuleNodeMapper;
    private final TenderDocumentRuleTreeAssembler tenderDocumentRuleTreeAssembler;
    private final ObjectMapper objectMapper;

    public FinalPackageDataAssembler(TenderDocumentSnapshotMapper tenderDocumentSnapshotMapper,
                                     TenderRuleHeaderMapper tenderRuleHeaderMapper,
                                     TenderRuleNodeMapper tenderRuleNodeMapper,
                                     TenderDocumentRuleTreeAssembler tenderDocumentRuleTreeAssembler,
                                     ObjectMapper objectMapper) {
        this.tenderDocumentSnapshotMapper = tenderDocumentSnapshotMapper;
        this.tenderRuleHeaderMapper = tenderRuleHeaderMapper;
        this.tenderRuleNodeMapper = tenderRuleNodeMapper;
        this.tenderDocumentRuleTreeAssembler = tenderDocumentRuleTreeAssembler;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> readBasicInfoProjectInfo(Long tenderDocumentId) {
        Map<String, Object> basicInfoPayload = readSnapshotPayload(tenderDocumentId, TenderDocumentStepCode.BASIC_INFO);
        return asMap(basicInfoPayload.get("projectInfo"));
    }

    public FinalPackagePayload assemble(TenderDocument tenderDocument) {
        // FINAL_PACKAGE_FILE 明文来源于各步骤快照，而不是页面临时态，确保可追溯与可复现。
        Map<String, Object> basicInfoPayload = readSnapshotPayload(tenderDocument.getId(), TenderDocumentStepCode.BASIC_INFO);
        Map<String, Object> bidRecordPayload = readSnapshotPayload(tenderDocument.getId(), TenderDocumentStepCode.BID_RECORD);
        List<Map<String, Object>> tenderItems = resolveScopedTenderItems(tenderDocument, basicInfoPayload);

        FinalPackagePayload payload = new FinalPackagePayload();
        Map<String, Object> projectInfo = asMap(basicInfoPayload.get("projectInfo"));
        payload.setBaseInfo(buildBaseInfo(tenderDocument, tenderItems, projectInfo));
        payload.setTenders(buildTenders(tenderDocument, tenderItems, bidRecordPayload));
        return payload;
    }

    private FinalPackagePayload.BaseInfo buildBaseInfo(TenderDocument tenderDocument, List<Map<String, Object>> tenderItems, Map<String, Object> projectInfo) {
        FinalPackagePayload.BaseInfo baseInfo = new FinalPackagePayload.BaseInfo();
        baseInfo.setProjectId(tenderDocument.getProjectId());
        baseInfo.setProjectName(tenderDocument.getProjectName());
        baseInfo.setProjectNo(tenderDocument.getProjectCode());
        baseInfo.setPurchaserName(asString(projectInfo.get("purchaserName")));
        baseInfo.setPurchaseMethod(asString(projectInfo.get("purchaseMethod")));
        baseInfo.setBidEndTime(asString(projectInfo.get("bidEndTime")));
        baseInfo.setTendersInfo(tenderItems.stream().map(this::toTenderInfo).toList());
        return baseInfo;
    }

    private List<FinalPackageTenderPayload> buildTenders(TenderDocument tenderDocument,
                                                         List<Map<String, Object>> tenderItems,
                                                         Map<String, Object> bidRecordPayload) {
        List<TenderRuleHeader> headers = tenderRuleHeaderMapper.selectList(Wrappers.<TenderRuleHeader>lambdaQuery()
                .eq(TenderRuleHeader::getTenderDocumentId, tenderDocument.getId())
                .orderByAsc(TenderRuleHeader::getTenderId, TenderRuleHeader::getId));
        Map<String, List<TenderRuleHeader>> headersByTenderId = headers.stream()
                .filter(header -> StringUtils.hasText(header.getTenderId()))
                .collect(Collectors.groupingBy(TenderRuleHeader::getTenderId, LinkedHashMap::new, Collectors.toList()));

        Map<String, Object> topLevelBidRecord = asMap(bidRecordPayload.get("bidRecord"));
        List<Map<String, Object>> bidRecordTenderList = asListOfMap(bidRecordPayload.get("tenderList"));
        Map<String, Map<String, Object>> bidRecordByTenderId = bidRecordTenderList.stream()
                .filter(item -> StringUtils.hasText(asString(item.get("tenderId"))))
                .collect(Collectors.toMap(
                        item -> asString(item.get("tenderId")),
                        item -> item.containsKey("bidRecord") ? asMap(item.get("bidRecord")) : topLevelBidRecord,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<FinalPackageTenderPayload> tenders = new ArrayList<>();
        for (Map<String, Object> tenderItem : tenderItems) {
            String tenderId = asString(tenderItem.get("tenderId"));
            FinalPackageTenderPayload tenderPayload = new FinalPackageTenderPayload();
            tenderPayload.setTenderId(tenderId);
            tenderPayload.setTenderName(firstNonBlank(asString(tenderItem.get("tenderName")), tenderId));
            tenderPayload.setTenderNo(asString(tenderItem.get("tenderNo")));
            tenderPayload.setBidForms(bidRecordByTenderId.getOrDefault(tenderId, topLevelBidRecord));
            tenderPayload.setBidEvalRules(buildBidEvalRules(
                    firstNonBlank(tenderDocument.getEvalMethod(), headersByTenderId.getOrDefault(tenderId, List.of()).stream()
                            .map(TenderRuleHeader::getEvalMethod)
                            .filter(StringUtils::hasText)
                            .findFirst()
                            .orElse(null)),
                    headersByTenderId.getOrDefault(tenderId, List.of())
            ));
            tenders.add(tenderPayload);
        }
        return tenders;
    }

    private FinalPackageRulePayload buildBidEvalRules(String evalMethod, List<TenderRuleHeader> headers) {
        FinalPackageRulePayload rules = new FinalPackageRulePayload();
        FinalPackageRulePayload.Info info = new FinalPackageRulePayload.Info();
        info.setBidEvalMethod(evalMethod);
        info.setReviewMode(null);
        rules.setInfo(info);

        for (TenderRuleHeader header : headers) {
            List<TenderRuleNode> nodes = tenderRuleNodeMapper.selectList(Wrappers.<TenderRuleNode>lambdaQuery()
                    .eq(TenderRuleNode::getRuleHeaderId, header.getId())
                    .orderByAsc(TenderRuleNode::getLevel, TenderRuleNode::getSortNo, TenderRuleNode::getId));
            List<FinalPackageRulePayload.RuleNode> ruleNodes = toRuleNodes(tenderDocumentRuleTreeAssembler.buildTree(nodes));
            // 对外协议字段保持固定枚举分类，便于业务系统按分类稳定解析。
            TenderDocumentRuleCategory category = TenderDocumentRuleCategory.valueOf(header.getNodeCategory());
            switch (category) {
                case QUALIFICATION -> rules.setQualification(toRuleSection(header, ruleNodes));
                case CONFORMITY -> rules.setConformity(toRuleSection(header, ruleNodes));
                case DETAIL -> rules.setDetail(toRuleSection(header, ruleNodes));
                case CREDIT -> rules.setCredit(toScoreRuleSection(header, ruleNodes));
                case TECHNICAL -> rules.setTechnical(toScoreRuleSection(header, ruleNodes));
                case BUSINESS -> rules.setBusiness(toScoreRuleSection(header, ruleNodes));
            }
        }
        return rules;
    }

    private FinalPackageRulePayload.RuleSection toRuleSection(TenderRuleHeader header, List<FinalPackageRulePayload.RuleNode> nodes) {
        FinalPackageRulePayload.RuleSection section = new FinalPackageRulePayload.RuleSection();
        section.setReviewMode(header.getReviewMode());
        section.setScoreRules(nodes);
        return section;
    }

    private FinalPackageRulePayload.ScoreRuleSection toScoreRuleSection(TenderRuleHeader header, List<FinalPackageRulePayload.RuleNode> nodes) {
        FinalPackageRulePayload.ScoreRuleSection section = new FinalPackageRulePayload.ScoreRuleSection();
        section.setReviewMode(header.getReviewMode());
        section.setTotalScore(header.getTotalScore() == null ? null : header.getTotalScore().intValue());
        section.setPercentage(header.getWeightRate() == null ? null : header.getWeightRate().intValue());
        section.setScoreRules(nodes);
        return section;
    }

    private List<FinalPackageRulePayload.RuleNode> toRuleNodes(List<TenderDocumentRuleTreeNode> treeNodes) {
        return treeNodes.stream()
                .sorted(Comparator.comparing(TenderDocumentRuleTreeNode::getOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toRuleNode)
                .toList();
    }

    private FinalPackageRulePayload.RuleNode toRuleNode(TenderDocumentRuleTreeNode treeNode) {
        FinalPackageRulePayload.RuleNode ruleNode = new FinalPackageRulePayload.RuleNode();
        ruleNode.setId(treeNode.getId());
        ruleNode.setOrder(treeNode.getOrder());
        ruleNode.setKey(treeNode.getKey());
        ruleNode.setName(treeNode.getName());
        ruleNode.setScore(treeNode.getScore());
        ruleNode.setLowest(treeNode.getLowest());
        ruleNode.setHighest(treeNode.getHighest());
        ruleNode.setStandard(treeNode.getStandard());
        ruleNode.setObjectiveType(treeNode.getObjectiveType());
        ruleNode.setChildren(toRuleNodes(treeNode.getChildren()));
        ruleNode.setIsPass(treeNode.getPass());
        ruleNode.setIsParent(treeNode.getParent());
        return ruleNode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSnapshotPayload(Long tenderDocumentId, TenderDocumentStepCode stepCode) {
        TenderDocumentSnapshot snapshot = tenderDocumentSnapshotMapper.selectOne(Wrappers.<TenderDocumentSnapshot>lambdaQuery()
                .eq(TenderDocumentSnapshot::getTenderDocumentId, tenderDocumentId)
                .eq(TenderDocumentSnapshot::getStepCode, stepCode.name())
                .last("limit 1"));
        if (snapshot == null || !StringUtils.hasText(snapshot.getSnapshotJson())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshot.getSnapshotJson(), Map.class);
        } catch (Exception ex) {
            throw new BusinessException(stepCode.name() + "快照解析失败: " + ex.getMessage());
        }
    }

    private List<Map<String, Object>> resolveScopedTenderItems(TenderDocument tenderDocument, Map<String, Object> basicInfoPayload) {
        List<Map<String, Object>> tenderList = asListOfMap(basicInfoPayload.get("tenderList"));
        if (tenderList.isEmpty()) {
            // 快照缺失标段列表时回退到编制单自身标段，保证最小可生成。
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("tenderId", tenderDocument.getTenderId());
            fallback.put("tenderName", tenderDocument.getTenderId());
            fallback.put("tenderNo", null);
            tenderList = List.of(fallback);
        }
        if (!TenderDocumentScopeType.TENDER.name().equalsIgnoreCase(tenderDocument.getCompileScope())) {
            return tenderList;
        }
        // 标段级编制单只输出当前标段，项目级编制单输出全标段列表。
        return tenderList.stream()
                .filter(item -> Objects.equals(asString(item.get("tenderId")), tenderDocument.getTenderId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        converted.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                result.add(converted);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return converted;
    }

    private FinalPackagePayload.TenderInfo toTenderInfo(Map<String, Object> tenderItem) {
        FinalPackagePayload.TenderInfo tenderInfo = new FinalPackagePayload.TenderInfo();
        tenderInfo.setTenderId(asString(tenderItem.get("tenderId")));
        tenderInfo.setTenderName(firstNonBlank(asString(tenderItem.get("tenderName")), asString(tenderItem.get("tenderId"))));
        tenderInfo.setTenderNo(asString(tenderItem.get("tenderNo")));
        tenderInfo.setTenderAmount(toBigDecimal(tenderItem.get("tenderAmount")));
        tenderInfo.setPurchaseContext(asString(tenderItem.get("purchaseContext")));
        return tenderInfo;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.math.BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new java.math.BigDecimal(n.toString());
        }
        try {
            return new java.math.BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}

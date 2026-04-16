package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jy.eleaitender.common.entity.support.AiModelConfig;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.util.AesUtil;
import com.jy.eleaitender.common.util.RsaKeyUtil;
import com.jy.eleaitender.support.dto.ModelConfigCreateDTO;
import com.jy.eleaitender.support.dto.ModelConfigUpdateDTO;
import com.jy.eleaitender.support.mapper.ModelConfigMapper;
import com.jy.eleaitender.support.service.IModelConfigService;
import com.jy.eleaitender.support.vo.ModelConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
@Slf4j
public class ModelConfigServiceImpl implements IModelConfigService {

    private static final String CONFIG_REFRESH_CHANNEL = "ai:config:refresh";

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Page<ModelConfigVO> getPage(Integer pageNum, Integer pageSize, String modelType, String provider, String usageScenario, String modelName, Integer isActive) {
        Page<AiModelConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(modelType)) {
            wrapper.eq(AiModelConfig::getModelType, modelType);
        }
        if (StringUtils.hasText(provider)) {
            wrapper.eq(AiModelConfig::getProvider, provider);
        }
        if (StringUtils.hasText(usageScenario)) {
            wrapper.eq(AiModelConfig::getUsageScenario, usageScenario);
        }
        if (StringUtils.hasText(modelName)) {
            wrapper.like(AiModelConfig::getModelName, modelName);
        }
        if (isActive != null) {
            wrapper.eq(AiModelConfig::getIsActive, isActive);
        }
        wrapper.eq(AiModelConfig::getIsDelete, 0);
        wrapper.orderByDesc(AiModelConfig::getCreateTime);

        Page<AiModelConfig> entityPage = modelConfigMapper.selectPage(page, wrapper);

        // 转换为 VO 分页
        Page<ModelConfigVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<ModelConfigVO> getActiveList() {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelConfig::getIsActive, 1);
        wrapper.eq(AiModelConfig::getIsDelete, 0);
        wrapper.orderByAsc(AiModelConfig::getModelName);
        return modelConfigMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ModelConfigVO getDetailById(Long id) {
        AiModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResponseCode.MODEL_CONFIG_NOT_FOUND);
        }
        return toVO(config);
    }

    @Override
    @Transactional
    public ModelConfigVO create(ModelConfigCreateDTO dto) {
        AiModelConfig config = toEntity(dto);
        if (config.getIsActive() == null) {
            config.setIsActive(1);
        }
        if (config.getTokenUsage() == null) {
            config.setTokenUsage(0L);
        }
        // AES加密apiKey
        encryptApiKey(config, dto.getApiKey(), dto.getKeyId());

        modelConfigMapper.insert(config);
        publishConfigRefresh();
        return toVO(config);
    }

    @Override
    @Transactional
    public void update(Long id, ModelConfigUpdateDTO dto) {
        AiModelConfig existing = modelConfigMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResponseCode.MODEL_CONFIG_NOT_FOUND);
        }

        // 合并DTO字段到实体
        mergeDtoToEntity(dto, existing);

        // 如果传入了新的apiKey，RSA解密后AES加密存储
        if (StringUtils.hasText(dto.getApiKey())) {
            encryptApiKey(existing, dto.getApiKey(), dto.getKeyId());
        }
        // 未传apiKey则保留原值，不做修改

        modelConfigMapper.updateById(existing);
        publishConfigRefresh();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
        publishConfigRefresh();
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        modelConfigMapper.deleteBatchIds(ids);
        publishConfigRefresh();
    }

    @Override
    @Transactional
    public void setActive(Long id, Integer isActive) {
        AiModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResponseCode.MODEL_CONFIG_NOT_FOUND);
        }
        config.setIsActive(isActive);
        modelConfigMapper.updateById(config);
        publishConfigRefresh();
    }

    // ==================== 转换方法 ====================

    /**
     * Entity → VO（apiKey脱敏，字段名映射）
     */
    private ModelConfigVO toVO(AiModelConfig entity) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setId(entity.getId());
        vo.setModelName(entity.getModelName());
        vo.setModelType(entity.getModelType());
        vo.setProvider(entity.getProvider());
        vo.setEndpoint(entity.getApiEndpoint());

        // apiKey脱敏：先AES解密再mask
        if (StringUtils.hasText(entity.getApiKey())) {
            try {
                String plainApiKey = AesUtil.decrypt(entity.getApiKey());
                vo.setApiKey(AesUtil.mask(plainApiKey));
            } catch (Exception e) {
                // 解密失败（如历史明文数据），直接脱敏
                vo.setApiKey(AesUtil.mask(entity.getApiKey()));
            }
        }

        vo.setUsageScenario(entity.getUsageScenario());
        vo.setIsActive(entity.getIsActive());
        vo.setTokenUsage(entity.getTokenUsage());
        vo.setCost(entity.getCost());
        vo.setRemark(entity.getRemark());

        // 从 modelParams JSON 中提取扁平字段
        parseModelParamsToVO(entity.getModelParams(), vo);

        // 时间格式化
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(sdf.format(entity.getCreateTime()));
        }
        if (entity.getModifyTime() != null) {
            vo.setModifyTime(sdf.format(entity.getModifyTime()));
        }
        vo.setCreateName(entity.getCreateName());

        return vo;
    }

    /**
     * CreateDTO → Entity（组装modelParams JSON）
     */
    private AiModelConfig toEntity(ModelConfigCreateDTO dto) {
        AiModelConfig config = new AiModelConfig();
        config.setModelName(dto.getModelName());
        config.setModelType(dto.getModelType());
        config.setProvider(StringUtils.hasText(dto.getProvider()) ? dto.getProvider() : "OPENAI");
        config.setApiEndpoint(dto.getEndpoint());
        config.setUsageScenario(dto.getUsageScenario());
        config.setIsActive(1);
        config.setTokenUsage(0L);
        config.setCost(dto.getCost() != null ? dto.getCost() : java.math.BigDecimal.ZERO);
        config.setRemark(dto.getRemark());

        // 组装 modelParams JSON
        config.setModelParams(buildModelParamsJson(dto.getModelCode(), dto.getMaxTokens(),
                dto.getTemperature(), dto.getTopP(), dto.getTimeout(), dto.getTokenLimit(),
                dto.getParameters()));

        return config;
    }

    /**
     * UpdateDTO → Entity 合并（仅更新非空字段）
     */
    private void mergeDtoToEntity(ModelConfigUpdateDTO dto, AiModelConfig entity) {
        if (dto.getModelName() != null) entity.setModelName(dto.getModelName());
        if (dto.getModelType() != null) entity.setModelType(dto.getModelType());
        if (dto.getProvider() != null) entity.setProvider(dto.getProvider());
        if (dto.getEndpoint() != null) entity.setApiEndpoint(dto.getEndpoint());
        if (dto.getUsageScenario() != null) entity.setUsageScenario(dto.getUsageScenario());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());
        if (dto.getCost() != null) entity.setCost(dto.getCost());

        // 如果有任意一个modelParams相关字段被传入，重新组装modelParams
        if (dto.getModelCode() != null || dto.getMaxTokens() != null || dto.getTemperature() != null
                || dto.getTopP() != null || dto.getTimeout() != null || dto.getTokenLimit() != null
                || dto.getParameters() != null) {
            // 先从现有modelParams中读取当前值作为默认值
            ModelParams current = parseModelParams(entity.getModelParams());

            String modelCode = dto.getModelCode() != null ? dto.getModelCode() : current.model;
            Integer maxTokens = dto.getMaxTokens() != null ? dto.getMaxTokens() : current.maxTokens;
            Double temperature = dto.getTemperature() != null ? dto.getTemperature() : current.temperature;
            Double topP = dto.getTopP() != null ? dto.getTopP() : current.topP;
            Integer timeout = dto.getTimeout() != null ? dto.getTimeout() : current.timeout;
            Long tokenLimit = dto.getTokenLimit() != null ? dto.getTokenLimit() : current.tokenLimit;

            entity.setModelParams(buildModelParamsJson(modelCode, maxTokens, temperature, topP,
                    timeout, tokenLimit, dto.getParameters()));
        }
    }

    // ==================== modelParams JSON 处理 ====================

    /**
     * 内部模型参数结构
     */
    private static class ModelParams {
        String model;
        Integer maxTokens;
        Double temperature;
        Double topP;
        Integer timeout;
        Long tokenLimit;
    }

    /**
     * 解析 modelParams JSON
     */
    private ModelParams parseModelParams(String modelParamsJson) {
        ModelParams params = new ModelParams();
        if (!StringUtils.hasText(modelParamsJson)) {
            return params;
        }
        try {
            JsonNode node = objectMapper.readTree(modelParamsJson);
            if (node.has("model")) params.model = node.get("model").asText();
            if (node.has("maxTokens")) params.maxTokens = node.get("maxTokens").intValue();
            if (node.has("temperature")) params.temperature = node.get("temperature").doubleValue();
            if (node.has("topP")) params.topP = node.get("topP").doubleValue();
            if (node.has("timeout")) params.timeout = node.get("timeout").intValue();
            if (node.has("tokenLimit")) params.tokenLimit = node.get("tokenLimit").longValue();
        } catch (Exception e) {
            log.warn("解析modelParams失败: {}", modelParamsJson, e);
        }
        return params;
    }

    /**
     * 从 modelParams JSON 提取扁平字段到 VO
     */
    private void parseModelParamsToVO(String modelParamsJson, ModelConfigVO vo) {
        if (!StringUtils.hasText(modelParamsJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(modelParamsJson);
            if (node.has("model")) vo.setModelCode(node.get("model").asText());
            if (node.has("maxTokens")) vo.setMaxTokens(node.get("maxTokens").intValue());
            if (node.has("temperature")) vo.setTemperature(node.get("temperature").doubleValue());
            if (node.has("topP")) vo.setTopP(node.get("topP").doubleValue());
            if (node.has("timeout")) vo.setTimeout(node.get("timeout").intValue());
            if (node.has("tokenLimit")) vo.setTokenLimit(node.get("tokenLimit").longValue());
        } catch (Exception e) {
            log.warn("解析modelParams到VO失败: {}", modelParamsJson, e);
        }
    }

    /**
     * 组装 modelParams JSON
     */
    private String buildModelParamsJson(String modelCode, Integer maxTokens, Double temperature,
                                        Double topP, Integer timeout, Long tokenLimit,
                                        String extraParameters) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            if (modelCode != null) root.put("model", modelCode);
            if (maxTokens != null) root.put("maxTokens", maxTokens);
            if (temperature != null) root.put("temperature", temperature);
            if (topP != null) root.put("topP", topP);
            if (timeout != null) root.put("timeout", timeout);
            if (tokenLimit != null) root.put("tokenLimit", tokenLimit);

            // 合并高级参数
            if (StringUtils.hasText(extraParameters)) {
                JsonNode extra = objectMapper.readTree(extraParameters);
                extra.fields().forEachRemaining(entry -> {
                    if (!root.has(entry.getKey())) {
                        root.set(entry.getKey(), entry.getValue());
                    }
                });
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("组装modelParams JSON失败", e);
            throw new BusinessException(ResponseCode.MODEL_CONFIG_ENCRYPT_ERROR);
        }
    }

    // ==================== 加密相关 ====================

    /**
     * RSA解密前端传来的apiKey，然后AES加密存储
     */
    private void encryptApiKey(AiModelConfig config, String encryptedApiKey, String keyId) {
        if (!StringUtils.hasText(encryptedApiKey)) {
            return;
        }
        try {
            // 1. RSA解密前端传输的密文
            String plainApiKey;
            if (StringUtils.hasText(keyId)) {
                plainApiKey = RsaKeyUtil.decryptPassword(keyId, encryptedApiKey);
            } else {
                // 兼容未加密的场景（如API测试工具直接调用）
                log.warn("API密钥未经过RSA加密传输，建议前端使用RSA加密");
                plainApiKey = encryptedApiKey;
            }
            // 2. AES加密后存储
            config.setApiKey(AesUtil.encrypt(plainApiKey));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("API密钥加密失败", e);
            throw new BusinessException(ResponseCode.MODEL_CONFIG_ENCRYPT_ERROR);
        }
    }

    /**
     * 发布模型配置刷新通知到Redis Pub/Sub
     */
    private void publishConfigRefresh() {
        try {
            redisTemplate.convertAndSend(CONFIG_REFRESH_CHANNEL, "model_config_updated");
        } catch (Exception e) {
            // 通知失败不影响主流程，缓存会自然过期
        }
    }
}

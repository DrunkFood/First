package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import com.jy.eletender.tenderdocument.mapper.ExternalSystemAccessMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 外部系统访问配置解析器。
 * 根据 appKey 读取并校验业务系统接入配置，输出可直接用于远程调用的标准化对象。
 */
@Component
public class ExternalSystemAccessResolver {

    private final ExternalSystemAccessMapper externalSystemAccessMapper;

    public ExternalSystemAccessResolver(ExternalSystemAccessMapper externalSystemAccessMapper) {
        this.externalSystemAccessMapper = externalSystemAccessMapper;
    }

    /**
     * 解析业务系统访问配置并执行基础可用性校验。
     */
    public ResolvedExternalSystem resolve(String appKey) {
        if (StringUtils.isBlank(appKey)) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_CONFIG_INVALID.getCode(), "当前外部用户缺少 appKey");
        }
        SysAccessSystem system = externalSystemAccessMapper.selectByAppKey(appKey);
        if (system == null) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_CONFIG_INVALID.getCode(), "未找到对应业务系统配置: " + appKey);
        }
        if (system.getStatus() == null || system.getStatus() != 1) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_CONFIG_INVALID.getCode(), "业务系统配置已禁用: " + appKey);
        }
        if (system.getExpireTime() != null && system.getExpireTime().before(new Date())) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_CONFIG_INVALID.getCode(), "业务系统配置已过期: " + appKey);
        }
        if (StringUtils.isAnyBlank(system.getSystemUrl(), system.getAppSecret())) {
            throw new BusinessException(TenderDocumentErrorCode.BUSINESS_SYSTEM_CONFIG_INVALID.getCode(), "业务系统配置不完整: " + appKey);
        }
        // 统一去除末尾斜杠，避免拼接 path 时出现双斜杠。
        return new ResolvedExternalSystem(system.getAppKey(), system.getAppSecret(), trimTrailingSlash(system.getSystemUrl()));
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

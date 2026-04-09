package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.support.interaction.TenderDocumentInteractionProperties;
import org.springframework.stereotype.Component;

@Component
public class DefaultFinalPackageEncryptor implements FinalPackageEncryptor {

    private final TenderDocumentInteractionProperties properties;

    public DefaultFinalPackageEncryptor(TenderDocumentInteractionProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] encrypt(byte[] plainContent) {
        if (plainContent == null || plainContent.length == 0) {
            throw new BusinessException("最终数据包内容为空，不能加密");
        }
        try {
            // 与 CLI 共用同一套 key 校验和二进制封装格式，避免线上/线下行为不一致。
            byte[] aesKey = FinalPackageCryptoUtil.decodeBase64Key(properties.getFinalPackageAesKeyBase64());
            return FinalPackageCryptoUtil.encrypt(plainContent, properties.getFinalPackageEncryptAlgorithm(), aesKey);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("最终数据包AES配置非法: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException("最终数据包AES加密失败: " + ex.getMessage());
        }
    }
}

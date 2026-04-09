package com.jy.eletender.crypto.service.impl;

import com.jy.eletender.common.constant.FileConstants;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.SysAccessSystemReadMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.hutool.crypto.digest.DigestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 解密成功后将加密文件和解密文件复制到按项目/标段组织的目录中。
 * 组织路径格式：{organizedFilePath}/{appKey}/{projectId}/{tenderId}/encrypted|decrypted/{bidRecordId}.ext
 * 该服务是原始存储（SHA256 内容寻址）的辅助视图，复制失败不阻塞主流程。
 */
@Service
public class OrganizedFileCopyService {

    private static final Logger log = LoggerFactory.getLogger(OrganizedFileCopyService.class);

    private final CryptoProperties cryptoProperties;
    private final SysAccessSystemReadMapper sysAccessSystemReadMapper;

    public OrganizedFileCopyService(CryptoProperties cryptoProperties,
                                    SysAccessSystemReadMapper sysAccessSystemReadMapper) {
        this.cryptoProperties = cryptoProperties;
        this.sysAccessSystemReadMapper = sysAccessSystemReadMapper;
    }

    /**
     * 将加密投标文件复制到按项目/标段组织的目录。
     *
     * @param request    解密请求（提供 appKey/projectId/tenderId/bidRecordId）
     * @param sourcePath 加密文件原始路径
     * @return 目标路径字符串，复制失败时返回 null
     */
    public String copyEncryptedFile(BdcDecryptRequest request, String sourcePath) {
        if (!validateRequest(request) || StringUtils.isBlank(sourcePath)) {
            return null;
        }
        try {
            String suffix = resolveBidDocumentSuffix(request.getAppKey());
            Path dest = buildOrganizedDir(request, "encrypted")
                    .resolve(request.getBidRecordId() + suffix);
            return doCopy(Path.of(sourcePath), dest);
        } catch (Exception ex) {
            log.warn("按项目/标段组织加密文件失败 recordId={} source={}: {}",
                    request.getId(), sourcePath, ex.getMessage());
            return null;
        }
    }

    /**
     * 将解密后的文件复制到按项目/标段组织的目录。
     *
     * @param request    解密请求（提供 appKey/projectId/tenderId/bidRecordId）
     * @param sourcePath 解密文件原始路径
     * @return 目标路径字符串，复制失败时返回 null
     */
    public String copyDecryptedFile(BdcDecryptRequest request, String sourcePath) {
        if (!validateRequest(request) || StringUtils.isBlank(sourcePath)) {
            return null;
        }
        try {
            Path dest = buildOrganizedDir(request, "decrypted")
                    .resolve(request.getBidRecordId() + ".json");
            return doCopy(Path.of(sourcePath), dest);
        } catch (Exception ex) {
            log.warn("按项目/标段组织解密文件失败 recordId={} source={}: {}",
                    request.getId(), sourcePath, ex.getMessage());
            return null;
        }
    }

    private Path buildOrganizedDir(BdcDecryptRequest request, String subDir) {
        return Path.of(
                cryptoProperties.getOrganizedFilePath(),
                request.getAppKey(),
                request.getProjectId(),
                request.getTenderId(),
                subDir
        );
    }

    /**
     * 执行文件复制。目标文件已存在且 SHA256 一致时跳过（幂等）。
     */
    private String doCopy(Path source, Path dest) throws IOException {
        if (!Files.exists(source)) {
            log.warn("源文件不存在，跳过组织复制 source={}", source);
            return null;
        }
        if (Files.exists(dest)) {
            String srcHash = DigestUtil.sha256Hex(source.toFile());
            String destHash = DigestUtil.sha256Hex(dest.toFile());
            if (srcHash.equalsIgnoreCase(destHash)) {
                return dest.toString();
            }
        }
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }

    /**
     * 校验请求字段不含路径穿越字符，防止目录遍历攻击。
     */
    private boolean validateRequest(BdcDecryptRequest request) {
        if (request == null) {
            return false;
        }
        return isSafePathSegment(request.getAppKey())
                && isSafePathSegment(request.getProjectId())
                && isSafePathSegment(request.getTenderId())
                && isSafePathSegment(request.getBidRecordId());
    }

    private boolean isSafePathSegment(String segment) {
        if (StringUtils.isBlank(segment)) {
            return false;
        }
        return !segment.contains("..") && !segment.contains("/") && !segment.contains("\\");
    }

    private String resolveBidDocumentSuffix(String appKey) {
        if (StringUtils.isBlank(appKey)) {
            return FileConstants.DEFAULT_BID_DOCUMENT_SUFFIX;
        }
        SysAccessSystem system = sysAccessSystemReadMapper.selectByAppKey(appKey);
        if (system != null && StringUtils.isNotBlank(system.getBidDocumentSuffix())) {
            return system.getBidDocumentSuffix();
        }
        return FileConstants.DEFAULT_BID_DOCUMENT_SUFFIX;
    }
}

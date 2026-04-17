package com.jy.eleaitender.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.FileException;
import com.jy.eleaitender.file.config.AllowedTypesResolver;
import com.jy.eleaitender.file.config.FileStorageConfig;
import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.entity.file.FileInfo;
import com.jy.eleaitender.file.mapper.FileInfoMapper;
import com.jy.eleaitender.file.service.IFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements IFileStorageService {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private AllowedTypesResolver allowedTypesResolver;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse upload(MultipartFile file, String bizType) {
        // 校验文件
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        try (InputStream inputStream = file.getInputStream()) {
            // 计算文件 SHA-256
            String fileSha256 = DigestUtil.sha256Hex(inputStream);

            // 检查是否存在相同 SHA-256 的文件（秒传）
            FileInfo existingFile = getBySha256(fileSha256);
            if (existingFile != null) {
                log.info("文件秒传成功, sha256: {}, fileId: {}", fileSha256, existingFile.getId());
                return new FileUploadResponse(existingFile.getId(), originalFilename,
                        existingFile.getFileSize(), fileSha256, extractFileType(originalFilename));
            }

            // 生成存储路径
            String relativePath = generateRelativePath(bizType, originalFilename);
            String absolutePath = fileStorageConfig.getBasePath() + File.separator + relativePath;

            // 确保目录存在
            File destFile = new File(absolutePath);
            FileUtil.mkParentDirs(destFile);

            // 保存文件
            file.transferTo(destFile);

            // 保存文件信息到数据库
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(originalFilename);
            fileInfo.setFilePath(relativePath);
            fileInfo.setFileSize(fileSize);
            fileInfo.setFileSha256(fileSha256);
            fileInfo.setBizType(bizType);
            fileInfoMapper.insert(fileInfo);

            log.info("文件上传成功, fileId: {}, fileName: {}, path: {}", 
                    fileInfo.getId(), originalFilename, relativePath);

            return new FileUploadResponse(fileInfo.getId(), originalFilename, fileSize, fileSha256, extractFileType(originalFilename));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public FileInfo getById(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return fileInfoMapper.selectById(fileId);
    }

    @Override
    public String getFilePath(Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new FileException(ResponseCode.FILE_NOT_FOUND);
        }
        return fileStorageConfig.getBasePath() + File.separator + fileInfo.getFilePath();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return false;
        }

        // 逻辑删除数据库记录
        int result = fileInfoMapper.deleteById(fileId);

        // 删除物理文件（可选，根据业务需求决定是否删除）
        // String absolutePath = fileStorageConfig.getBasePath() + File.separator + fileInfo.getFilePath();
        // FileUtil.del(absolutePath);

        return result > 0;
    }

    @Override
    public FileInfo getBySha256(String sha256) {
        if (StringUtils.isBlank(sha256)) {
            return null;
        }
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getFileSha256, sha256);
        queryWrapper.last("LIMIT 1");
        return fileInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 校验文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "文件名不能为空");
        }

        // 检查文件大小
        if (file.getSize() > fileStorageConfig.getMaxSizeBytes()) {
            throw new FileException(ResponseCode.FILE_SIZE_EXCEEDED,
                    "文件大小超出限制，最大允许" + fileStorageConfig.getMaxSize() + "MB");
        }

        // 检查文件类型（静态白名单 + 动态注册后缀）
        if (!allowedTypesResolver.isAllowed(originalFilename)) {
            throw new FileException(ResponseCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件格式");
        }
    }

    /**
     * 生成相对存储路径
     * 格式：bizType/yyyyMMdd/时间戳_原始文件名
     */
    private String generateRelativePath(String bizType, String originalFilename) {
        String dateDir = LocalDateTime.now().format(DATE_FORMATTER);
        String newFilename = System.currentTimeMillis() + "_" + originalFilename;
        return bizType + File.separator + dateDir + File.separator + newFilename;
    }

    /**
     * 从文件名提取扩展名作为文件类型
     */
    private String extractFileType(String fileName) {
        if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
    }
}

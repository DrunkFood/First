package com.jy.eletender.file.service;

import com.jy.eletender.common.dto.response.FileUploadResponse;
import com.jy.eletender.common.entity.file.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface IFileStorageService {

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @return 上传结果
     */
    FileUploadResponse upload(MultipartFile file, String bizType);

    /**
     * 根据ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileInfo getById(Long fileId);

    /**
     * 获取文件绝对路径
     *
     * @param fileId 文件ID
     * @return 文件绝对路径
     */
    String getFilePath(Long fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean delete(Long fileId);

    /**
     * 根据 SHA-256 查询文件（秒传）
     *
     * @param sha256 文件SHA-256
     * @return 文件信息
     */
    FileInfo getBySha256(String sha256);
}

package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.core.TbPolicyFile;
import com.jy.eleaitender.core.dto.response.PolicyFileVO;

import java.util.List;

/**
 * 政策文件服务接口（Core模块 - 用户政策文件）
 */
public interface IPolicyFileService extends IService<TbPolicyFile> {

    /**
     * 分页查询当前用户的政策文件
     */
    Page<PolicyFileVO> getPage(Integer pageNum, Integer pageSize, String fileName, String fileCategory, String applicableCategory);

    /**
     * 获取详情
     */
    TbPolicyFile getById(Long id);

    /**
     * 上传政策文件
     */
    TbPolicyFile create(TbPolicyFile policyFile);

    /**
     * 删除政策文件
     */
    void deleteById(Long id);

    /**
     * 启用/禁用
     */
    void setStatus(Long id, Integer status);

    /**
     * 获取全部可用政策文件（系统级+用户级合并，供检测选择用）
     */
    List<PolicyFileVO> getAllAvailable(String applicableCategory);
}

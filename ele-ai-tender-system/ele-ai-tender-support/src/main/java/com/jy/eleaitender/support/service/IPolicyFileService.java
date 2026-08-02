package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;

/**
 * 政策文件服务接口
 */
public interface IPolicyFileService extends IService<SupPolicyFile> {

    /**
     * 分页查询政策文件
     *
     * @param pageNum            页码
     * @param pageSize           每页条数
     * @param fileCategory       文件分类（可选）
     * @param applicableCategory 适用项目类别（可选）
     * @return 分页结果
     */
    Page<SupPolicyFile> getPage(Integer pageNum, Integer pageSize,
                                String fileName, String fileCategory, String applicableCategory);

    /**
     * 获取政策文件详情
     *
     * @param id 主键ID
     * @return 政策文件
     */
    SupPolicyFile getById(Long id);

    /**
     * 创建政策文件记录
     *
     * @param policyFile 政策文件
     * @return 创建后的实体
     */
    SupPolicyFile create(SupPolicyFile policyFile);

    /**
     * 更新政策文件记录
     *
     * @param policyFile 政策文件
     */
    void update(SupPolicyFile policyFile);

    /**
     * 删除政策文件（逻辑删除）
     *
     * @param id 主键ID
     */
    void deleteById(Long id);

    /**
     * 启用/禁用政策文件
     *
     * @param id     主键ID
     * @param status 状态: 0-禁用, 1-启用
     */
    void setStatus(Long id, Integer status);
}

package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.PolicyFileMapper;
import com.jy.eleaitender.support.service.IPolicyFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 政策文件服务实现
 */
@Service
public class PolicyFileServiceImpl implements IPolicyFileService {

    @Autowired
    private PolicyFileMapper policyFileMapper;

    @Override
    public Page<SupPolicyFile> getPage(Integer pageNum, Integer pageSize,
                                       String fileName, String fileCategory, String applicableCategory) {
        Page<SupPolicyFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SupPolicyFile> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(fileName)) {
            wrapper.like(SupPolicyFile::getFileName, fileName);
        }
        if (StringUtils.hasText(fileCategory)) {
            wrapper.eq(SupPolicyFile::getFileCategory, fileCategory);
        }
        if (StringUtils.hasText(applicableCategory)) {
            wrapper.eq(SupPolicyFile::getApplicableCategory, applicableCategory);
        }
        wrapper.orderByDesc(SupPolicyFile::getCreateTime);

        return policyFileMapper.selectPage(page, wrapper);
    }

    @Override
    public SupPolicyFile getById(Long id) {
        SupPolicyFile policyFile = policyFileMapper.selectById(id);
        if (policyFile == null) {
            throw new BusinessException(ResponseCode.POLICY_FILE_NOT_FOUND);
        }
        return policyFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupPolicyFile create(SupPolicyFile policyFile) {
        if (policyFile.getStatus() == null) {
            policyFile.setStatus(1);
        }
        policyFileMapper.insert(policyFile);
        return policyFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SupPolicyFile policyFile) {
        policyFileMapper.updateById(policyFile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        policyFileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long id, Integer status) {
        SupPolicyFile policyFile = policyFileMapper.selectById(id);
        if (policyFile == null) {
            throw new BusinessException(ResponseCode.POLICY_FILE_NOT_FOUND);
        }
        policyFile.setStatus(status);
        policyFileMapper.updateById(policyFile);
    }
}

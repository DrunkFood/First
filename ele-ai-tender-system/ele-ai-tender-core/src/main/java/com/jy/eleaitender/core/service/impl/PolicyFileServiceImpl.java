package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.core.AiPolicyFile;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.response.PolicyFileVO;
import com.jy.eleaitender.core.mapper.AiPolicyFileMapper;
import com.jy.eleaitender.core.mapper.SupPolicyFileMapper;
import com.jy.eleaitender.core.service.IPolicyFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 政策文件服务实现
 */
@Service
public class PolicyFileServiceImpl implements IPolicyFileService {

    @Autowired
    private AiPolicyFileMapper aiPolicyFileMapper;

    @Autowired
    private SupPolicyFileMapper supPolicyFileMapper;

    @Override
    public Page<AiPolicyFile> getPage(Integer pageNum, Integer pageSize, String fileCategory, String applicableCategory) {
        Page<AiPolicyFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiPolicyFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPolicyFile::getUserId, SecurityContextHolder.getUserId());
        if (StringUtils.hasText(fileCategory)) {
            wrapper.eq(AiPolicyFile::getFileCategory, fileCategory);
        }
        if (StringUtils.hasText(applicableCategory)) {
            wrapper.eq(AiPolicyFile::getApplicableCategory, applicableCategory);
        }
        wrapper.orderByDesc(AiPolicyFile::getCreateTime);
        return aiPolicyFileMapper.selectPage(page, wrapper);
    }

    @Override
    public AiPolicyFile getById(Long id) {
        AiPolicyFile file = aiPolicyFileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResponseCode.POLICY_FILE_NOT_FOUND);
        }
        return file;
    }

    @Override
    @Transactional
    public AiPolicyFile create(AiPolicyFile policyFile) {
        policyFile.setUserId(SecurityContextHolder.getUserId());
        if (policyFile.getStatus() == null) {
            policyFile.setStatus(1);
        }
        aiPolicyFileMapper.insert(policyFile);
        return policyFile;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        AiPolicyFile file = getById(id);
        // 只能删除自己的文件
        if (!file.getUserId().equals(SecurityContextHolder.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权删除该政策文件");
        }
        aiPolicyFileMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setStatus(Long id, Integer status) {
        AiPolicyFile file = getById(id);
        file.setStatus(status);
        aiPolicyFileMapper.updateById(file);
    }

    @Override
    public List<PolicyFileVO> getAllAvailable(String applicableCategory) {
        List<PolicyFileVO> result = new ArrayList<>();

        // 1. 查询系统级政策文件（sup_policy_file）
        LambdaQueryWrapper<SupPolicyFile> sysWrapper = new LambdaQueryWrapper<>();
        sysWrapper.eq(SupPolicyFile::getStatus, 1);
        if (StringUtils.hasText(applicableCategory)) {
            sysWrapper.and(w -> w.eq(SupPolicyFile::getApplicableCategory, applicableCategory)
                    .or().isNull(SupPolicyFile::getApplicableCategory));
        }
        List<SupPolicyFile> sysFiles = supPolicyFileMapper.selectList(sysWrapper);
        for (SupPolicyFile f : sysFiles) {
            result.add(toVO(f, "SYSTEM"));
        }

        // 2. 查询用户级政策文件（ai_policy_file）
        LambdaQueryWrapper<AiPolicyFile> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(AiPolicyFile::getUserId, SecurityContextHolder.getUserId())
                .eq(AiPolicyFile::getStatus, 1);
        if (StringUtils.hasText(applicableCategory)) {
            userWrapper.and(w -> w.eq(AiPolicyFile::getApplicableCategory, applicableCategory)
                    .or().isNull(AiPolicyFile::getApplicableCategory));
        }
        List<AiPolicyFile> userFiles = aiPolicyFileMapper.selectList(userWrapper);
        for (AiPolicyFile f : userFiles) {
            result.add(toVO(f, "USER"));
        }

        return result;
    }

    private PolicyFileVO toVO(SupPolicyFile f, String source) {
        PolicyFileVO vo = new PolicyFileVO();
        vo.setId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileCategory(f.getFileCategory());
        vo.setApplicableCategory(f.getApplicableCategory());
        vo.setFileId(f.getFileId());
        vo.setFileSize(f.getFileSize());
        vo.setFileType(f.getFileType());
        vo.setDescription(f.getDescription());
        vo.setStatus(f.getStatus());
        vo.setSource(source);
        vo.setCreateTime(f.getCreateTime());
        vo.setCreateName(f.getCreateName());
        return vo;
    }

    private PolicyFileVO toVO(AiPolicyFile f, String source) {
        PolicyFileVO vo = new PolicyFileVO();
        vo.setId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileCategory(f.getFileCategory());
        vo.setApplicableCategory(f.getApplicableCategory());
        vo.setFileId(f.getFileId());
        vo.setFileSize(f.getFileSize());
        vo.setFileType(f.getFileType());
        vo.setDescription(f.getDescription());
        vo.setStatus(f.getStatus());
        vo.setSource(source);
        vo.setCreateTime(f.getCreateTime());
        vo.setCreateName(f.getCreateName());
        return vo;
    }
}

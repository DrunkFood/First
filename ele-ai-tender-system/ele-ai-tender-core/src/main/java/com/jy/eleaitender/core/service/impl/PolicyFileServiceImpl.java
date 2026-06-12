package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.entity.core.TbPolicyFile;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.response.PolicyFileVO;
import com.jy.eleaitender.core.mapper.TbPolicyFileMapper;
import com.jy.eleaitender.core.mapper.SupPolicyFileMapper;
import com.jy.eleaitender.core.service.IPolicyFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 政策文件服务实现
 */
@Service
public class PolicyFileServiceImpl implements IPolicyFileService {

    @Autowired
    private TbPolicyFileMapper aiPolicyFileMapper;

    @Autowired
    private SupPolicyFileMapper supPolicyFileMapper;

    @Override
    public Page<PolicyFileVO> getPage(Integer pageNum, Integer pageSize, String fileName, String fileCategory, String applicableCategory) {
        Page<TbPolicyFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TbPolicyFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbPolicyFile::getUserId, SecurityContextHolder.getUserId());
        if (StringUtils.hasText(fileName)) {
            wrapper.like(TbPolicyFile::getFileName, fileName);
        }
        if (StringUtils.hasText(fileCategory)) {
            wrapper.eq(TbPolicyFile::getFileCategory, fileCategory);
        }
        if (StringUtils.hasText(applicableCategory)) {
            wrapper.eq(TbPolicyFile::getApplicableCategory, applicableCategory);
        }
        wrapper.orderByDesc(TbPolicyFile::getCreateTime);
        Page<TbPolicyFile> entityPage = aiPolicyFileMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<PolicyFileVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(f -> toVO(f, "USER"))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public TbPolicyFile getById(Long id) {
        TbPolicyFile file = aiPolicyFileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResponseCode.POLICY_FILE_NOT_FOUND);
        }
        // 校验归属（此表用 user_id 隔离）
        DataScopeHelper.checkOwnership(file.getUserId());
        return file;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbPolicyFile create(TbPolicyFile policyFile) {
        policyFile.setUserId(SecurityContextHolder.getUserId());
        if (policyFile.getStatus() == null) {
            policyFile.setStatus(1);
        }
        aiPolicyFileMapper.insert(policyFile);
        return policyFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        TbPolicyFile file = getById(id);
        // 只能删除自己的文件
        if (!file.getUserId().equals(SecurityContextHolder.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权删除该政策文件");
        }
        aiPolicyFileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long id, Integer status) {
        TbPolicyFile file = getById(id); // 内部已做归属校验
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
        LambdaQueryWrapper<TbPolicyFile> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(TbPolicyFile::getUserId, SecurityContextHolder.getUserId())
                .eq(TbPolicyFile::getStatus, 1);
        if (StringUtils.hasText(applicableCategory)) {
            userWrapper.and(w -> w.eq(TbPolicyFile::getApplicableCategory, applicableCategory)
                    .or().isNull(TbPolicyFile::getApplicableCategory));
        }
        List<TbPolicyFile> userFiles = aiPolicyFileMapper.selectList(userWrapper);
        for (TbPolicyFile f : userFiles) {
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

    private PolicyFileVO toVO(TbPolicyFile f, String source) {
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

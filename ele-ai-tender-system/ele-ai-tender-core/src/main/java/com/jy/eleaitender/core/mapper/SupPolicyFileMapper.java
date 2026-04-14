package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统政策文件Mapper（Core模块读取sup_policy_file表）
 */
@Mapper
public interface SupPolicyFileMapper extends BaseMapper<SupPolicyFile> {
}

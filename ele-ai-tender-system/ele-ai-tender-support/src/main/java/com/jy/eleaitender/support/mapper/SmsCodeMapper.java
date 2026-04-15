package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupSmsCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信验证码Mapper
 */
@Mapper
public interface SmsCodeMapper extends BaseMapper<SupSmsCode> {
}

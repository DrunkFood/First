package com.jy.eletender.crypto.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * `bdc_decrypt_request` 表访问入口。
 */
public interface BdcDecryptRequestMapper extends BaseMapper<BdcDecryptRequest> {
}

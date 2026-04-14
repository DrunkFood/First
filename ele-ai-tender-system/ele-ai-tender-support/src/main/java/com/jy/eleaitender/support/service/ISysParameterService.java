package com.jy.eleaitender.support.service;

import com.jy.eleaitender.common.entity.support.SysParameter;

import java.util.List;
import java.util.Map;

/**
 * 系统参数服务接口
 */
public interface ISysParameterService {

    /**
     * 按分组查询参数列表
     *
     * @param paramGroup 参数分组（可选）
     * @return 参数列表
     */
    List<SysParameter> listByGroup(String paramGroup);

    /**
     * 根据key获取参数值
     *
     * @param paramKey 参数键
     * @return 参数值
     */
    String getValueByKey(String paramKey);

    /**
     * 批量更新参数值
     *
     * @param params key-value 映射
     */
    void batchUpdate(Map<String, String> params);

    /**
     * 更新单个参数
     *
     * @param paramKey   参数键
     * @param paramValue 参数值
     */
    void updateByKey(String paramKey, String paramValue);
}

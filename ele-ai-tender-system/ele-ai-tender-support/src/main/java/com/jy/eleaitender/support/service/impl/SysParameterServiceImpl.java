package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jy.eleaitender.common.constant.RedisKeyConstant;
import com.jy.eleaitender.common.entity.support.SysParameter;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SysParameterMapper;
import com.jy.eleaitender.support.notifier.ThreadPoolConfigNotifier;
import com.jy.eleaitender.support.service.ISysParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统参数服务实现
 * 使用 Redis 缓存加速读取
 */
@Service
public class SysParameterServiceImpl implements ISysParameterService {

    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private SysParameterMapper sysParameterMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolConfigNotifier threadPoolConfigNotifier;

    @Override
    public List<SysParameter> listByGroup(String paramGroup) {
        LambdaQueryWrapper<SysParameter> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(paramGroup)) {
            wrapper.eq(SysParameter::getParamGroup, paramGroup);
        }
        wrapper.orderByAsc(SysParameter::getParamGroup)
               .orderByAsc(SysParameter::getSortOrder);
        return sysParameterMapper.selectList(wrapper);
    }

    @Override
    public String getValueByKey(String paramKey) {
        // 先查缓存
        String cacheKey = RedisKeyConstant.SYS_PARAM_PREFIX + paramKey;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查数据库
        LambdaQueryWrapper<SysParameter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysParameter::getParamKey, paramKey);
        SysParameter param = sysParameterMapper.selectOne(wrapper);
        if (param == null) {
            throw new BusinessException(ResponseCode.SYS_PARAM_NOT_FOUND);
        }
        // 写入缓存
        String value = param.getParamValue();
        if (value != null) {
            redisTemplate.opsForValue().set(cacheKey, value, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(Map<String, String> params) {
        boolean aiThreadPoolChanged = false;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramGroup = doUpdateByKey(entry.getKey(), entry.getValue());
            if ("AI_THREAD_POOL".equals(paramGroup)) {
                aiThreadPoolChanged = true;
            }
        }
        // 批量更新后统一通知一次
        if (aiThreadPoolChanged) {
            threadPoolConfigNotifier.onParameterChanged("AI_THREAD_POOL");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByKey(String paramKey, String paramValue) {
        String paramGroup = doUpdateByKey(paramKey, paramValue);
        threadPoolConfigNotifier.onParameterChanged(paramGroup);
    }

    /**
     * 实际执行参数更新，返回参数分组
     */
    private String doUpdateByKey(String paramKey, String paramValue) {
        LambdaQueryWrapper<SysParameter> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysParameter::getParamKey, paramKey)
                    .select(SysParameter::getParamGroup);
        SysParameter existing = sysParameterMapper.selectOne(queryWrapper);
        String paramGroup = existing != null ? existing.getParamGroup() : null;

        LambdaUpdateWrapper<SysParameter> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysParameter::getParamKey, paramKey)
               .set(SysParameter::getParamValue, paramValue);
        int rows = sysParameterMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.SYS_PARAM_NOT_FOUND);
        }
        String cacheKey = RedisKeyConstant.SYS_PARAM_PREFIX + paramKey;
        redisTemplate.delete(cacheKey);

        return paramGroup;
    }
}

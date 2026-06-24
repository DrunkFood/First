package com.jy.eleaitender.common.datascope;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * 数据隔离工具类
 * 提供管理员判断、用户ID获取、归属校验等通用方法
 */
@Slf4j
public class DataScopeHelper {

    private DataScopeHelper() {
    }

    /**
     * 判断当前用户是否管理员（ADMIN角色）
     */
    public static boolean isAdmin() {
        return SecurityContextHolder.isAdmin();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return SecurityContextHolder.getUserId();
    }

    /**
     * 判断是否跳过数据隔离
     * 只检查 SQL 解析前能确定的条件：
     * 1. 无用户上下文 → 跳过
     * 2. 管理员 → 跳过
     * 3. @DataScope(skip=true) → 跳过
     * <p>
     * 表级别的过滤由 DataScopeTenantHandler.ignoreTable() 在 SQL 解析阶段处理
     */
    public static boolean isDataScopeTable(String tableName) {
        return tableName != null && DataScopeTable.ISOLATED_TABLES.contains(tableName);
    }

    /**
     * 判断是否应跳过数据隔离过滤
     * 以下情况跳过：管理员 / 无用户上下文 / 非隔离表 / 方法标记 @DataScope(skip=true)
     */
    public static boolean shouldSkipDataScope(String tableName, MappedStatement ms) {
        // 无用户上下文（后台任务等）跳过
        Long userId = getCurrentUserId();
        if (userId == null) {
            return true;
        }
        // 管理员跳过
        if (isAdmin()) {
            return true;
        }
        // 非隔离表跳过
        if (tableName != null && !isDataScopeTable(tableName)) {
            return true;
        }
        // 方法标记 @DataScope(skip=true) 跳过
        if (ms != null && isDataScopeSkip(ms)) {
            return true;
        }
        // 其他情况不过滤
        return false;
    }

    /**
     * 校验数据归属，非管理员只能操作自己创建的数据
     * 系统创建的数据（createId=0或null）视为公共资源，不做归属限制
     *
     * @param dataCreateId 数据的 create_id 字段值
     * @throws BusinessException 如果无权访问
     */
    public static void checkOwnership(Long dataCreateId) {
        if (isAdmin()) {
            return;
        }
        // 系统创建的数据（createId=0或null）视为公共资源
        if (dataCreateId == null || dataCreateId == 0L) {
            return;
        }
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        if (!currentUserId.equals(dataCreateId)) {
            throw new BusinessException(ResponseCode.DATA_ACCESS_DENIED);
        }
    }

    /**
     * 检查 MappedStatement 对应的方法是否标记了 @DataScope(skip=true)
     */
    public static boolean isDataScopeSkip(MappedStatement ms) {
        String className = null;
        try {
            String id = ms.getId();
            int lastDot = id.lastIndexOf('.');
            if (lastDot < 0) {
                return false;
            }
            className = id.substring(0, lastDot);
            String methodName = id.substring(lastDot + 1);

            Class<?> mapperClass = Class.forName(className);
            DataScope classAnnotation = mapperClass.getAnnotation(DataScope.class);
            if (classAnnotation != null && classAnnotation.skip()) {
                return true;
            }

            for (java.lang.reflect.Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataScope methodAnnotation = method.getAnnotation(DataScope.class);
                    return methodAnnotation != null && methodAnnotation.skip();
                }
            }
        } catch (ClassNotFoundException e) {
            log.warn("Mapper接口找不到，跳过DataScope注解检查: mapperClass={}", className, e);
        }
        return false;
    }
}

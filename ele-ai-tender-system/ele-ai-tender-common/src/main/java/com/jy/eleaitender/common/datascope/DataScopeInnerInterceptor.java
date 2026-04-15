package com.jy.eleaitender.common.datascope;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;

/**
 * 数据隔离拦截器
 * 基于 MyBatis-Plus TenantLineInnerInterceptor 实现
 * 对隔离表自动追加 WHERE create_id = 当前用户ID
 *
 * 跳过条件：管理员 / 无用户上下文 / 非隔离表 / @DataScope(skip=true)
 */
public class DataScopeInnerInterceptor extends TenantLineInnerInterceptor {

    public DataScopeInnerInterceptor() {
        super(new DataScopeTenantHandler());
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler,
                            org.apache.ibatis.mapping.BoundSql boundSql) throws SQLException {
        // 检查是否应跳过数据隔离
        if (shouldSkip(ms)) {
            return;
        }
        super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
    }

    /**
     * 判断是否跳过数据隔离
     * 只检查 SQL 解析前能确定的条件：
     * 1. 无用户上下文 → 跳过
     * 2. 管理员 → 跳过
     * 3. @DataScope(skip=true) → 跳过
     *
     * 表级别的过滤由 DataScopeTenantHandler.ignoreTable() 在 SQL 解析阶段处理
     */
    private boolean shouldSkip(MappedStatement ms) {
        // 无用户上下文跳过（后台任务等）
        Long userId = SecurityContextHolder.getUserId();
        if (userId == null) {
            return true;
        }
        // 管理员跳过
        if (SecurityContextHolder.isAdmin()) {
            return true;
        }
        // 方法标记 @DataScope(skip=true) 跳过
        if (ms != null && DataScopeHelper.isDataScopeSkip(ms)) {
            return true;
        }
        return false;
    }

    /**
     * 数据隔离 TenantLineHandler 实现
     * 复用 TenantLineInnerInterceptor 的 SQL 改写能力
     * 对隔离表返回 create_id = 当前用户ID 的条件
     */
    private static class DataScopeTenantHandler implements TenantLineHandler {

        @Override
        public Expression getTenantId() {
            Long userId = SecurityContextHolder.getUserId();
            return userId != null ? new LongValue(userId) : new NullValue();
        }

        @Override
        public String getTenantIdColumn() {
            return DataScopeTable.SCOPE_COLUMN;
        }

        @Override
        public boolean ignoreTable(String tableName) {
            // 非隔离表忽略（使用 DataScopeHelper 确保 null 安全）
            return !DataScopeHelper.isDataScopeTable(tableName);
        }
    }
}

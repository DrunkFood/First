package com.jy.eleaitender.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.mapper.SysAccessLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * 通用日志自动配置
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AccessLogProperties.class)
public class LoggingAutoConfiguration {

    @Bean(name = "httpRequestLogFilterRegistration")
    @ConditionalOnMissingBean(name = "httpRequestLogFilterRegistration")
    public FilterRegistrationBean<HttpRequestLogFilter> httpRequestLogFilter(Environment environment,
                                                                             ObjectMapper objectMapper,
                                                                             AccessLogProperties properties,
                                                                             ObjectProvider<AccessLogPersistenceService> persistenceServiceProvider) {
        String serviceName = environment.getProperty("spring.application.name", "unknown-service");
        AccessLogPersistenceService persistenceService = persistenceServiceProvider.getIfAvailable();
        log.info("AccessLog filter init service={} enabled={} persistEnabled={} asyncPersist={}",
                serviceName,
                properties.isEnabled(),
                properties.isPersistEnabled(),
                properties.isAsyncPersist());
        if (persistenceService == null) {
            log.warn("AccessLog persistence service unavailable service={}, logs will not be persisted to DB", serviceName);
        }
        FilterRegistrationBean<HttpRequestLogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HttpRequestLogFilter(
                serviceName,
                objectMapper,
                properties,
                persistenceService));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean("accessLogTaskExecutor")
    @ConditionalOnMissingBean(name = "accessLogTaskExecutor")
    public TaskExecutor accessLogTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("access-log-writer");
            thread.setDaemon(true);
            return thread;
        }));
    }

    @Bean
    @ConditionalOnProperty(prefix = "ele-tender.logging.access", name = "persist-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public AccessLogPersistenceService accessLogPersistenceService(ObjectProvider<SysAccessLogMapper> accessLogMapperProvider,
                                                                   @Qualifier("accessLogTaskExecutor") TaskExecutor accessLogTaskExecutor,
                                                                   AccessLogProperties properties) {
        SysAccessLogMapper accessLogMapper = accessLogMapperProvider.getIfAvailable();
        if (accessLogMapper == null) {
            log.warn("AccessLog DB persistence disabled because SysAccessLogMapper bean is missing");
            return accessLog -> {
            };
        }
        log.info("AccessLog DB persistence enabled asyncPersist={}", properties.isAsyncPersist());
        return new DbAccessLogPersistenceService(accessLogMapper, accessLogTaskExecutor, properties);
    }
}

package com.jy.eleaitender.interaction.core.support;

import com.jy.eleaitender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

/**
 * 提取交互日志的业务上下文。
 */
@Slf4j
public final class InteractionLogContextExtractor {

    private InteractionLogContextExtractor() {
    }

    public static InteractionLogContext extract(String apiName, Object request, Object response) {
        InteractionLogContext context = new InteractionLogContext();
        context.setTraceId(InteractionTraceSupport.getTraceId());
        context.setApiName(apiName);
        context.setRequestType(resolveTypeName(request));
        context.setResponseType(resolveTypeName(response));
        merge(context, request);
        merge(context, response);
        return context;
    }

    private static void merge(InteractionLogContext context, Object source) {
        if (source == null) {
            return;
        }
        if (source instanceof InteractionResult) {
            merge(context, ((InteractionResult<?>) source).getData());
            return;
        }
        if (source instanceof HttpRequest) {
            mergeHttpRequest(context, (HttpRequest) source);
            return;
        }
        if (source instanceof Number || source instanceof CharSequence || source instanceof Boolean || source.getClass().isEnum()) {
            return;
        }
        Map<String, Object> values = describe(source);
        fillIfAbsent(context, "bizType", stringValue(values.get("bizType")));
        fillIfAbsent(context, "bizId", stringValue(values.get("bizId")));
        fillIfAbsent(context, "projectId", stringValue(values.get("projectId")));
        fillIfAbsent(context, "tenderId", stringValue(values.get("tenderId")));
        fillIfAbsent(context, "fileId", stringValue(values.get("fileId")));
        fillIfAbsent(context, "fileName", stringValue(values.get("fileName")));
        fillIfAbsent(context, "userId", stringValue(values.get("userId")));
        fillIfAbsent(context, "userName", stringValue(values.get("userName")));
        fillIfAbsent(context, "enterpriseId", stringValue(values.get("enterpriseId")));
        fillIfAbsent(context, "enterpriseName", stringValue(values.get("enterpriseName")));
        fillIfAbsent(context, "appKey", stringValue(values.get("appKey")));
    }

    private static void mergeHttpRequest(InteractionLogContext context, HttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        fillIfAbsent(context, "appKey", headers.getFirst(InteractionHeaderConstants.APP_KEY));

        URI uri = request.getURI();
        fillIfAbsent(context, "fileId", resolveFileId(uri));
        Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
        fillIfAbsent(context, "bizType", queryParams.get("bizType"));
        fillIfAbsent(context, "bizId", queryParams.get("bizId"));
        fillIfAbsent(context, "projectId", queryParams.get("projectId"));
        fillIfAbsent(context, "tenderId", queryParams.get("tenderId"));
    }

    private static String resolveFileId(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            return null;
        }
        String value = path.substring(lastSlash + 1);
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return null;
            }
        }
        return value;
    }

    private static Map<String, Object> describe(Object source) {
        try {
            PropertyDescriptor[] descriptors = Introspector.getBeanInfo(source.getClass(), Object.class).getPropertyDescriptors();
            java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<String, Object>();
            for (PropertyDescriptor descriptor : descriptors) {
                Method method = descriptor.getReadMethod();
                if (method == null) {
                    continue;
                }
                values.put(descriptor.getName(), method.invoke(source));
            }
            return values;
        } catch (IntrospectionException e) {
            log.warn("交互日志上下文内省失败: sourceType={}", resolveTypeName(source), e);
            return java.util.Collections.emptyMap();
        } catch (ReflectiveOperationException e) {
            log.warn("交互日志上下文读取属性失败: sourceType={}", resolveTypeName(source), e);
            return java.util.Collections.emptyMap();
        }
    }

    private static String resolveTypeName(Object source) {
        return source == null ? null : source.getClass().getSimpleName();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void fillIfAbsent(InteractionLogContext context, String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if ("bizType".equals(field) && isBlank(context.getBizType())) {
            context.setBizType(value);
            return;
        }
        if ("bizId".equals(field) && isBlank(context.getBizId())) {
            context.setBizId(value);
            return;
        }
        if ("projectId".equals(field) && isBlank(context.getProjectId())) {
            context.setProjectId(value);
            return;
        }
        if ("tenderId".equals(field) && isBlank(context.getTenderId())) {
            context.setTenderId(value);
            return;
        }
        if ("fileId".equals(field) && isBlank(context.getFileId())) {
            context.setFileId(value);
            return;
        }
        if ("fileName".equals(field) && isBlank(context.getFileName())) {
            context.setFileName(value);
            return;
        }
        if ("userId".equals(field) && isBlank(context.getUserId())) {
            context.setUserId(value);
            return;
        }
        if ("userName".equals(field) && isBlank(context.getUserName())) {
            context.setUserName(value);
            return;
        }
        if ("enterpriseId".equals(field) && isBlank(context.getEnterpriseId())) {
            context.setEnterpriseId(value);
            return;
        }
        if ("enterpriseName".equals(field) && isBlank(context.getEnterpriseName())) {
            context.setEnterpriseName(value);
            return;
        }
        if ("appKey".equals(field) && isBlank(context.getAppKey())) {
            context.setAppKey(value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

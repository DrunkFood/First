package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.TenderEntryContext;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import com.jy.eleaitender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionLogMasker;
import com.jy.eleaitender.interaction.core.support.InteractionTraceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 编制页跳转地址构造器
 */
@Slf4j
public class TenderDocumentEntryUrlBuilder {

    private final EleTenderInteractionProperties properties;

    public TenderDocumentEntryUrlBuilder(EleTenderInteractionProperties properties) {
        this.properties = properties;
    }

    public String build(TenderEntryContext context) {
        InteractionValidationUtils.validateTenderEntryContext(context);
        String entryPage = resolveEntryPage();
        String query = UriComponentsBuilder.newInstance()
                .queryParam("bizType", context.getBizType())
                .queryParam("bizId", context.getBizId())
                .queryParam("projectId", context.getProjectId())
                .queryParam("tenderId", context.getTenderId())
                .queryParam("token", context.getToken())
                .build()
                .encode()
                .toUriString();
        String url = appendQuery(entryPage, query);
        log.info("INTERACTION LOCAL traceId={} api=tender-document/entry-url success=true bizType={} bizId={} projectId={} tenderId={} page={} token={}",
                InteractionTraceSupport.getTraceId(),
                context.getBizType(),
                context.getBizId(),
                context.getProjectId(),
                context.getTenderId(),
                properties.getTenderDocumentPagePath(),
                InteractionLogMasker.maskToken(context.getToken()));
        return url;
    }

    private String resolveEntryPage() {
        String base = trimToEmpty(properties.getPageBaseUrl());
        String pagePath = properties.getTenderDocumentPagePath() == null ? "" : properties.getTenderDocumentPagePath();
        if (base.isEmpty()) {
            return pagePath;
        }
        if (pagePath.isEmpty() || base.endsWith(pagePath)) {
            return base;
        }
        if (base.endsWith("/") && pagePath.startsWith("/")) {
            return base + pagePath.substring(1);
        }
        if (!base.endsWith("/") && !pagePath.startsWith("/")) {
            return base + "/" + pagePath;
        }
        return base + pagePath;
    }

    private String appendQuery(String page, String query) {
        if (!query.startsWith("?")) {
            return page;
        }
        if (page.contains("#")) {
            return page + query;
        }
        return UriComponentsBuilder.fromHttpUrl(page)
                .query(query.substring(1))
                .build(true)
                .toUriString();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 回传网关占位实现。
 * 当前仅返回固定成功结果，方便在未接入外部业务系统时联调主流程。
 */
@Component
@ConditionalOnMissingBean(TenderDocumentCallbackGateway.class)
public class DefaultTenderDocumentCallbackGateway implements TenderDocumentCallbackGateway {

    /**
     * 占位签章文件回传。
     */
    @Override
    public TenderDocumentCallbackGatewayResult callbackSignedFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                                  String tenderId, TenderDocumentUserContext userContext) {
        return new TenderDocumentCallbackGatewayResult(true, "200", "签章文件回传成功");
    }

    /**
     * 占位数据包回传。
     */
    @Override
    public TenderDocumentCallbackGatewayResult callbackPackageFile(TenderDocument tenderDocument, TenderDocumentFile file,
                                                                   String tenderId, TenderDocumentUserContext userContext) {
        return new TenderDocumentCallbackGatewayResult(true, "200", "数据包回传成功");
    }
}

package com.jy.eletender.crypto.service;

import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.crypto.support.CryptoUserContext;

/**
 * 投标文件预存服务，负责写入 `bdc_bid_document` 并触发预存结果回调。
 */
public interface IBidDocumentService {

    /**
     * 推送投标文件预存。
     */
    BidDocumentPushResponse pushBidDocument(CryptoUserContext userContext, BidDocumentPushRequest request);
}

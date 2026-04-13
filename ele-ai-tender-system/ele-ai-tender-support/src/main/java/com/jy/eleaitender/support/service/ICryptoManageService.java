package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eleaitender.common.entity.crypto.BdcDecryptRequest;

public interface ICryptoManageService {

    Page<BdcDecryptRequest> getRequestPage(Integer pageNum,
                                           Integer pageSize,
                                           String appKey,
                                           String projectId,
                                           String tenderId,
                                           String bidRecordId,
                                           String status,
                                           String callbackStatus);

    BdcDecryptArtifact getArtifactDetail(Long artifactId);

    void retryRequest(Long requestId);
}

package com.jy.eletender.crypto.service;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.crypto.model.CallbackInvokeResult;

public interface ICallbackService {

    CallbackInvokeResult callbackBidDocumentResult(String appKey, Long fileId, String uploadResult, String traceId);

    CallbackInvokeResult callbackDecryptResult(BdcDecryptRequest request, BdcDecryptArtifact artifact, String traceId);
}

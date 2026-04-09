package com.jy.eletender.crypto.model;

import lombok.Data;

@Data
public class CallbackInvokeResult {

    private boolean success;

    private String responseCode;

    private String responseMessage;

    public static CallbackInvokeResult success(String responseCode, String responseMessage) {
        CallbackInvokeResult result = new CallbackInvokeResult();
        result.setSuccess(true);
        result.setResponseCode(responseCode);
        result.setResponseMessage(responseMessage);
        return result;
    }

    public static CallbackInvokeResult fail(String responseCode, String responseMessage) {
        CallbackInvokeResult result = new CallbackInvokeResult();
        result.setSuccess(false);
        result.setResponseCode(responseCode);
        result.setResponseMessage(responseMessage);
        return result;
    }
}

package com.jy.eletender.tenderdocument.support;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TenderDocumentBidRecordSyncResult {

    private Map<String, Object> projectInfo;
    private List<Map<String, Object>> tenderList = new ArrayList<>();

    /**
     * 标录原始结构化对象，由业务系统返回后原样进入快照。
     * 电子标系统不在这里强行固化字段模型。
     */
    private Object bidRecord;
}

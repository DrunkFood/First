package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class TenderDocumentBasicInfoPageResponse {

    private Map<String, Object> projectInfo;
    private List<Map<String, Object>> tenderList = new ArrayList<>();
    private Date lastSyncTime;
}

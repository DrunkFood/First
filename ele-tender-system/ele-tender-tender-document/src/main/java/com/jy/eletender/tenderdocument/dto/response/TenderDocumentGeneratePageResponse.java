package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TenderDocumentGeneratePageResponse {

    /**
     * 编制单状态枚举名，取值见 TenderDocumentStatus。
     */
    private String status;
    private Integer versionNo;

    private TenderDocumentFileView signedFile;
    private TenderDocumentFileView finalPackageFile;
    private TenderDocumentFileView compileInfoFile;
    private Date compileCompleteTime;
    private TenderDocumentGenerationRecordView latestGenerateRecord;

    /**
     * 统一回传按钮触发后的历史记录，后端内部仍按签章文件/数据包分别留痕。
     */
    private List<TenderDocumentCallbackHistoryView> callbackHistory = new ArrayList<>();
}

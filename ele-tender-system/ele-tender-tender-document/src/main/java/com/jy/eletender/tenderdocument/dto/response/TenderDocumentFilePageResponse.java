package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentFilePageResponse {

    private List<TenderDocumentFileView> fileList = new ArrayList<>();
}

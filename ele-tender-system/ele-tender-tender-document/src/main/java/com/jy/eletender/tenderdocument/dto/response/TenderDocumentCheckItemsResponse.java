package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentCheckItemsResponse {

    private List<TenderDocumentCheckItemView> itemList = new ArrayList<>();
}

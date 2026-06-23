package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.ai.dto.response.ModelConnectivityTestResponse;

public interface IModelConnectivityTestService {

    ModelConnectivityTestResponse testDeepSeek();

    ModelConnectivityTestResponse testZhiPu();
}

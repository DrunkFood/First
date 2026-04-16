package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.support.dto.ModelConfigCreateDTO;
import com.jy.eleaitender.support.dto.ModelConfigUpdateDTO;
import com.jy.eleaitender.support.vo.ModelConfigVO;

import java.util.List;

public interface IModelConfigService {
    Page<ModelConfigVO> getPage(Integer pageNum, Integer pageSize, String modelType, String provider, String usageScenario, String modelName, Integer isActive);
    List<ModelConfigVO> getActiveList();
    ModelConfigVO getDetailById(Long id);
    ModelConfigVO create(ModelConfigCreateDTO dto);
    void update(Long id, ModelConfigUpdateDTO dto);
    void deleteById(Long id);
    void deleteByIds(List<Long> ids);
    void setActive(Long id, Integer isActive);
}

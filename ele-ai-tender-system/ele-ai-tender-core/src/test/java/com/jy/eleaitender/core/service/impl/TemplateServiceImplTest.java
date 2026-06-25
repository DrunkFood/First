package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupTemplate;
import com.jy.eleaitender.core.mapper.SupTemplateMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupTemplate.class);
    }

    @InjectMocks
    private TemplateServiceImpl templateService;

    @Mock
    private SupTemplateMapper templateMapper;

    @Test
    void getPageFiltersTemplatesByCategoryOnly() {
        when(templateMapper.selectPage(any(Page.class), any())).thenReturn(new Page<>());

        templateService.getPage(1, 10, null, "SMALL_TRADE", "SERVICE");

        ArgumentCaptor<LambdaQueryWrapper<SupTemplate>> wrapperCaptor = wrapperCaptor();
        verify(templateMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertThat(paramValues(wrapperCaptor.getValue()))
                .contains("SMALL_TRADE")
                .doesNotContain("SERVICE");
    }

    @Test
    void getDefaultFiltersTemplatesByCategoryOnly() {
        when(templateMapper.selectOne(any())).thenReturn(new SupTemplate());

        templateService.getDefault("SMALL_TRADE", "SERVICE");

        ArgumentCaptor<LambdaQueryWrapper<SupTemplate>> wrapperCaptor = wrapperCaptor();
        verify(templateMapper).selectOne(wrapperCaptor.capture());
        assertThat(paramValues(wrapperCaptor.getValue()))
                .contains("SMALL_TRADE")
                .doesNotContain("SERVICE");
    }

    @Test
    void setDefaultClearsDefaultTemplatesByCategoryOnly() {
        SupTemplate template = new SupTemplate();
        template.setId(10L);
        template.setProjectCategory("SMALL_TRADE");
        template.setProjectType("SERVICE");
        SupTemplate oldDefault = new SupTemplate();
        oldDefault.setId(9L);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(templateMapper.selectList(any())).thenReturn(List.of(oldDefault));

        templateService.setDefault(10L);

        ArgumentCaptor<LambdaQueryWrapper<SupTemplate>> wrapperCaptor = wrapperCaptor();
        verify(templateMapper).selectList(wrapperCaptor.capture());
        assertThat(paramValues(wrapperCaptor.getValue()))
                .contains("SMALL_TRADE")
                .doesNotContain("SERVICE");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaQueryWrapper<SupTemplate>> wrapperCaptor() {
        return ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
    }

    private Collection<Object> paramValues(LambdaQueryWrapper<SupTemplate> wrapper) {
        wrapper.getSqlSegment();
        return wrapper.getParamNameValuePairs().values();
    }
}

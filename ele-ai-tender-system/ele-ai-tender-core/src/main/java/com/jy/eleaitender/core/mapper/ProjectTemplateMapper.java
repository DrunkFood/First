package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProjectTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目模板Mapper
 */
@Mapper
public interface ProjectTemplateMapper extends BaseMapper<TbProjectTemplate> {

    /**
     * 物理删除：按projectId删除所有记录（包括逻辑删除的），释放唯一约束
     */
    @Delete("DELETE FROM tb_project_template WHERE project_id = #{projectId}")
    int physicalDeleteByProjectId(Long projectId);
}

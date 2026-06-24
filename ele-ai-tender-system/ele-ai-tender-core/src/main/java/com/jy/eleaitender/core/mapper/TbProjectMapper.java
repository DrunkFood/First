package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI编制项目Mapper
 */
@Mapper
public interface TbProjectMapper extends BaseMapper<TbProject> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM tb_project WHERE project_code = #{projectCode}",
            "<if test='excludeId != null'>",
            "AND id != #{excludeId}",
            "</if>",
            "</script>"
    })
    long countByProjectCodeIncludingDeleted(@Param("projectCode") String projectCode,
                                            @Param("excludeId") Long excludeId);
}

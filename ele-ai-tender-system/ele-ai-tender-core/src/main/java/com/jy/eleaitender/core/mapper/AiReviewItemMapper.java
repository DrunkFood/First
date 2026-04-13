package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.core.entity.AiReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评审项Mapper
 */
@Mapper
public interface AiReviewItemMapper extends BaseMapper<AiReviewItem> {

    /**
     * 根据项目ID查询评审项列表
     */
    @Select("SELECT * FROM ai_review_item WHERE project_id = #{projectId} AND is_delete = 0 ORDER BY sort_order ASC, id ASC")
    List<AiReviewItem> selectByProjectId(@Param("projectId") Long projectId);
}

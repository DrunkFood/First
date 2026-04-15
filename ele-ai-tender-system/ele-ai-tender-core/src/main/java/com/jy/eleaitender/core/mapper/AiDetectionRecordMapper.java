package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.AiDetectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检测记录Mapper（Core模块）
 */
@Mapper
public interface AiDetectionRecordMapper extends BaseMapper<AiDetectionRecord> {

    /**
     * 根据项目ID查询检测记录
     */
    @Select("SELECT * FROM ai_detection_record WHERE project_id = #{projectId} AND is_delete = 0 ORDER BY id ASC")
    List<AiDetectionRecord> selectByProjectId(@Param("projectId") Long projectId);
}

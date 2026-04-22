package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.core.TbDetectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 检测记录Mapper（Core模块）
 */
@Mapper
public interface TbDetectionRecordMapper extends BaseMapper<TbDetectionRecord> {

    /**
     * 根据项目ID查询检测记录
     */
    @Select("SELECT * FROM tb_detection_record WHERE project_id = #{projectId} AND is_delete = 0 ORDER BY id ASC")
    List<TbDetectionRecord> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM tb_detection_record WHERE requirement_id = #{requirementId} AND is_delete = 0 ORDER BY id ASC")
    List<TbDetectionRecord> selectByRequirementId(@Param("requirementId") Long requirementId);
}

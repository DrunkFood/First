package com.jy.eleaitender.ai.mapper;

import com.jy.eleaitender.ai.dto.response.MatchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 需求匹配查询Mapper（AI模块只读）
 * 直接查询ai_requirement和ai_project表用于文档匹配
 */
@Mapper
public interface AiRequirementMatchMapper {

    /**
     * 按项目类型和类别查询历史需求（用于匹配评分）
     */
    @Select("""
            <script>
            SELECT r.id AS requirementId,
                   r.requirement_name AS requirementName,
                   p.project_name AS projectName,
                   p.project_type AS projectType,
                   p.project_category AS projectCategory,
                   p.budget AS budget,
                   SUBSTRING(r.content, 1, 200) AS contentPreview
            FROM ai_requirement r
            LEFT JOIN ai_project p ON r.project_id = p.id AND p.is_delete = 0
            WHERE r.is_delete = 0
              AND r.content IS NOT NULL
              AND r.content != ''
              <if test="projectType != null and projectType != ''">
                AND p.project_type = #{projectType}
              </if>
              <if test="projectCategory != null and projectCategory != ''">
                AND p.project_category = #{projectCategory}
              </if>
            ORDER BY r.create_time DESC
            LIMIT #{limit}
            </script>
            """)
    List<MatchResultVO> selectCandidateRequirements(
            @Param("projectType") String projectType,
            @Param("projectCategory") String projectCategory,
            @Param("limit") int limit);
}

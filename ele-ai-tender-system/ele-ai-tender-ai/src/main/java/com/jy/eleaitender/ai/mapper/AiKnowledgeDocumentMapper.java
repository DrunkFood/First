package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.ai.dto.response.MatchResultVO;
import com.jy.eleaitender.common.datascope.DataScope;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

    /**
     * 按项目类型和类别查询历史需求（用于匹配评分）
     * 跳过数据隔离：需求匹配需要跨用户查询历史数据
     */
    @DataScope(skip = true)
    @Select("""
            <script>
            SELECT r.id AS requirementId,
                   r.requirement_name AS requirementName,
                   p.project_name AS projectName,
                   p.project_type AS projectType,
                   p.project_category AS projectCategory,
                   p.budget AS budget,
                   SUBSTRING(r.content, 1, 200) AS contentPreview
            FROM tb_requirement r
                    LEFT JOIN tb_project p ON p.requirement_id = r.id AND p.is_delete = 0
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

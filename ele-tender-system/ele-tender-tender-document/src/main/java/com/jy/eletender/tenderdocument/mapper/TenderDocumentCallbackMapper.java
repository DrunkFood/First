package com.jy.eletender.tenderdocument.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.tenderdocument.entity.TenderDocumentCallback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TenderDocumentCallbackMapper extends BaseMapper<TenderDocumentCallback> {

    @Select({
            "<script>",
            "SELECT retry_count",
            "FROM td_tender_document_callback",
            "WHERE tender_document_id = #{tenderDocumentId}",
            "  AND version_no = #{versionNo}",
            "  AND file_role = #{fileRole}",
            "  <choose>",
            "    <when test='tenderId != null'>",
            "      AND tender_id = #{tenderId}",
            "    </when>",
            "    <otherwise>",
            "      AND tender_id IS NULL",
            "    </otherwise>",
            "  </choose>",
            "ORDER BY retry_count DESC, id DESC",
            "LIMIT 1",
            "FOR UPDATE",
            "</script>"
    })
    Integer selectMaxRetryCountForUpdate(@Param("tenderDocumentId") Long tenderDocumentId,
                                         @Param("versionNo") Integer versionNo,
                                         @Param("fileRole") String fileRole,
                                         @Param("tenderId") String tenderId);
}

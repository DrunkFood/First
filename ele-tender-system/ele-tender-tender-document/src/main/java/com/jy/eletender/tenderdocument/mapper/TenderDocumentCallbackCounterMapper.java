package com.jy.eletender.tenderdocument.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eletender.tenderdocument.entity.TenderDocumentCallbackCounter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TenderDocumentCallbackCounterMapper extends BaseMapper<TenderDocumentCallbackCounter> {

    @Insert({
            "INSERT INTO td_tender_document_callback_counter",
            "(tender_document_id, version_no, file_role, tender_id, current_retry_count,",
            " create_time, create_id, create_name, modify_time, modify_id, modify_name, ver, is_delete)",
            "VALUES (#{tenderDocumentId}, #{versionNo}, #{fileRole}, #{tenderId}, 1,",
            " NOW(), 0, '', NOW(), 0, '', 1, 0)",
            "ON DUPLICATE KEY UPDATE",
            " current_retry_count = current_retry_count + 1,",
            " modify_time = NOW(),",
            " modify_id = 0,",
            " modify_name = ''"
    })
    int upsertAndIncreaseRetryCount(@Param("tenderDocumentId") Long tenderDocumentId,
                                    @Param("versionNo") Integer versionNo,
                                    @Param("fileRole") String fileRole,
                                    @Param("tenderId") String tenderId);

    @Select({
            "SELECT current_retry_count",
            "FROM td_tender_document_callback_counter",
            "WHERE tender_document_id = #{tenderDocumentId}",
            "  AND version_no = #{versionNo}",
            "  AND file_role = #{fileRole}",
            "  AND tender_id = #{tenderId}",
            "LIMIT 1"
    })
    Integer selectCurrentRetryCount(@Param("tenderDocumentId") Long tenderDocumentId,
                                    @Param("versionNo") Integer versionNo,
                                    @Param("fileRole") String fileRole,
                                    @Param("tenderId") String tenderId);
}

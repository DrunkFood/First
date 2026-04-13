package com.jy.eleaitender.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.file.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}

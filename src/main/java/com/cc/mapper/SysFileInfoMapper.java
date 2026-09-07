package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.SysFileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统文件信息表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-06
 */
@Mapper
public interface SysFileInfoMapper extends BaseMapper<SysFileInfo> {
}

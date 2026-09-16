package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息通知 Mapper：单表 CRUD 走 BaseMapper，
 * 列表/计数/去重查询由 Service 层 LambdaQueryWrapper 组装（命中 idx_user_read_created）。
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}

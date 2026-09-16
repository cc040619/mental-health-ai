package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.UserNotifySetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息设置 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface UserNotifySettingMapper extends BaseMapper<UserNotifySetting> {
}

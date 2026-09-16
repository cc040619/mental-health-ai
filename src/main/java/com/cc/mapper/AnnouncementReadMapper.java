package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AnnouncementRead;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 公告已读 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface AnnouncementReadMapper extends BaseMapper<AnnouncementRead> {

    /**
     * 标记已读（唯一键 uk_announcement_user 防重，重复标记不报错、不重复插入）
     */
    @Insert("INSERT IGNORE INTO announcement_read(announcement_id, user_id, read_at) "
            + "VALUES(#{announcementId}, #{userId}, NOW())")
    int insertIgnore(@Param("announcementId") Long announcementId, @Param("userId") Long userId);
}

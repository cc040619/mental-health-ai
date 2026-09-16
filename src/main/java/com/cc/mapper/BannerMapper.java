package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.Banner;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页轮播图表 Mapper，继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {
}

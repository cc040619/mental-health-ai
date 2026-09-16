package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞表 Mapper，继承 BaseMapper 获得基础 CRUD
 *
 * @author cc
 * @date 2026-09-15
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
}

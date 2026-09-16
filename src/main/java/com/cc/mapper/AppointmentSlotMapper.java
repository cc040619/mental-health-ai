package com.cc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cc.entity.AppointmentSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 可预约时段 Mapper
 *
 * @author cc
 * @date 2026-09-14
 */
@Mapper
public interface AppointmentSlotMapper extends BaseMapper<AppointmentSlot> {

    /**
     * 原子抢占时段：仅 status=0 时置 1。
     * 影响行数 0 表示已被其他并发请求抢走，调用方抛业务异常——禁止先查后改（会超卖）。
     *
     * @param slotId 时段ID
     * @return 影响行数（1=抢占成功，0=时段不可约）
     */
    @Update("UPDATE appointment_slot SET status = 1 WHERE id = #{slotId} AND status = 0")
    int occupyIfFree(@Param("slotId") Long slotId);

    /**
     * 取消预约时释放时段：仅 status=1 时还原为 0；status=2(停用) 的时段不恢复。
     *
     * @param slotId 时段ID
     * @return 影响行数
     */
    @Update("UPDATE appointment_slot SET status = 0 WHERE id = #{slotId} AND status = 1")
    int releaseIfOccupied(@Param("slotId") Long slotId);
}

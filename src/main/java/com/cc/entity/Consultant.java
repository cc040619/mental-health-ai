package com.cc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 咨询师实体，对应 consultant 表。
 * rating/reviewCount/consultCount 为冗余统计字段，避免列表页实时聚合。
 * status 见 {@link com.cc.enums.ConsultantStatus}：0-待审核 1-正常 2-禁用。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@TableName(value = "consultant", autoResultMap = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consultant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联登录账号 id（user.user_type=3），唯一；创建咨询师时同步建号回填 */
    private Long userId;

    private String name;

    private String avatar;

    private String title;

    /** 擅长领域，逗号分隔中文领域名 */
    private String specialties;

    @TableField("years_of_practice")
    private Integer yearsOfPractice;

    private String intro;

    /** 平均评分(冗余)，评价提交时重算 */
    private BigDecimal rating;

    /** 评价次数(冗余) */
    @TableField("review_count")
    private Integer reviewCount;

    /** 咨询次数(冗余)，预约 CONFIRMED 时 +1 */
    @TableField("consult_count")
    private Integer consultCount;

    /** 0-待审核 1-正常 2-禁用，见 ConsultantStatus */
    private Integer status;

    /** 资质证明图片URL数组（JSON 列，JacksonTypeHandler 自动序列化；只存 OSS 上传产物） */
    @TableField(value = "qualification_images", typeHandler = JacksonTypeHandler.class)
    private List<String> qualificationImages;

    /** 驳回原因；非空表示「已驳回待修改」，重新提交保存时清空 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 最近审核时间 */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /** 入驻时间（审核通过时间） */
    @TableField("joined_at")
    private LocalDateTime joinedAt;

    /** 排序权重，越大越靠前；用户端列表 sort_weight 优先、rating/consult_count 次之 */
    @TableField("sort_weight")
    private Integer sortWeight;

    /** 咨询价格(元/小时，付费预留)；为空表示未设置，收入统计返回「未设置」 */
    private BigDecimal price;

    /** 可预约天数：用户端只能预约今天起未来 N 天（含今天） */
    @TableField("booking_window_days")
    private Integer bookingWindowDays;

    /** 用户取消时限：咨询开始前 N 小时内限制用户取消（0=不限）；咨询师/管理员取消不受此限 */
    @TableField("cancel_hours_before")
    private Integer cancelHoursBefore;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

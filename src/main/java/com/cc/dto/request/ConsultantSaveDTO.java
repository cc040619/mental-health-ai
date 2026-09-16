package com.cc.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 咨询师新增/编辑请求（id 为空=新增，非空=编辑）。
 * 新增记录默认待审核(status=0)；编辑待审核记录视为重新提交，Service 清空驳回原因。
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class ConsultantSaveDTO {

    /** 编辑时必传，新增时为空 */
    private Long id;

    /**
     * 登录用户名（仅新增时可选）：留空由系统生成 doc_xxxxxxxx。
     * 与 user.username 规则一致：3-50 位字母/数字/下划线。
     */
    @Pattern(regexp = "^$|^[a-zA-Z0-9_]{3,50}$", message = "登录用户名需为3-50位字母、数字或下划线")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50字")
    private String name;

    @Size(max = 500, message = "头像地址不能超过500字")
    private String avatar;

    @Size(max = 50, message = "职称不能超过50字")
    private String title;

    @Size(max = 200, message = "擅长领域不能超过200字")
    private String specialties;

    @NotNull(message = "从业年限不能为空")
    @Min(value = 0, message = "从业年限不能为负数")
    private Integer yearsOfPractice;

    @Size(max = 1000, message = "个人简介不能超过1000字")
    private String intro;

    /** 资质证明图片URL数组，只允许 OSS 上传产物（Service 校验域名），最多 9 张 */
    @Size(max = 9, message = "资质证明图片最多9张")
    private List<String> qualificationImages;

    /** 排序权重，越大越靠前；为空按 0 处理 */
    @Min(value = 0, message = "排序权重不能为负数")
    private Integer sortWeight;

    /** 咨询价格(元/小时，付费预留)，为空=未设置 */
    @DecimalMin(value = "0.00", message = "咨询价格不能为负数")
    private BigDecimal price;
}

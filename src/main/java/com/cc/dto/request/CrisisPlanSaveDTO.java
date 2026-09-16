package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 个人安全计划保存请求 DTO
 *
 * <p>仅本人可保存；lovedOnes 数组元素校验 name/relation/phone 长度，
 * copingStrategies 每项≤50，emergencyContact≤50。空数组允许（用户可分步填写）。</p>
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class CrisisPlanSaveDTO {

    /** 可信赖的人列表 */
    @Valid
    private List<LovedOne> lovedOnes;

    /** 应对策略字符串数组 */
    private List<@Size(max = 50, message = "应对策略每项不能超过50个字符") String> copingStrategies;

    /** 紧急联系方式 */
    @Size(max = 50, message = "紧急联系方式不能超过50个字符")
    private String emergencyContact;

    /**
     * 可信赖的人条目
     */
    @Data
    public static class LovedOne {
        @Size(max = 30, message = "姓名不能超过30个字符")
        private String name;

        @Size(max = 20, message = "关系不能超过20个字符")
        private String relation;

        @Size(max = 20, message = "电话不能超过20个字符")
        private String phone;
    }
}

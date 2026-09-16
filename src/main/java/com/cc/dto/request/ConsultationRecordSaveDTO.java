package com.cc.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 咨询记录保存 DTO（咨询师本人 upsert，一单一记录）。
 *
 * <p>字段长度与表定义一致：主诉≤1000、分析≤2000、干预方案≤2000、下次建议≤1000。
 * 各字段均允许为空（咨询师可分阶段填写）。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class ConsultationRecordSaveDTO {

    @Size(max = 1000, message = "主诉不能超过1000字")
    private String chiefComplaint;

    @Size(max = 2000, message = "分析不能超过2000字")
    private String analysis;

    @Size(max = 2000, message = "干预方案不能超过2000字")
    private String interventionPlan;

    @Size(max = 1000, message = "下次建议不能超过1000字")
    private String nextSuggestion;
}

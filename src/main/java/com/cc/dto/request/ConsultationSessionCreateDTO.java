package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationSessionCreateDTO {
    @Size(max = 200, message = "会话标题最多200个字符")
    private String sessionTitle;

    @NotBlank(message = "初始消息不能为空")
    @Size(max = 2000, message = "初始消息最多2000个字符")
    private String initialMessage;

    // 咨询场景：EMOTION/RELATIONSHIP/ACADEMIC/SLEEP/OTHER（合法值由 Service 层校验）
    @NotBlank(message = "请选择咨询场景")
    @Size(max = 20, message = "场景参数不合法")
    private String scene;
}

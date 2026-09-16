package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读通知数响应 DTO（读 Redis，O(1)）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountDTO {

    private Long count;
}

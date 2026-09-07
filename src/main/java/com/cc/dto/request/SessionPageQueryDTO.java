package com.cc.dto.request;

import lombok.Data;

/**
 * 咨询会话分页查询 DTO
 *
 * <p>用户端（consultation.vue）传 pageNum/pageSize，管理端（consultations.vue）传 currentPage/size，两者兼容。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Data
public class SessionPageQueryDTO {

    // 页码（用户端参数名）
    private Long pageNum;

    // 每页条数（用户端参数名）
    private Long pageSize;

    // 页码（管理端参数名）
    private Long currentPage;

    // 每页条数（管理端参数名）
    private Long size;

    /**
     * 获取页码，优先取 pageNum，其次 currentPage，默认 1
     */
    public long resolvePageNum() {
        if (pageNum != null && pageNum > 0) {
            return pageNum;
        }
        if (currentPage != null && currentPage > 0) {
            return currentPage;
        }
        return 1L;
    }

    /**
     * 获取每页条数，优先取 pageSize，其次 size，默认 10
     */
    public long resolvePageSize() {
        if (pageSize != null && pageSize > 0) {
            return pageSize;
        }
        if (size != null && size > 0) {
            return size;
        }
        return 10L;
    }
}

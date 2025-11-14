package com.lulajax.instagraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博主状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloggerStatusResponse {

    /**
     * 用户名
     */
    private String username;

    /**
     * 是否存在
     */
    private Boolean exists;

    /**
     * 是否已放弃
     */
    private Boolean abandoned;

    /**
     * 放弃时间戳
     */
    private Long abandonedAt;

    /**
     * 放弃原因
     */
    private String abandonedReason;

    /**
     * 当前分组
     */
    private String seedGroup;
}

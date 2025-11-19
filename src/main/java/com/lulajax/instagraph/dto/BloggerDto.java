package com.lulajax.instagraph.dto;

import lombok.Data;

/**
 * Blogger 的投影类，只包含基本属性，不包含关系集合。
 * 用于避免全量加载 followers/followings 等重型关系。
 */
@Data
public class BloggerDto {

    private String username;
    private String seedGroup;
    private String seedReason;
    private String bio;
    private String gender;
    private Long instagramId;
    private String country;
    private String dateJoined;
    private Long dateJoinedAsTimestamp;
    private String dateVerified;
    private Long dateVerifiedAsTimestamp;
    private Integer formerUsernames;
    private Boolean isVerified;
    private Boolean isPrivate;
    private String fullName;

    // 放弃相关字段
    private Boolean abandoned;
    private Long abandonedAt;
    private String abandonedReason;
}

package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.model.Blogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AggregationService {

    private static final Logger logger = LoggerFactory.getLogger(AggregationService.class);

    private final UserInfoService userInfoService;
    private final FollowingService followingService;
    private final PostService postService;
    private final TaggedPostService taggedPostService;

    public AggregationService(UserInfoService userInfoService,
                              FollowingService followingService,
                              PostService postService,
                              TaggedPostService taggedPostService) {
        this.userInfoService = userInfoService;
        this.followingService = followingService;
        this.postService = postService;
        this.taggedPostService = taggedPostService;
    }

    public Blogger aggregateUserData(String username) {
        logger.info("开始为用户 {} 聚合数据", username);
        // 1. 获取并更新博主信息
        logger.info("正在获取 {} 的用户信息", username);
        Blogger blogger = userInfoService.fetchUserInfoByUsernameV3(username);

        if (blogger == null || blogger.getInstagramId() == null) {
            logger.error("无法获取用户 {} 的基本信息或 Instagram ID", username);
            return null;
        }
        Long userId = blogger.getInstagramId();
        logger.info("成功获取到用户 {} 的信息，Instagram ID: {}", username, userId);

        // 2. 获取并存储用户的关注列表
        logger.info("正在获取 {} 的关注列表", username);
        followingService.fetchUserFollowingByUsername(username);
        logger.info("完成 {} 关注列表的获取", username);

        // 3. 获取并存储用户的帖子列表
        logger.info("正在获取用户 {} (ID: {}) 的帖子列表", username, userId);
        postService.fetchUserPostsByUserId(userId);
        logger.info("完成用户 {} (ID: {}) 帖子列表的获取", username, userId);

        // 4. 获取并存储用户被标记的帖子
        logger.info("正在获取用户 {} (ID: {}) 被标记的帖子", username, userId);
        taggedPostService.fetchUserTaggedPostsByUserId(userId);
        logger.info("完成用户 {} (ID: {}) 被标记帖子的获取", username, userId);

        logger.info("用户 {} 的数据聚合完成", username);
        return blogger;
    }
}

package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.api.dto.FollowingResponse;
import com.lulajax.instagraph.config.TikhubApiProperties;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.service.BloggerService;
import com.lulajax.instagraph.util.HttpUtil;
import com.lulajax.instagraph.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowingService {

    private static final Logger logger = LoggerFactory.getLogger(FollowingService.class);

    private final BloggerRepository bloggerRepository;
    private final TikhubApiProperties tikhubApiProperties;
    private final BloggerService bloggerService;

    public FollowingService(BloggerRepository bloggerRepository, TikhubApiProperties tikhubApiProperties, BloggerService bloggerService) {
        this.bloggerRepository = bloggerRepository;
        this.tikhubApiProperties = tikhubApiProperties;
        this.bloggerService = bloggerService;
    }

    public FollowingResponse testParseUserFollowing(String json) {
        logger.info("开始测试解析用户关注列表JSON");
        logger.debug("接收到的JSON: {}", json);
        FollowingResponse response = JsonUtil.parseObject(json, FollowingResponse.class);
        if (response != null) {
            logger.info("JSON解析成功");
        } else {
            logger.warn("JSON解析失败或结果为空");
        }
        return response;
    }

    public void fetchUserFollowingByUsername(String username) {
        logger.info("开始为用户 {} 获取关注列表", username);
        String url = tikhubApiProperties.getUrl().get("fetch-user-following-by-username") + "?username=" + username;
        logger.debug("请求URL: {}", url);

        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())
                .execute()
                .body();
        logger.debug("API 响应: {}", result);

        FollowingResponse response = JsonUtil.parseObject(result, FollowingResponse.class);
        if (response != null && response.getData() != null && response.getData().getData() != null && response.getData().getData().getItems() != null) {
            logger.info("成功获取到 {} 的 {} 个关注用户信息", username, response.getData().getData().getItems().size());
            Blogger follower = bloggerService.getOrCreateBlogger(username);

            List<FollowingResponse.FollowingItem> items = response.getData().getData().getItems();
            for (FollowingResponse.FollowingItem item : items) {
                Blogger followed = bloggerService.getOrCreateBloggerByInstagramId(
                    Long.parseLong(item.getId()), 
                    item.getUsername(), 
                    "default"
                );
                // 更新额外信息
                if (item.getFullName() != null) {
                    followed.setFullName(item.getFullName());
                }
                if (item.getIsVerified() != null) {
                    followed.setIsVerified(item.getIsVerified());
                }
                follower.getFollowings().add(followed);
                followed.getFollowers().add(follower);
                bloggerRepository.save(followed);
                logger.debug("已建立 {} -> {} 的关注关系", follower.getUsername(), followed.getUsername());
            }
            bloggerRepository.save(follower);
            logger.info("用户 {} 的关注列表处理完毕", username);
        } else {
            logger.warn("未能获取用户 {} 的关注列表，或列表为空", username);
        }
    }
}

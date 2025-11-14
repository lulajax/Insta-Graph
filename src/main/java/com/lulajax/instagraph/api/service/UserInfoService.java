package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.api.dto.UserInfoV2Response;
import com.lulajax.instagraph.api.dto.UserInfoV3Response;
import com.lulajax.instagraph.config.TikhubApiProperties;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.service.BloggerService;
import com.lulajax.instagraph.util.HttpUtil;
import com.lulajax.instagraph.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    private final BloggerRepository bloggerRepository;
    private final TikhubApiProperties tikhubApiProperties;
    private final BloggerService bloggerService;

    public UserInfoService(BloggerRepository bloggerRepository, TikhubApiProperties tikhubApiProperties, BloggerService bloggerService) {
        this.bloggerRepository = bloggerRepository;
        this.tikhubApiProperties = tikhubApiProperties;
        this.bloggerService = bloggerService;
    }

    public UserInfoV3Response testParseUserInfoV3(String json) {
        logger.info("开始测试解析用户信息JSON");
        logger.debug("接收到的JSON: {}", json);
        UserInfoV3Response response = JsonUtil.parseObject(json, UserInfoV3Response.class);
        if (response != null) {
            logger.info("JSON解析成功");
        } else {
            logger.warn("JSON解析失败或结果为空");
        }
        return response;
    }

    public Blogger fetchUserInfoByUsernameV3(String username) {
        logger.info("开始获取用户 {} 的信息 (V3)", username);
        String url = tikhubApiProperties.getUrl().get("fetch-user-info-by-username-v3") + "?username=" + username;
        logger.debug("请求URL: {}", url);

        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())
                .execute()
                .body();
        logger.debug("API 响应: {}", result);

        UserInfoV3Response response = JsonUtil.parseObject(result, UserInfoV3Response.class);
        if (response != null && response.getData() != null && response.getData().getData() != null) {
            logger.info("成功获取到用户 {} 的信息，正在更新数据库", username);
            UserInfoV3Response.UserInfoV3 userInfoV3 = response.getData().getData();
            Blogger blogger = bloggerService.getOrCreateBlogger(username);
            blogger.setCountry(userInfoV3.getCountry());
            blogger.setDateJoined(userInfoV3.getDateJoined());
            blogger.setDateJoinedAsTimestamp(userInfoV3.getDateJoinedAsTimestamp());
            blogger.setDateVerified(userInfoV3.getDateVerified());
            blogger.setDateVerifiedAsTimestamp(userInfoV3.getDateVerifiedAsTimestamp());
            blogger.setFormerUsernames(userInfoV3.getFormerUsernames());
            blogger.setInstagramId(userInfoV3.getId());
            blogger.setIsVerified(userInfoV3.getIsVerified());
            blogger.setBio(userInfoV3.getBio());
            blogger.setFullName(userInfoV3.getFullName());
            Blogger savedBlogger = bloggerRepository.save(blogger);
            logger.info("用户信息更新完毕: {}", savedBlogger);
            return savedBlogger;
        }
        logger.warn("未能获取用户 {} 的信息，或信息为空", username);
        return null;
    }

    public Blogger fetchUserInfoByUsernameV2(String username) {
        logger.info("开始获取用户 {} 的信息 (V2)", username);
        String url = tikhubApiProperties.getUrl().get("fetch-user-info-by-username-v2") + "?username=" + username;
        logger.debug("请求URL: {}", url);

        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())
                .execute()
                .body();
        logger.debug("API 响应: {}", result);

        UserInfoV2Response response = JsonUtil.parseObject(result, UserInfoV2Response.class);
        if (response != null && response.getData() != null) {
            logger.info("成功获取到用户 {} 的信息，正在更新数据库", username);
            UserInfoV2Response.UserInfoV2Data userInfoV2Data = response.getData();
            Blogger blogger = bloggerService.getOrCreateBlogger(username);

            blogger.setFullName(userInfoV2Data.getFullName());
            blogger.setBio(userInfoV2Data.getBiography());
            blogger.setIsVerified(userInfoV2Data.isVerified());
            if (userInfoV2Data.getId() != null) {
                try {
                    blogger.setInstagramId(Long.parseLong(userInfoV2Data.getId()));
                } catch (NumberFormatException e) {
                    logger.error("无法将ID {} 解析为Long类型", userInfoV2Data.getId(), e);
                }
            }
            
            Blogger savedBlogger = bloggerRepository.save(blogger);
            logger.info("用户信息更新完毕: {}", savedBlogger);
            return savedBlogger;
        }
        logger.warn("未能获取到用户 {} 的有效信息", username);
        return null;
    }
}

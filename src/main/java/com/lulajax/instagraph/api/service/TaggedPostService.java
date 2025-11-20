package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.api.dto.TaggedPostResponse;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.config.TikhubApiProperties;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.PostRepository;
import com.lulajax.instagraph.service.ApiLogService;
import com.lulajax.instagraph.service.BloggerService;
import com.lulajax.instagraph.util.HttpUtil;
import com.lulajax.instagraph.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaggedPostService {

    private static final Logger logger = LoggerFactory.getLogger(TaggedPostService.class);

    private final BloggerRepository bloggerRepository;
    private final PostRepository postRepository;
    private final TikhubApiProperties tikhubApiProperties;
    private final PostInfoService postInfoService;
    private final BloggerService bloggerService;
    private final ApiLogService apiLogService;

    public TaggedPostService(BloggerRepository bloggerRepository, PostRepository postRepository, TikhubApiProperties tikhubApiProperties, PostInfoService postInfoService, BloggerService bloggerService, ApiLogService apiLogService) {
        this.bloggerRepository = bloggerRepository;
        this.postRepository = postRepository;
        this.tikhubApiProperties = tikhubApiProperties;
        this.postInfoService = postInfoService;
        this.bloggerService = bloggerService;
        this.apiLogService = apiLogService;
    }

    public TaggedPostResponse testParseUserTaggedPosts(String json) {
        logger.info("开始测试解析用户被标记帖子列表JSON");
        logger.debug("接收到的JSON: {}", json);
        TaggedPostResponse response = JsonUtil.parseObject(json, TaggedPostResponse.class);
        if (response != null) {
            logger.info("JSON解析成功");
        } else {
            logger.warn("JSON解析失败或结果为空");
        }
        return response;
    }

    public void fetchUserTaggedPostsByUserId(Long userId) {
        logger.info("开始获取用户 (ID: {}) 被标记的帖子列表", userId);
        Blogger taggedBlogger = bloggerRepository.findByInstagramId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Blogger with Instagram ID " + userId + " not found."));

        String url = tikhubApiProperties.getUrl().get("fetch-user-tagged-posts-by-user-id") + "?user_id=" + userId + "&count=20";
        logger.debug("请求URL: {}", url);
        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())
                .execute()
                .body();
        logger.debug("API 响应: {}", result);
        apiLogService.saveLog("TaggedPostService", url, result);

        
        TaggedPostResponse response = null;
        try {
            response = JsonUtil.parseObject(result, TaggedPostResponse.class);
        } catch (Exception e) {
            taggedBlogger.setAggregationReason("无法采集，用户可能不存在被标记的帖子，请稍后重试");
            bloggerRepository.save(taggedBlogger);
            throw new RuntimeException("无法采集，用户可能不存在被标记的帖子，请稍后重试");
        }

        if (response != null && response.getData() != null && response.getData().getData() != null && response.getData().getData().getUser() != null &&
                response.getData().getData().getUser().getEdgeUserToPhotosOfYou() != null) {

            logger.info("成功获取到用户 {} 的 {} 个被标记的帖子", taggedBlogger.getUsername(), response.getData().getData().getUser().getEdgeUserToPhotosOfYou().getEdges().size());
            for (TaggedPostResponse.TaggedPostEdge edge : response.getData().getData().getUser().getEdgeUserToPhotosOfYou().getEdges()) {
                TaggedPostResponse.PostNode node = edge.getNode();
                logger.debug("正在处理帖子 ID: {}", node.getId());

                // Create or update the Post
                Post post = postRepository.findById(node.getId()).orElse(new Post(node.getId()));
                post.setShortcode(node.getShortcode());
                post.setDisplayUrl(node.getDisplayUrl());
                post.setIsVideo(node.isVideo());
                post.setVideoViewCount(node.getVideoViewCount());
                post.setCommentCount(node.getEdgeMediaToComment().getCount());
                post.setLikeCount(node.getEdgeLikedBy().getCount());
                post.setTimestamp(node.getTakenAtTimestamp());
                if (node.getEdgeMediaToCaption() != null && !node.getEdgeMediaToCaption().getEdges().isEmpty()) {
                    post.setCaption(node.getEdgeMediaToCaption().getEdges().get(0).getNode().getText());
                }
                if (node.getEdgeLikedBy() != null) {
                    post.setLikeCount(node.getEdgeLikedBy().getCount());
                }
                postRepository.save(post);

                try {
                    // 调用 PostInfoService 获取帖子详细信息
                    postInfoService.fetchPostInfoByPostId(node.getId());
                } catch (Exception e) {
                    logger.error("获取帖子 {} 详细信息失败: {}", node.getId(), e.getMessage());
                }

                // Create or update the post owner and link to the post
                TaggedPostResponse.Owner ownerDto = node.getOwner();
                logger.debug("处理帖子所有者: {}", ownerDto.getUsername());
                Blogger owner = bloggerService.getOrCreateBloggerByInstagramId(
                    Long.parseLong(ownerDto.getId()), 
                    ownerDto.getUsername(), 
                    "default"
                );
                // owner.getPosts().add(post);
                // post.setOwner(owner);
                bloggerRepository.save(owner);
                bloggerRepository.createPostedRelationship(owner.getUsername(), post.getId());
                
                // Link the tagged blogger to the post
                // post.getTaggedInUsers().add(taggedBlogger);
                // taggedBlogger.getTaggedInPosts().add(post);
                // postRepository.save(post);
                bloggerRepository.save(taggedBlogger);
                bloggerRepository.createTaggedInRelationship(taggedBlogger.getUsername(), post.getId());
            }
            logger.info("用户 {} 被标记的帖子列表处理完毕", taggedBlogger.getUsername());
        } else {
            logger.warn("未能获取用户 (ID: {}) 被标记的帖子列表，或列表为空", userId);
        }
    }
}

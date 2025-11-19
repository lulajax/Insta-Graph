package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.api.dto.UserPostResponse;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.config.TikhubApiProperties;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.PostRepository;
import com.lulajax.instagraph.service.BloggerService;
import com.lulajax.instagraph.util.HttpUtil;
import com.lulajax.instagraph.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final BloggerRepository bloggerRepository;
    private final PostRepository postRepository;
    private final TikhubApiProperties tikhubApiProperties;
    private final BloggerService bloggerService;

    public PostService(BloggerRepository bloggerRepository, PostRepository postRepository, TikhubApiProperties tikhubApiProperties, BloggerService bloggerService) {
        this.bloggerRepository = bloggerRepository;
        this.postRepository = postRepository;
        this.tikhubApiProperties = tikhubApiProperties;
        this.bloggerService = bloggerService;
    }

    public UserPostResponse testParseUserPosts(String json) {
        logger.info("开始测试解析用户帖子列表JSON");
        logger.debug("接收到的JSON: {}", json);
        UserPostResponse response = JsonUtil.parseObject(json, UserPostResponse.class);
        if (response != null) {
            logger.info("JSON解析成功");
        } else {
            logger.warn("JSON解析失败或结果为空");
        }
        return response;
    }

    public void fetchUserPostsByUserId(Long userId) {
        logger.info("开始获取用户 (ID: {}) 的帖子列表", userId);
        Optional<Blogger> bloggerOpt = bloggerRepository.findByInstagramId(userId);
        if (bloggerOpt.isEmpty()) {
            logger.error("未找到 Instagram ID 为 {} 的博主", userId);
            throw new IllegalArgumentException("Blogger with Instagram ID " + userId + " not found.");
        }
        Blogger blogger = bloggerOpt.get();

        String url = tikhubApiProperties.getUrl().get("fetch-user-posts-by-user-id") + "?user_id=" + userId + "&count=20";
        logger.debug("请求URL: {}", url);

        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())
                .execute()
                .body();
        logger.debug("API 响应: {}", result);

        UserPostResponse response = null;
        try {
            response = JsonUtil.parseObject(result, UserPostResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("无法采集，用户可能不存在发布的帖子，请稍后重试");
        }

        if (response != null && response.getData() != null && response.getData().getData() != null && response.getData().getData().getUser() != null &&
                response.getData().getData().getUser().getEdgeOwnerToTimelineMedia() != null) {
            logger.info("成功获取到用户 {} 的 {} 个帖子信息", blogger.getUsername(), response.getData().getData().getUser().getEdgeOwnerToTimelineMedia().getEdges().size());

            for (UserPostResponse.UserPostEdge edge : response.getData().getData().getUser().getEdgeOwnerToTimelineMedia().getEdges()) {
                UserPostResponse.PostNode node = edge.getNode();
                logger.debug("正在处理帖子 ID: {}", node.getId());
                Post post = postRepository.findById(node.getId()).orElse(new Post(node.getId()));
                post.setDisplayUrl(node.getDisplayUrl());
                post.setIsVideo(node.isVideo());
                post.setShortcode(node.getShortcode());
                if(node.getEdgeMediaToCaption() != null && !node.getEdgeMediaToCaption().getEdges().isEmpty()){
                    post.setCaption(node.getEdgeMediaToCaption().getEdges().get(0).getNode().getText());
                }

                // Tagged Users
                if(node.getEdgeMediaToTaggedUser() != null){
                    logger.debug("帖子 {} 中有 {} 个被标记的用户", node.getId(), node.getEdgeMediaToTaggedUser().getEdges().size());
                    for(UserPostResponse.TaggedUserEdge taggedUserEdge : node.getEdgeMediaToTaggedUser().getEdges()){
                        UserPostResponse.TaggedUser userDto = taggedUserEdge.getNode().getUser();
                        logger.debug("处理被标记用户: {}", userDto.getUsername());
                        Blogger taggedBlogger = bloggerService.getOrCreateBloggerByInstagramId(
                            Long.parseLong(userDto.getId()), 
                            userDto.getUsername(), 
                            "default"
                        );
                        // 更新fullName
                        if (userDto.getFullName() != null) {
                            taggedBlogger.setFullName(userDto.getFullName());
                        }
                        // Manually update both sides of the relationship
                        post.getTaggedInUsers().add(taggedBlogger);
                        taggedBlogger.getTaggedInPosts().add(post);
                        bloggerRepository.save(taggedBlogger);
                    }
                }

                // Liked By Users
                if(node.getEdgeMediaPreviewLike() != null){
                    logger.debug("帖子 {} 中有 {} 个点赞用户", node.getId(), node.getEdgeMediaPreviewLike().getEdges().size());
                    for(UserPostResponse.LikedByEdge likedByEdge : node.getEdgeMediaPreviewLike().getEdges()){
                        UserPostResponse.LikedByNode likedByNode = likedByEdge.getNode();
                        logger.debug("处理点赞用户: {}", likedByNode.getUsername());
                        Blogger liker = bloggerService.getOrCreateBloggerByInstagramId(
                            Long.parseLong(likedByNode.getId()), 
                            likedByNode.getUsername(), 
                            "default"
                        );
                        // Manually update both sides of the relationship
                        post.getLikedBy().add(liker);
                        liker.getLikedPosts().add(post);
                        bloggerRepository.save(liker);
                    }
                }
                
                postRepository.save(post);
                blogger.getPosts().add(post);
                post.setOwner(blogger);
            }
            bloggerRepository.save(blogger);
            logger.info("用户 {} 的帖子列表处理完毕", blogger.getUsername());
        } else {
            logger.warn("未能获取用户 (ID: {}) 的帖子列表，或列表为空", userId);
        }
    }
}

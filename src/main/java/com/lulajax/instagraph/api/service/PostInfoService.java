package com.lulajax.instagraph.api.service;

import com.lulajax.instagraph.api.dto.PostInfoResponse;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Location;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.config.TikhubApiProperties;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.LocationRepository;
import com.lulajax.instagraph.repository.PostRepository;
import com.lulajax.instagraph.util.HttpUtil;
import com.lulajax.instagraph.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PostInfoService {

    private static final Logger logger = LoggerFactory.getLogger(PostInfoService.class);

    private final PostRepository postRepository;
    private final BloggerRepository bloggerRepository;
    private final LocationRepository locationRepository;
    private final TikhubApiProperties tikhubApiProperties;

    public PostInfoService(PostRepository postRepository, BloggerRepository bloggerRepository, LocationRepository locationRepository, TikhubApiProperties tikhubApiProperties) {
        this.postRepository = postRepository;
        this.bloggerRepository = bloggerRepository;
        this.locationRepository = locationRepository;
        this.tikhubApiProperties = tikhubApiProperties;
    }

    public PostInfoResponse testParsePostInfo(String json) {
        logger.info("开始测试解析帖子详细信息JSON");
        logger.debug("接收到的JSON: {}", json);
        PostInfoResponse response = JsonUtil.parseObject(json, PostInfoResponse.class);
        if (response != null) {
            logger.info("JSON解析成功");
        } else {
            logger.warn("JSON解析失败或结果为空");
        }
        return response;
    }

    public void fetchPostInfoByPostId(String postId) {
        logger.info("开始获取帖子 {} 的详细信息", postId);

        String url = tikhubApiProperties.getUrl().get("fetch-post-info-by-post-id") + "?post_id=" + postId;
        logger.debug("请求URL: {}", url);

        String result = HttpUtil.createGet(url)
                .header("x-rapidapi-host", tikhubApiProperties.getXRapidapiHost())
                .header("x-rapidapi-key", tikhubApiProperties.getXRapidapiKey())                .execute()
                .body();
        logger.debug("API 响应: {}", result);

        PostInfoResponse response = JsonUtil.parseObject(result, PostInfoResponse.class);

        if (response != null && response.getData() != null) {
            logger.info("成功获取到帖子 {} 的详细信息，开始处理", postId);

            Post post = postRepository.findById(postId).orElseGet(() -> {
                logger.info("数据库中不存在帖子 {}，将创建一个新记录", postId);
                Post newPost = new Post();
                newPost.setId(postId);
                return newPost;
            });

            PostInfoResponse.PostInfo postInfo = response.getData();
            post.setTitle(postInfo.getTitle());
            post.setVideoDuration(postInfo.getVideoDuration());
            post.setVideoPlayCount(postInfo.getVideoPlayCount());
            
            // Create or update the post owner and link to the post
            if (postInfo.getOwner() != null) {
                PostInfoResponse.Owner ownerDto = postInfo.getOwner();
                logger.debug("处理帖子所有者: {}", ownerDto.getUsername());
                Blogger owner = bloggerRepository.findByInstagramId(Long.parseLong(ownerDto.getId())).orElseGet(() -> {
                    logger.info("帖子所有者 {} (ID: {}) 不在数据库中，将创建新记录", ownerDto.getUsername(), ownerDto.getId());
                    Blogger newOwner = new Blogger(ownerDto.getUsername(), "default");
                    newOwner.setInstagramId(Long.parseLong(ownerDto.getId()));
                    return bloggerRepository.save(newOwner);
                });
                // Manually update both sides of the relationship
                owner.getPosts().add(post);
                post.setOwner(owner);
                bloggerRepository.save(owner);
            }

            // Location
            if(postInfo.getLocation() != null){
                PostInfoResponse.Location locDto = postInfo.getLocation();
                logger.debug("处理地理位置信息: {}", locDto.getName());
                Location location = locationRepository.findById(locDto.getId()).orElse(new Location(locDto.getId()));
                location.setName(locDto.getName());
                location.setSlug(locDto.getSlug());
                location.setAddressJson(locDto.getAddressJson());
                locationRepository.save(location);
                post.setLocation(location);
            }

            // Tagged Users
            if(postInfo.getEdgeMediaToTaggedUser() != null){
                logger.info("帖子 {} 中有 {} 个被标记的用户", postId, postInfo.getEdgeMediaToTaggedUser().getEdges().size());
                for(PostInfoResponse.TaggedUserEdge edge : postInfo.getEdgeMediaToTaggedUser().getEdges()){
                    PostInfoResponse.TaggedUser userDto = edge.getNode().getUser();
                    logger.debug("处理被标记的用户: {}", userDto.getUsername());
                    Blogger taggedBlogger = bloggerRepository.findByInstagramId(Long.parseLong(userDto.getId())).orElseGet(() -> {
                        logger.info("被标记的用户 {} (ID: {}) 不在数据库中，将创建新记录", userDto.getUsername(), userDto.getId());
                        Blogger newBlogger = new Blogger(userDto.getUsername(), "default");
                        newBlogger.setInstagramId(Long.parseLong(userDto.getId()));
                        newBlogger.setFullName(userDto.getFullName());
                        return bloggerRepository.save(newBlogger);
                    });
                    // Manually update both sides of the relationship
                    post.getTaggedInUsers().add(taggedBlogger);
                    taggedBlogger.getTaggedInPosts().add(post);
                    bloggerRepository.save(taggedBlogger);
                }
            }    

            // Liked By Users
            if(postInfo.getEdgeMediaPreviewLike() != null){
                logger.info("帖子 {} 有 {} 个点赞用户", postId, postInfo.getEdgeMediaPreviewLike().getEdges().size());
                for(PostInfoResponse.LikedByEdge likedByEdge : postInfo.getEdgeMediaPreviewLike().getEdges()){
                    PostInfoResponse.LikedByNode likedByNode = likedByEdge.getNode();
                    logger.debug("处理点赞用户: {}", likedByNode.getUsername());
                    Blogger liker = bloggerRepository.findByInstagramId(Long.parseLong(likedByNode.getId())).orElseGet(() -> {
                        logger.info("点赞用户 {} (ID: {}) 不在数据库中，将创建新记录", likedByNode.getUsername(), likedByNode.getId());
                        Blogger newLiker = new Blogger(likedByNode.getUsername(), "default");
                        newLiker.setInstagramId(Long.parseLong(likedByNode.getId()));
                        return bloggerRepository.save(newLiker);
                    });
                    // Manually update both sides of the relationship
                    post.getLikedBy().add(liker);
                    liker.getLikedPosts().add(post);
                    bloggerRepository.save(liker);
                }
            }

            postRepository.save(post);
            logger.info("帖子 {} 的详细信息处理完毕", postId);
        } else {
            logger.warn("未能获取帖子 {} 的详细信息，或信息为空", postId);
        }
    }
}

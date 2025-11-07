package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lulajax.instagraph.api.dto.UserPostResponse;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/test/parse")
    @Operation(summary = "测试解析用户帖子列表JSON", description = "接收一个JSON字符串，解析为UserPostResponse对象并返回。")
    @Tag(name = "测试接口")
    public UserPostResponse testParseUserPosts(@RequestBody String json) {
        return postService.testParseUserPosts(json);
    }

    @GetMapping("/fetch/{userId}")
    @Operation(summary = "获取并存储用户的帖子列表", description = "根据用户ID获取其发布的帖子列表，并在图数据库中创建'Post'节点和'POSTED'关系。")
    @Tag(name = "数据采集")
    public void fetchUserPosts(@PathVariable Long userId) {
        postService.fetchUserPostsByUserId(userId);
    }
}

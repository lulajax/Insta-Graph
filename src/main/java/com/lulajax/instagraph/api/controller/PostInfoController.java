package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.PostInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lulajax.instagraph.api.dto.PostInfoResponse;

@RestController
@RequestMapping("/api/post-info")
public class PostInfoController {

    private final PostInfoService postInfoService;

    public PostInfoController(PostInfoService postInfoService) {
        this.postInfoService = postInfoService;
    }

    @PostMapping("/test/parse")
    @Operation(summary = "测试解析帖子详细信息JSON", description = "接收一个JSON字符串，解析为PostInfoResponse对象并返回。")
    @Tag(name = "测试接口")
    public PostInfoResponse testParsePostInfo(@RequestBody String json) {
        return postInfoService.testParsePostInfo(json);
    }

    @GetMapping("/fetch/{postId}")
    @Operation(summary = "获取并更新帖子的详细信息", description = "根据帖子ID获取其详细信息，并更新图数据库中的'Post'节点。")
    @Tag(name = "数据采集")
    public void fetchPostInfo(@PathVariable String postId) {
        postInfoService.fetchPostInfoByPostId(postId);
    }
}

package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.TaggedPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.lulajax.instagraph.api.dto.TaggedPostResponse;

@RestController
@RequestMapping("/api/tagged-posts")
public class TaggedPostController {

    private final TaggedPostService taggedPostService;

    public TaggedPostController(TaggedPostService taggedPostService) {
        this.taggedPostService = taggedPostService;
    }

    @PostMapping("/test/parse")
    @Operation(summary = "测试解析用户被标记帖子的JSON", description = "接收一个JSON字符串，解析为TaggedPostResponse对象并返回。")
    @Tag(name = "测试接口")
    public TaggedPostResponse testParseUserTaggedPosts(@RequestBody String json) {
        return taggedPostService.testParseUserTaggedPosts(json);
    }

    @GetMapping("/fetch/{userId}")
    @Operation(summary = "获取并存储用户被标记的帖子", description = "根据用户ID获取其被标记的帖子列表，并建立相应的图关系。")
    @Tag(name = "数据采集")
    public void fetchUserTaggedPosts(@PathVariable Long userId) {
        taggedPostService.fetchUserTaggedPostsByUserId(userId);
    }
}

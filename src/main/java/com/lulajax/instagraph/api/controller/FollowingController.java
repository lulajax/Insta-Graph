package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.FollowingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lulajax.instagraph.api.dto.FollowingResponse;

@RestController
@RequestMapping("/api/following")
public class FollowingController {

    private final FollowingService followingService;

    public FollowingController(FollowingService followingService) {
        this.followingService = followingService;
    }

    @PostMapping("/test/parse")
    @Operation(summary = "测试解析用户关注列表JSON", description = "接收一个JSON字符串，解析为FollowingResponse对象并返回。")
    @Tag(name = "测试接口")
    public FollowingResponse testParseUserFollowing(@RequestBody String json) {
        return followingService.testParseUserFollowing(json);
    }

    @GetMapping("/fetch/{username}")
    @Operation(summary = "获取并存储用户的关注列表", description = "根据用户名获取其关注的博主列表，并在图数据库中建立'FOLLOWS'关系。")
    @Tag(name = "数据采集")
    public void fetchUserFollowing(@PathVariable String username) {
        followingService.fetchUserFollowingByUsername(username);
    }
}

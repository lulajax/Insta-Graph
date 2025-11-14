package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.UserInfoService;
import com.lulajax.instagraph.model.Blogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lulajax.instagraph.api.dto.UserInfoV3Response;

@RestController
@RequestMapping("/api/blogger")
public class UserInfoController {

    private final UserInfoService userInfoService;

    public UserInfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @PostMapping("/test/parse")
    @Operation(summary = "测试解析用户信息JSON", description = "接收一个JSON字符串，解析为UserInfoV3Response对象并返回。")
    @Tag(name = "测试接口")
    public UserInfoV3Response testParseUserInfo(@RequestBody String json) {
        return userInfoService.testParseUserInfoV3(json);
    }

    @GetMapping("/fetch/{username}")
    @Operation(summary = "获取并更新博主信息", description = "根据用户名获取博主信息并更新到图数据库。")
    @Tag(name = "数据采集")
    public ResponseEntity<Blogger> fetchBlogger(@PathVariable String username) {
        Blogger blogger = null;
        try {
            blogger = userInfoService.fetchUserInfoByUsernameV2(username);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        if (blogger != null) {
            return ResponseEntity.ok(blogger);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

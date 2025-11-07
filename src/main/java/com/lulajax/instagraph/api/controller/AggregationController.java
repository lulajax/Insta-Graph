package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.api.service.AggregationService;
import com.lulajax.instagraph.model.Blogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/aggregate")
@Tag(name = "数据采集", description = "聚合获取各种用户数据")
public class AggregationController {

    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/{username}")
    @Operation(summary = "聚合获取用户所有相关信息", description = "获取并更新博主信息 -> 获取并存储用户的关注列表 -> 获取并存储用户的帖子列表 -> 获取并存储用户被标记的帖子")
    @Tag(name = "数据采集")
    public ResponseEntity<Blogger> aggregateUserData(@PathVariable String username) {
        Blogger blogger = aggregationService.aggregateUserData(username);
        return ResponseEntity.ok(blogger);
    }
}

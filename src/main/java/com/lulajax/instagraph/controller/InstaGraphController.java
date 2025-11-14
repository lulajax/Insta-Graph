package com.lulajax.instagraph.controller;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.BloggerStatusResponse;
import com.lulajax.instagraph.dto.EnhancedAnalysisResult;
import com.lulajax.instagraph.dto.PageResponse;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.service.InstaGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instagraph")
@Tag(name = "核心", description = "核心功能，管理博主和运行分析。")
public class InstaGraphController {

    private final InstaGraphService instaGraphService;

    public InstaGraphController(InstaGraphService instaGraphService) {
        this.instaGraphService = instaGraphService;
    }

    @GetMapping("/posts")
    @Operation(summary = "获取所有帖子", description = "返回数据库中所有帖子的列表。")
    @Tag(name = "核心")
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = instaGraphService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/bloggers")
    @Operation(summary = "获取所有博主", description = "返回数据库中所有博主的列表。")
    @Tag(name = "核心")
    public ResponseEntity<List<Blogger>> getAllBloggers() {
        List<Blogger> bloggers = instaGraphService.getAllBloggers();
        return ResponseEntity.ok(bloggers);
    }

    @GetMapping("/bloggers/page")
    @Operation(
        summary = "分页查询博主",
        description = "支持分页、搜索和筛选的博主查询接口。" +
                      "可通过用户名/全名搜索，可按分组筛选。"
    )
    @Tag(name = "核心")
    public ResponseEntity<PageResponse<Blogger>> getBloggersByPage(
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "搜索关键词（用户名或全名）", example = "dancer")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "分组筛选（为空表示不筛选，'__NO_GROUP__'表示未分组）", example = "busan_dancers")
            @RequestParam(required = false) String seedGroup,

            @Parameter(description = "是否放弃筛选（true 或 false）")
            @RequestParam(required = false) Boolean abandoned
    ) {
        PageResponse<Blogger> response = instaGraphService.getBloggersByPage(page, size, keyword, seedGroup, abandoned);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/blogger/{id}")
    @Operation(summary = "根据ID获取博主信息", description = "返回指定ID博主的详细信息及其关系。")
    @Tag(name = "核心")
    public ResponseEntity<Blogger> getBloggerById(@PathVariable String id) {
        Blogger blogger = instaGraphService.getBloggerById(id);
        return ResponseEntity.ok(blogger);
    }

    @PostMapping("/blogger")
    @Operation(summary = "更新博主", description = "更新现有博主节点。")
    @Tag(name = "核心")
    public ResponseEntity<Blogger> createOrUpdateBlogger(@RequestBody Blogger blogger) {
        Blogger savedBlogger = instaGraphService.addBlogger(blogger.getUsername(), blogger.getSeedGroup());
        return ResponseEntity.ok(savedBlogger);
    }

    @DeleteMapping("/blogger/{username}")
    @Operation(summary = "删除博主", description = "删除指定用户名的博主及其所有关系。")
    @Tag(name = "核心")
    public ResponseEntity<?> deleteBlogger(@PathVariable String username) {
        try {
            instaGraphService.deleteBlogger(username);
            return ResponseEntity.ok().body(java.util.Map.of("message", "博主 '" + username + "' 已删除"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analysis/common-follows")
    @Operation(summary = "共同关注分析", description = "查找被指定 `seed_group` 中的博主共同关注最多的“新博主”。")
    @Tag(name = "分析")
    public ResponseEntity<List<AnalysisResult>> getCommonFollows(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers") @RequestParam String project,
            @Parameter(description = "至少被 N 个种子博主关注的阈值") @RequestParam(defaultValue = "2") int min_follows) {
        List<AnalysisResult> results = instaGraphService.findCommonFollows(project, min_follows);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/analysis/co-tagged")
    @Operation(summary = "共同被标记分析", description = "查找与指定 seed_group 中的博主在同一篇帖子中被共同标记次数最多的新博主")
    @Tag(name = "分析")
    public ResponseEntity<List<AnalysisResult>> getCoTagged(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers") @RequestParam String project,
            @Parameter(description = "至少在 N 个不同的帖子里共同出现的阈值") @RequestParam(defaultValue = "2") int min_co_tags) {
        List<AnalysisResult> results = instaGraphService.findCoTagged(project, min_co_tags);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/analysis/co-tagged-enhanced")
    @Operation(
        summary = "增强的共同被标记分析",
        description = """
            使用双维度评分机制查找与种子博主最相关的新博主。

            **评分维度包括：**
            - **覆盖人数 (10分/人)**：与多少个不同的种子博主有连接（每个连接固定价值）
            - **共同标记次数 (5分/次)**：绝对的共同被标记次数（反映互动频率和强度）

            **评分公式：**
            综合评分 = 覆盖人数 × 10 + 共同标记次数 × 5

            **返回信息：**
            - `username`: 推荐博主用户名
            - `coTaggedCount`: 共同被标记的帖子数
            - `connectedSeeds`: 与多少个不同的种子博主有连接
            - `totalSeeds`: 该分组的总种子博主数量
            - `seedCoverage`: 种子博主覆盖率 (0.0-1.0)
            - `compositeScore`: 综合评分

            **使用建议：**
            - 小型种子组（<10人）：`min_coverage=0.2`（至少与20%的种子有连接）
            - 中型种子组（10-30人）：`min_coverage=0.15`（默认值）
            - 大型种子组（>30人）：`min_coverage=0.1`（降低要求）
            """
    )
    @Tag(name = "分析")
    public ResponseEntity<List<EnhancedAnalysisResult>> getCoTaggedEnhanced(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers")
            @RequestParam String project,

            @Parameter(description = "至少在 N 个不同的帖子里共同出现的阈值", example = "2")
            @RequestParam(defaultValue = "2") int min_co_tags,

            @Parameter(description = "最小种子博主覆盖率（0.0-1.0），例如 0.15 表示至少与 15% 的种子博主有连接", example = "0.15")
            @RequestParam(defaultValue = "0.15") double min_coverage) {

        List<EnhancedAnalysisResult> results = instaGraphService.findCoTaggedEnhanced(project, min_co_tags, min_coverage);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/blogger/{username}/abandon")
    @Operation(summary = "放弃博主", description = "将博主标记为已放弃状态，并记录放弃原因（可选）。已放弃的博主不会出现在智能分析结果中。")
    @Tag(name = "核心")
    public ResponseEntity<?> abandonBlogger(
            @Parameter(description = "博主用户名", required = true) @PathVariable String username,
            @Parameter(description = "放弃原因（可选）") @RequestParam(required = false) String reason) {
        try {
            Blogger blogger = instaGraphService.abandonBlogger(username, reason);
            return ResponseEntity.ok(java.util.Map.of(
                "message", "博主 '" + username + "' 已标记为放弃状态",
                "blogger", blogger
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/blogger/{username}/restore")
    @Operation(summary = "恢复博主", description = "恢复已放弃的博主，使其重新出现在智能分析结果中。")
    @Tag(name = "核心")
    public ResponseEntity<?> restoreBlogger(
            @Parameter(description = "博主用户名", required = true) @PathVariable String username) {
        try {
            Blogger blogger = instaGraphService.restoreBlogger(username);
            return ResponseEntity.ok(java.util.Map.of(
                "message", "博主 '" + username + "' 已恢复",
                "blogger", blogger
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blogger/{username}/status")
    @Operation(summary = "检查博主状态", description = "查询博主的存在状态和放弃状态，用于添加博主前的校验。")
    @Tag(name = "核心")
    public ResponseEntity<BloggerStatusResponse> getBloggerStatus(
            @Parameter(description = "博主用户名", required = true) @PathVariable String username) {
        Blogger blogger = instaGraphService.getBloggerById(username);
        
        if (blogger == null) {
            // 博主不存在
            BloggerStatusResponse response = new BloggerStatusResponse();
            response.setUsername(username);
            response.setExists(false);
            response.setAbandoned(false);
            return ResponseEntity.ok(response);
        }
        
        // 博主存在，返回详细状态
        BloggerStatusResponse response = new BloggerStatusResponse();
        response.setUsername(username);
        response.setExists(true);
        response.setAbandoned(Boolean.TRUE.equals(blogger.getAbandoned()));
        response.setAbandonedAt(blogger.getAbandonedAt());
        response.setAbandonedReason(blogger.getAbandonedReason());
        response.setSeedGroup(blogger.getSeedGroup());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analysis/connected-seeds")
    @Operation(summary = "获取连接的种子博主", description = "获取与指定博主有共同标记关系的种子博主列表")
    @Tag(name = "分析")
    public ResponseEntity<List<com.lulajax.instagraph.dto.ConnectedSeedInfo>> getConnectedSeeds(
            @Parameter(description = "博主用户名", required = true) @RequestParam String username,
            @Parameter(description = "种子分组", required = true) @RequestParam String project) {
        List<com.lulajax.instagraph.dto.ConnectedSeedInfo> seeds = instaGraphService.getConnectedSeeds(username, project);
        return ResponseEntity.ok(seeds);
    }

    @GetMapping("/analysis/co-tagged-posts")
    @Operation(summary = "获取共同标记的帖子", description = "获取博主与种子博主共同被标记的帖子列表")
    @Tag(name = "分析")
    public ResponseEntity<List<com.lulajax.instagraph.dto.CoTaggedPostInfo>> getCoTaggedPosts(
            @Parameter(description = "博主用户名", required = true) @RequestParam String username,
            @Parameter(description = "种子分组", required = true) @RequestParam String project) {
        List<com.lulajax.instagraph.dto.CoTaggedPostInfo> posts = instaGraphService.getCoTaggedPosts(username, project);
        return ResponseEntity.ok(posts);
    }
}

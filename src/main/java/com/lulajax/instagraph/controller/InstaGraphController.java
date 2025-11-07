package com.lulajax.instagraph.controller;

import com.lulajax.instagraph.dto.AnalysisResult;
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
    @Operation(summary = "共同被标记分析", description = "查找与指定 `seed_group` 中的博主在同一篇帖子中被共同标记次数最多的“新博主”。")
    @Tag(name = "分析")
    public ResponseEntity<List<AnalysisResult>> getCoTagged(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers") @RequestParam String project,
            @Parameter(description = "至少在 N 个不同的帖子里共同出现的阈值") @RequestParam(defaultValue = "2") int min_co_tags) {
        List<AnalysisResult> results = instaGraphService.findCoTagged(project, min_co_tags);
        return ResponseEntity.ok(results);
    }
}

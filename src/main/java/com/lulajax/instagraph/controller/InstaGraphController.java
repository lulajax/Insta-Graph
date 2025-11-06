package com.lulajax.instagraph.controller;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.BloggerRequest;
import com.lulajax.instagraph.dto.CoTagRequest;
import com.lulajax.instagraph.dto.FollowRequest;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.service.InstaGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Insta-Graph API", description = "用于 Instagram 社群分析的 API")
public class InstaGraphController {

    private final InstaGraphService instaGraphService;

    @PostMapping("/blogger")
    @Operation(summary = "创建或更新博主", description = "系统的入口。如果博主已存在，则更新其信息；如果不存在，则创建新节点。")
    @Tag(name = "数据录入")
    public ResponseEntity<Blogger> createOrUpdateBlogger(@RequestBody BloggerRequest request) {
        Blogger blogger = instaGraphService.createOrUpdateBlogger(request);
        return ResponseEntity.ok(blogger);
    }

    @PostMapping("/relationship/follow")
    @Operation(summary = "添加关注关系", description = "创建一个从博主到另一个博主的 `[:FOLLOWS]` 关系。")
    @Tag(name = "数据录入")
    public ResponseEntity<Void> addFollowRelationship(@RequestBody FollowRequest request) {
        instaGraphService.addFollowRelationship(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/relationship/co_tag")
    @Operation(summary = "添加共同标记关系 (核心)", description = "创建一个帖子节点，并将其与所有被标记的博主建立 `[:TAGGED_IN]` 关系。这是最高效的录入接口。")
    @Tag(name = "数据录入")
    public ResponseEntity<Void> addCoTagRelationship(@RequestBody CoTagRequest request) {
        instaGraphService.addCoTagRelationship(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analysis/common_follows")
    @Operation(summary = "共同关注分析", description = "查找被指定 `seed_group` 中的博主共同关注最多的“新博主”。")
    @Tag(name = "分析查询")
    public ResponseEntity<List<AnalysisResult>> getCommonFollows(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers") @RequestParam String project,
            @Parameter(description = "至少被 N 个种子博主关注的阈值") @RequestParam(defaultValue = "2") int min_follows) {
        List<AnalysisResult> results = instaGraphService.findCommonFollows(project, min_follows);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/analysis/co_tagged")
    @Operation(summary = "共同被标记分析 (核心)", description = "查找与指定 `seed_group` 中的博主在同一篇帖子中被共同标记次数最多的“新博主”。")
    @Tag(name = "分析查询")
    public ResponseEntity<List<AnalysisResult>> getCoTagged(
            @Parameter(description = "要分析的种子项目组", required = true, example = "busan_dancers") @RequestParam String project,
            @Parameter(description = "至少在 N 个不同的帖子里共同出现的阈值") @RequestParam(defaultValue = "2") int min_co_tags) {
        List<AnalysisResult> results = instaGraphService.findCoTagged(project, min_co_tags);
        return ResponseEntity.ok(results);
    }
}

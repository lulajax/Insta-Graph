package com.lulajax.instagraph.controller;

import com.lulajax.instagraph.dto.SeedGroupWithStats;
import com.lulajax.instagraph.model.SeedGroup;
import com.lulajax.instagraph.service.SeedGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instagraph/groups")
@Tag(name = "分组管理", description = "种子分组的增删改查")
public class SeedGroupController {

    private final SeedGroupService seedGroupService;

    public SeedGroupController(SeedGroupService seedGroupService) {
        this.seedGroupService = seedGroupService;
    }

    @PostMapping
    @Operation(summary = "创建新分组", description = "创建一个新的种子分组")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            SeedGroup group = seedGroupService.createGroup(
                request.getName(),
                request.getDescription()
            );
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "获取所有分组", description = "获取所有种子分组列表")
    public ResponseEntity<List<SeedGroup>> getAllGroups() {
        return ResponseEntity.ok(seedGroupService.getAllGroups());
    }

    @GetMapping("/with-stats")
    @Operation(summary = "获取所有分组及统计信息", description = "获取所有种子分组列表，包含每个分组下的博主数量")
    public ResponseEntity<List<SeedGroupWithStats>> getAllGroupsWithStats() {
        return ResponseEntity.ok(seedGroupService.getAllGroupsWithStats());
    }

    @GetMapping("/{name}")
    @Operation(summary = "获取分组详情", description = "根据名称获取分组详情")
    public ResponseEntity<?> getGroup(@PathVariable String name) {
        return seedGroupService.getGroupByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{name}/count")
    @Operation(summary = "获取分组博主数量", description = "获取指定分组下的博主数量")
    public ResponseEntity<Map<String, Long>> getGroupBloggerCount(@PathVariable String name) {
        long count = seedGroupService.countBloggers(name);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{name}")
    @Operation(summary = "更新分组", description = "更新分组描述")
    public ResponseEntity<?> updateGroup(
            @PathVariable String name,
            @RequestBody UpdateGroupRequest request
    ) {
        try {
            SeedGroup group = seedGroupService.updateGroup(name, request.getDescription());
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "删除分组", description = "删除分组（仅当分组下没有博主时）")
    public ResponseEntity<?> deleteGroup(@PathVariable String name) {
        try {
            seedGroupService.deleteGroup(name);
            return ResponseEntity.ok(Map.of("message", "分组 '" + name + "' 已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // DTO classes
    public static class CreateGroupRequest {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdateGroupRequest {
        private String description;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

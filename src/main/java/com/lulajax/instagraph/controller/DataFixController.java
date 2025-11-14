package com.lulajax.instagraph.controller;

import com.lulajax.instagraph.service.DataFixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/instagraph/admin/data-fix")
@Tag(name = "数据修复", description = "数据一致性检查和修复工具")
public class DataFixController {

    private final DataFixService dataFixService;

    public DataFixController(DataFixService dataFixService) {
        this.dataFixService = dataFixService;
    }

    @GetMapping("/check")
    @Operation(
        summary = "检查数据一致性", 
        description = "检查 seed_group 属性和 BELONGS_TO 关系的一致性"
    )
    public ResponseEntity<Map<String, Object>> checkConsistency() {
        Map<String, Object> report = dataFixService.checkDataConsistency();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/list-inconsistent")
    @Operation(
        summary = "列出所有不一致的博主",
        description = "详细列出所有属性和关系不一致的博主信息"
    )
    public ResponseEntity<Map<String, Object>> listInconsistent() {
        Map<String, Object> report = dataFixService.listInconsistentBloggers();
        return ResponseEntity.ok(report);
    }

    @PostMapping("/fix-relationships")
    @Operation(
        summary = "修复分组关系",
        description = "将所有博主的 seed_group 属性同步到 BELONGS_TO 关系"
    )
    public ResponseEntity<Map<String, Object>> fixRelationships() {
        Map<String, Object> result = dataFixService.fixBloggerGroupRelationships();
        return ResponseEntity.ok(result);
    }
}


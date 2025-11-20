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


    @PostMapping("/fix-relationships")
    @Operation(
        summary = "修复分组关系（综合修复）",
        description = "确保所有博主的 seed_group 属性和 BELONGS_TO 关系完全一致。以属性为准，自动同步关系。"
    )
    public ResponseEntity<Map<String, Object>> fixRelationships() {
        Map<String, Object> result = dataFixService.fixBloggerGroupRelationships();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/diagnose-group-mismatch")
    @Operation(
        summary = "诊断分组统计不一致问题",
        description = "比较通过 seed_group 属性和 BELONGS_TO 关系统计的博主数量，找出差异"
    )
    public ResponseEntity<Map<String, Object>> diagnoseGroupMismatch() {
        Map<String, Object> result = dataFixService.diagnoseGroupCountMismatch();
        return ResponseEntity.ok(result);
    }
}


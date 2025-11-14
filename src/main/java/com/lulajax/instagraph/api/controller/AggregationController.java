package com.lulajax.instagraph.api.controller;

import com.lulajax.instagraph.model.AggregationTask;
import com.lulajax.instagraph.service.AggregationTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/aggregate")
@Tag(name = "数据采集", description = "聚合获取各种用户数据")
public class AggregationController {

    private final AggregationTaskService taskService;

    public AggregationController(AggregationTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{username}")
    @Operation(
        summary = "聚合获取用户所有相关信息（加入任务队列）",
        description = "将用户数据采集任务加入队列。同一时间只能执行一个任务，其他任务会排队等待。" +
                      "如果该用户已在队列中或正在执行，将返回错误。" +
                      "采集流程：获取并更新博主信息 -> 获取并存储用户被标记的帖子"
    )
    @Tag(name = "数据采集")
    public ResponseEntity<?> aggregateUserData(@PathVariable String username) {
        AggregationTask task = taskService.addTask(username);

        if (task == null) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "error", "该用户的任务已在队列中或正在执行",
                    "username", username
                )
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "message", "任务已加入队列",
                "taskId", task.getId(),
                "username", task.getUsername(),
                "status", task.getStatus().toString(),
                "createdAt", task.getCreatedAt().toString()
            )
        );
    }

    @GetMapping("/queue/status")
    @Operation(
        summary = "获取任务队列状态",
        description = "返回当前正在执行的任务、待执行的任务列表和最近完成的任务历史"
    )
    @Tag(name = "数据采集")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        Map<String, Object> status = taskService.getQueueStatus();
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/queue/task/{taskId}")
    @Operation(
        summary = "取消待执行的任务",
        description = "从队列中移除指定的待执行任务。正在执行的任务无法取消。"
    )
    @Tag(name = "数据采集")
    public ResponseEntity<?> cancelTask(@PathVariable String taskId) {
        boolean cancelled = taskService.cancelPendingTask(taskId);

        if (cancelled) {
            return ResponseEntity.ok(
                Map.of(
                    "message", "任务已取消",
                    "taskId", taskId
                )
            );
        } else {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "error", "任务不存在或已在执行中",
                    "taskId", taskId
                )
            );
        }
    }

    @DeleteMapping("/queue/clear")
    @Operation(
        summary = "清空所有待执行的任务",
        description = "移除队列中所有待执行的任务。正在执行的任务不受影响。"
    )
    @Tag(name = "数据采集")
    public ResponseEntity<?> clearQueue() {
        taskService.clearPendingTasks();
        return ResponseEntity.ok(
            Map.of("message", "队列已清空")
        );
    }
}

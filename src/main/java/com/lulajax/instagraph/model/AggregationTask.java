package com.lulajax.instagraph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregationTask {
    private String id;
    private String username;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;

    public enum TaskStatus {
        PENDING,    // 等待中
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }

    public AggregationTask(String username) {
        this.id = java.util.UUID.randomUUID().toString();
        this.username = username;
        this.status = TaskStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}

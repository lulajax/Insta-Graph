package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 种子分组节点
 * 代表一个分析项目/社群分组
 */
@Node("SeedGroup")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeedGroup {

    @Id
    private String name;  // 分组名称，作为主键（例如：busan_dancers）

    @Property("description")
    private String description;  // 分组描述

    @Property("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;  // 创建时间

    @Property("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;  // 更新时间

    public SeedGroup(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SeedGroup(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}

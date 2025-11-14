package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "种子分组及其统计信息")
public class SeedGroupWithStats {
    @Schema(description = "分组名称", example = "busan_dancers")
    private String name;

    @Schema(description = "分组描述", example = "釜山地区的舞者")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "该分组下的博主数量", example = "15")
    private Long bloggerCount;
}

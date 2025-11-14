package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "增强的分析查询结果，包含双维度评分信息")
public class EnhancedAnalysisResult {
    @Schema(description = "被推荐的博主用户名", example = "new_dancer_X")
    private String username;

    @Schema(description = "共同被标记的帖子总数", example = "8")
    private Long coTaggedCount;

    @Schema(description = "与该推荐博主有连接的不同种子博主数量", example = "5")
    private Long connectedSeeds;

    @Schema(description = "该分组的总种子博主数量", example = "20")
    private Long totalSeeds;

    @Schema(description = "种子博主覆盖率（0.0-1.0）", example = "0.5")
    private Double seedCoverage;

    @Schema(description = "综合评分（基于双维度的加权评分）", example = "85.5")
    private Double compositeScore;
}

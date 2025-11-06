package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分析查询接口的返回结果")
public class AnalysisResult {
    @Schema(description = "被推荐的博主用户名", example = "new_dancer_X")
    private String username;
    @Schema(description = "共同关联的次数（共同关注或共同标记）", example = "5")
    private long count;
}

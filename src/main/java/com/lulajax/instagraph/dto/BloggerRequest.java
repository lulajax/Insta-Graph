package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建或更新博主的请求体")
public class BloggerRequest {
    @Schema(description = "Instagram 用户名", example = "dancer_A")
    private String username;
    @Schema(description = "种子项目组", example = "busan_dancers")
    private String seedGroup;
}

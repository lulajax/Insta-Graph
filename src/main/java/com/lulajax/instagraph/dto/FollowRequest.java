package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "添加关注关系的请求体")
public class FollowRequest {
    @Schema(description = "发起关注的博主用户名", example = "dancer_A")
    private String fromUsername;
    @Schema(description = "被关注的博主用户名", example = "dancer_B")
    private String toUsername;
}

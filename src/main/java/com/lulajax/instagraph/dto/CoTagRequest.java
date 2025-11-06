package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "添加共同标记关系的请求体")
public class CoTagRequest {
    @Schema(description = "帖子的唯一 ID", example = "CqXYZabc")
    private String postId;
    @Schema(description = "被标记的所有博主的用户名列表", example = "[\"dancer_A\", \"dancer_B\"]")
    private List<String> taggedUsernames;
    @Schema(description = "关于帖子的备注", example = "XX 舞蹈室 5 月考核视频")
    private String postNotes;
}

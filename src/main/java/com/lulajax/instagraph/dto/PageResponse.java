package com.lulajax.instagraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResponse<T> {
    @Schema(description = "当前页数据")
    private List<T> content;

    @Schema(description = "当前页码（从1开始）", example = "1")
    private int page;

    @Schema(description = "每页大小", example = "20")
    private int size;

    @Schema(description = "总记录数", example = "156")
    private long totalElements;

    @Schema(description = "总页数", example = "8")
    private int totalPages;

    @Schema(description = "是否是第一页", example = "true")
    private boolean first;

    @Schema(description = "是否是最后一页", example = "false")
    private boolean last;
}


package com.lulajax.instagraph.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private String shortcode;
    private String caption;
    private Long timestamp;
}

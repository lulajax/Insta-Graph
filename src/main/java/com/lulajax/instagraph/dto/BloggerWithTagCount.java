package com.lulajax.instagraph.dto;

import com.lulajax.instagraph.model.Blogger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloggerWithTagCount {
    private Blogger blogger;
    private long taggedPostCount;
}

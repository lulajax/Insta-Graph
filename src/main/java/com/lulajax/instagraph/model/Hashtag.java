package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

@Node("Hashtag")
@Data
public class Hashtag {

    @Id
    private final String name;
}

package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Node("Post")
@Data
public class Post {

    @Id
    private final String postId;

    @Property("notes")
    private String notes;

    @Property("latitude")
    private Float latitude;

    @Property("longitude")
    private Float longitude;

    @Relationship(type = "TAGGED_IN", direction = Relationship.Direction.OUTGOING)
    private Set<Blogger> taggedBloggers = new HashSet<>();

    @Relationship(type = "USES_HASHTAG", direction = Relationship.Direction.OUTGOING)
    private Set<Hashtag> hashtags = new HashSet<>();
}

package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Node("Blogger")
@Data
@AllArgsConstructor
public class Blogger {

    @Id
    private String username;

    @Property("seed_group")
    private String seedGroup;

    @Property("bio")
    private String bio;

    @Property("gender")
    private String gender;

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<Blogger> follows = new HashSet<>();

    public Blogger(String username, String seedGroup) {
        this.username = username;
        this.seedGroup = seedGroup;
    }
}

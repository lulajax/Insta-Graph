package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Node("Location")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {

    @Id
    private String id;

    @Property("name")
    private String name;

    @Property("slug")
    private String slug;

    @Property("address_json")
    private String addressJson;

    public Location(String id) {
        this.id = id;
    }
}

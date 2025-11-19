package com.lulajax.instagraph.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Node("ApiLog")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog {

    @Id @GeneratedValue
    private String id;

    @Property("service_name")
    private String serviceName;

    @Property("url")
    private String url;

    @Property("response")
    private String response;

    @Property("timestamp")
    private Long timestamp;
}


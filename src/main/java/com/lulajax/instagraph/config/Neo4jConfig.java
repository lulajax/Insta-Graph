package com.lulajax.instagraph.config;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Neo4jConfig {

    /**
     * 配置 Cypher-DSL 使用 Neo4j 5 方言，避免使用已弃用的 id() 函数
     * 这将使 Spring Data Neo4j 生成的查询使用 elementId() 而不是 id()
     */
    @Bean
    public Configuration cypherDslConfiguration() {
        return Configuration.newConfig()
                .withDialect(Dialect.NEO4J_5)
                .build();
    }
}


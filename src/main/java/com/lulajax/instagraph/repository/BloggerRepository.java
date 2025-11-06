package com.lulajax.instagraph.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.lulajax.instagraph.model.Blogger;

public interface BloggerRepository extends Neo4jRepository<Blogger, String> {
}

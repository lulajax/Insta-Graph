package com.lulajax.instagraph.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.lulajax.instagraph.model.Post;

public interface PostRepository extends Neo4jRepository<Post, String> {
}

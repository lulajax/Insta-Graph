package com.lulajax.instagraph.repository;

import com.lulajax.instagraph.model.ApiLog;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiLogRepository extends Neo4jRepository<ApiLog, String> {
}


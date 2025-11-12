package com.lulajax.instagraph.repository;

import com.lulajax.instagraph.model.SeedGroup;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SeedGroupRepository extends Neo4jRepository<SeedGroup, String> {

    /**
     * 检查分组下是否有博主
     */
    @Query("""
        MATCH (b:Blogger)-[:BELONGS_TO]->(g:SeedGroup {name: $groupName})
        RETURN COUNT(b) > 0
    """)
    boolean hasAnyBloggers(String groupName);

    /**
     * 获取分组下的博主数量
     */
    @Query("""
        MATCH (b:Blogger)-[:BELONGS_TO]->(g:SeedGroup {name: $groupName})
        RETURN COUNT(b)
    """)
    long countBloggers(String groupName);
}

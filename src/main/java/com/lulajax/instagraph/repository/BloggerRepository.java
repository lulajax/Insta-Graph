package com.lulajax.instagraph.repository;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.model.Blogger;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloggerRepository extends Neo4jRepository<Blogger, String> {
    Optional<Blogger> findByInstagramId(Long instagramId);

    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:FOLLOWS]->(rec:Blogger)
        WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
        WITH rec, COUNT(seed) AS common_follow_count
        WHERE common_follow_count >= $min_follows
        RETURN rec.username AS username, common_follow_count AS count
        ORDER BY common_follow_count DESC
        LIMIT 100
    """)
    List<AnalysisResult> findCommonFollows(String project, int min_follows);

    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:TAGGED_IN]->(post:Post)<-[:TAGGED_IN]-(rec:Blogger)
        WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
        WITH rec, COUNT(DISTINCT post) AS common_post_count
        WHERE common_post_count >= $min_co_tags
        RETURN rec.username AS username, common_post_count AS count
        ORDER BY common_post_count DESC
        LIMIT 100
    """)
    List<AnalysisResult> findCoTagged(String project, int min_co_tags);
}

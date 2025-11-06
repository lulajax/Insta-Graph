package com.lulajax.instagraph.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.BloggerRequest;
import com.lulajax.instagraph.dto.CoTagRequest;
import com.lulajax.instagraph.dto.FollowRequest;
import com.lulajax.instagraph.model.Blogger;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InstaGraphService {

    private final Neo4jClient neo4jClient;

    @Transactional
    public Blogger createOrUpdateBlogger(BloggerRequest request) {
        String cypher = "MERGE (b:Blogger {username: $username}) " +
                        "SET b.seed_group = $seed_group " +
                        "RETURN b";

        return neo4jClient.query(cypher)
                .bind(request.getUsername()).to("username")
                .bind(request.getSeedGroup()).to("seed_group")
                .fetchAs(Blogger.class)
                .mappedBy((typeSystem, record) -> {
                    var bloggerNode = record.get("b").asNode();
                    return new Blogger(
                        bloggerNode.get("username").asString(),
                        bloggerNode.get("seed_group").asString(null)
                    );
                })
                .one()
                .orElse(null);
    }

    @Transactional
    public void addFollowRelationship(FollowRequest request) {
        String cypher = "MERGE (a:Blogger {username: $from_username}) " +
                        "MERGE (b:Blogger {username: $to_username}) " +
                        "MERGE (a)-[:FOLLOWS]->(b)";

        neo4jClient.query(cypher)
                .bind(request.getFromUsername()).to("from_username")
                .bind(request.getToUsername()).to("to_username")
                .run();
    }

    @Transactional
    public void addCoTagRelationship(CoTagRequest request) {
        // 1. Create or merge the Post node
        String postCypher = "MERGE (p:Post {post_id: $post_id}) SET p.notes = $post_notes";
        neo4jClient.query(postCypher)
                .bind(request.getPostId()).to("post_id")
                .bind(request.getPostNotes()).to("post_notes")
                .run();

        // 2. Loop through tagged usernames and create relationships
        String bloggerAndRelCypher = "MATCH (p:Post {post_id: $post_id}) " +
                                     "MERGE (b:Blogger {username: $username}) " +
                                     "MERGE (p)-[:TAGGED_IN]->(b)";

        for (String username : request.getTaggedUsernames()) {
            neo4jClient.query(bloggerAndRelCypher)
                    .bind(request.getPostId()).to("post_id")
                    .bind(username).to("username")
                    .run();
        }
    }

    public List<AnalysisResult> findCommonFollows(String project, int minFollows) {
        String cypher = """
            MATCH (seed:Blogger {seed_group: $project})
            MATCH (seed)-[:FOLLOWS]->(rec:Blogger)
            WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
            WITH rec, COUNT(seed) AS common_follow_count
            WHERE common_follow_count >= $min_follows
            RETURN rec.username AS username, common_follow_count AS count
            ORDER BY common_follow_count DESC
            LIMIT 100
            """;

        return (List<AnalysisResult>) neo4jClient.query(cypher)
                .bind(project).to("project")
                .bind(minFollows).to("min_follows")
                .fetchAs(AnalysisResult.class)
                .mappedBy((typeSystem, record) -> new AnalysisResult(
                        record.get("username").asString(),
                        record.get("count").asLong()
                ))
                .all();
    }

    public List<AnalysisResult> findCoTagged(String project, int minCoTags) {
        String cypher = """
            MATCH (seed:Blogger {seed_group: $project})
            MATCH (seed)<-[:TAGGED_IN]-(post:Post)
            MATCH (post)-[:TAGGED_IN]->(rec:Blogger)
            WHERE rec.seed_group IS NULL OR rec.seed_group <> $project
            WITH rec, COUNT(DISTINCT post) AS common_post_count
            WHERE common_post_count >= $min_co_tags
            RETURN rec.username AS username, common_post_count AS count
            ORDER BY common_post_count DESC
            LIMIT 100
            """;

        return (List<AnalysisResult>) neo4jClient.query(cypher)
                .bind(project).to("project")
                .bind(minCoTags).to("min_co_tags")
                .fetchAs(AnalysisResult.class)
                .mappedBy((typeSystem, record) -> new AnalysisResult(
                        record.get("username").asString(),
                        record.get("count").asLong()
                ))
                .all();
    }
}

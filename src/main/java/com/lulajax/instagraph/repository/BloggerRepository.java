package com.lulajax.instagraph.repository;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.EnhancedAnalysisResult;
import com.lulajax.instagraph.model.Blogger;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloggerRepository extends Neo4jRepository<Blogger, String> {
    Optional<Blogger> findByInstagramId(Long instagramId);

    /**
     * 动态分页查询博主（支持可选的关键词搜索和分组筛选）
     * @param keyword 搜索关键词（用户名或全名），为null或空字符串时不进行关键词筛选
     * @param seedGroup 种子分组名称，为null时不进行分组筛选，"__NO_GROUP__"表示查询未分组的博主
     * @param skip 跳过的记录数
     * @param limit 返回的最大记录数
     * @return 博主列表
     */
    @Query("""
        MATCH (b:Blogger)
        WHERE
            // 关键词搜索条件（如果提供了keyword）
            CASE
                WHEN $keyword IS NULL OR $keyword = '' THEN true
                ELSE toLower(b.username) CONTAINS toLower($keyword)
                     OR toLower(b.full_name) CONTAINS toLower($keyword)
            END
            // 分组筛选条件（如果提供了seedGroup）
            AND CASE
                WHEN $seedGroup IS NULL OR $seedGroup = '' THEN true
                WHEN $seedGroup = '__NO_GROUP__' THEN b.seed_group IS NULL
                ELSE b.seed_group = $seedGroup
            END
            // 放弃状态筛选条件
            AND CASE
                // 如果参数为 true, 查询已放弃的
                WHEN $abandoned = true THEN b.abandoned = true
                // 如果参数为 false, 查询活跃的 (abandoned 为 false 或 null)
                WHEN $abandoned = false THEN (b.abandoned IS NULL OR b.abandoned = false)
                // 否则 (参数为 null), 不进行筛选
                ELSE true
            END
        RETURN b
        ORDER BY b.username
        SKIP $skip LIMIT $limit
    """)
    List<Blogger> findByFiltersWithPagination(@Param("keyword") String keyword,
                                               @Param("seedGroup") String seedGroup,
                                               @Param("abandoned") Boolean abandoned,
                                               @Param("skip") long skip,
                                               @Param("limit") int limit);

    /**
     * 统计符合条件的博主总数
     */
    @Query("""
        MATCH (b:Blogger)
        WHERE
            // 关键词搜索条件（如果提供了keyword）
            CASE
                WHEN $keyword IS NULL OR $keyword = '' THEN true
                ELSE toLower(b.username) CONTAINS toLower($keyword)
                     OR toLower(b.full_name) CONTAINS toLower($keyword)
            END
            // 分组筛选条件（如果提供了seedGroup）
            AND CASE
                WHEN $seedGroup IS NULL OR $seedGroup = '' THEN true
                WHEN $seedGroup = '__NO_GROUP__' THEN b.seed_group IS NULL
                ELSE b.seed_group = $seedGroup
            END
            // 放弃状态筛选条件
            AND CASE
                // 如果参数为 true, 查询已放弃的
                WHEN $abandoned = true THEN b.abandoned = true
                // 如果参数为 false, 查询活跃的 (abandoned 为 false 或 null)
                WHEN $abandoned = false THEN (b.abandoned IS NULL OR b.abandoned = false)
                // 否则 (参数为 null), 不进行筛选
                ELSE true
            END
        RETURN count(b)
    """)
    long countByFilters(@Param("keyword") String keyword,
                        @Param("seedGroup") String seedGroup,
                        @Param("abandoned") Boolean abandoned);

    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:FOLLOWS]->(rec:Blogger)
        WHERE (rec.seed_group IS NULL OR rec.seed_group <> $project)
          AND (rec.abandoned IS NULL OR rec.abandoned = false)
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
        WHERE (rec.seed_group IS NULL OR rec.seed_group <> $project)
          AND (rec.abandoned IS NULL OR rec.abandoned = false)
        WITH rec, COUNT(DISTINCT post) AS common_post_count
        WHERE common_post_count >= $min_co_tags
        RETURN rec.username AS username, common_post_count AS count
        ORDER BY common_post_count DESC
        LIMIT 100
    """)
    List<AnalysisResult> findCoTagged(String project, int min_co_tags);

    @Query("""
        // 第一步：计算总的种子博主数量
        MATCH (seed:Blogger {seed_group: $project})
        WITH COUNT(seed) AS total_seeds

        // 第二步：找到所有与种子博主共同被标记的推荐博主
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:TAGGED_IN]->(post:Post)<-[:TAGGED_IN]-(rec:Blogger)
        WHERE (rec.seed_group IS NULL OR rec.seed_group <> $project)
          AND rec <> seed
          AND (rec.abandoned IS NULL OR rec.abandoned = false)
          AND (rec.isPrivate IS NULL OR rec.isPrivate = false)

        // 第三步：汇总共同标记的帖子和连接的种子
        WITH rec, total_seeds,
             COLLECT(DISTINCT post) AS shared_posts,
             COLLECT(DISTINCT seed) AS connected_seed_list

        // 第四步：计算各项指标
        WITH rec,
             SIZE(shared_posts) AS co_tagged_count,
             SIZE(connected_seed_list) AS connected_seeds,
             total_seeds,
             // 覆盖率：与多少比例的seed博主有连接
             toFloat(SIZE(connected_seed_list)) / total_seeds AS seed_coverage

        // 第五步：计算综合评分（双维度评分）
        WITH rec,
             co_tagged_count,
             connected_seeds,
             total_seeds,
             seed_coverage,
             // 综合评分公式（双维度）：
             // 1. 覆盖人数 × 10：与多少个种子博主有连接（核心指标）
             //    例如：与12个种子有连接 → 12 × 10 = 120分
             // 2. 共同标记次数 × 5：绝对的共同被标记次数
             //    例如：共同被标记8次 → 8 × 5 = 40分
             (toFloat(connected_seeds) * 10.0) +
             (toFloat(co_tagged_count) * 5.0)
             AS composite_score

        WHERE co_tagged_count >= $min_co_tags
          AND seed_coverage >= $min_coverage

        RETURN rec.username AS username,
               co_tagged_count AS coTaggedCount,
               connected_seeds AS connectedSeeds,
               total_seeds AS totalSeeds,
               seed_coverage AS seedCoverage,
               composite_score AS compositeScore
        ORDER BY composite_score DESC
        LIMIT 100
    """)
    List<EnhancedAnalysisResult> findCoTaggedEnhanced(String project, int min_co_tags, double min_coverage);

    /**
     * 获取与指定博主有连接的种子博主列表
     */
    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:TAGGED_IN]->(post:Post)<-[:TAGGED_IN]-(rec:Blogger {username: $username})
        WITH seed, COUNT(DISTINCT post) AS coTagCount
        RETURN seed.username AS username, coTagCount AS coTagCount
        ORDER BY coTagCount DESC
    """)
    List<com.lulajax.instagraph.dto.ConnectedSeedInfo> findConnectedSeeds(@Param("username") String username, @Param("project") String project);

    /**
     * 获取博主与种子博主共同被标记的帖子列表
     */
    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:TAGGED_IN]->(post:Post)<-[:TAGGED_IN]-(rec:Blogger {username: $username})
        WITH post, COLLECT(DISTINCT seed.username) AS taggedSeeds
        RETURN post.shortcode AS shortCode, post.taken_at AS takenAt, taggedSeeds AS taggedSeeds
        ORDER BY post.id DESC
    """)
    List<com.lulajax.instagraph.dto.CoTaggedPostInfo> findCoTaggedPosts(@Param("username") String username, @Param("project") String project);

    /**
     * 删除指定博主的所有 BELONGS_TO 关系（同时清除 seed_group 属性）
     */
    @Query("""
        MATCH (b:Blogger {username: $username})
        OPTIONAL MATCH (b)-[r:BELONGS_TO]->(:SeedGroup)
        DELETE r
        SET b.seed_group = null
    """)
    void deleteBelongsToRelationship(@Param("username") String username);

    /**
     * 为指定博主创建 BELONGS_TO 关系到指定分组（同时更新属性）
     * 如果分组不存在，先创建分组节点
     */
    @Query("""
        MATCH (b:Blogger {username: $username})
        SET b.seed_group = $groupName
        WITH b
        MERGE (g:SeedGroup {name: $groupName})
        MERGE (b)-[:BELONGS_TO]->(g)
    """)
    void createBelongsToRelationship(@Param("username") String username, @Param("groupName") String groupName);

    /**
     * 直接修复博主的分组关系（删除旧关系并创建新关系，同时更新属性）
     */
    @Query("""
        MATCH (b:Blogger {username: $username})
        OPTIONAL MATCH (b)-[oldRel:BELONGS_TO]->(:SeedGroup)
        DELETE oldRel
        WITH b
        SET b.seed_group = $groupName
        WITH b
        MERGE (g:SeedGroup {name: $groupName})
        MERGE (b)-[:BELONGS_TO]->(g)
    """)
    void fixBelongsToRelationship(@Param("username") String username, @Param("groupName") String groupName);
}

package com.lulajax.instagraph.repository;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.BloggerDto;
import com.lulajax.instagraph.dto.BloggerWithTagCount;
import com.lulajax.instagraph.dto.EnhancedAnalysisResult;
import com.lulajax.instagraph.dto.ConnectedSeedInfo;
import com.lulajax.instagraph.dto.PostDTO;
import com.lulajax.instagraph.model.Blogger;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface BloggerRepository extends Neo4jRepository<Blogger, String> {
    @Query("MATCH (b:Blogger {username: $username}) RETURN " +
           "b.username AS username, " +
           "b.seed_group AS seedGroup, " +
           "b.seed_reason AS seedReason, " +
           "b.bio AS bio, " +
           "b.gender AS gender, " +
           "b.instagram_id AS instagramId, " +
           "b.country AS country, " +
           "b.date_joined AS dateJoined, " +
           "b.date_joined_as_timestamp AS dateJoinedAsTimestamp, " +
           "b.date_verified AS dateVerified, " +
           "b.date_verified_as_timestamp AS dateVerifiedAsTimestamp, " +
           "b.former_usernames AS formerUsernames, " +
           "b.is_verified AS isVerified, " +
           "b.is_private AS isPrivate, " +
           "b.full_name AS fullName, " +
           "b.abandoned AS abandoned, " +
           "b.abandoned_at AS abandonedAt, " +
           "b.abandoned_reason AS abandonedReason")
    Optional<BloggerDto> findProjectedByUsername(@Param("username") String username);

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
    List<ConnectedSeedInfo> findConnectedSeeds(@Param("username") String username, @Param("project") String project);

    /**
     * 获取博主与种子博主共同被标记的帖子列表
     */
    @Query("""
        MATCH (seed:Blogger {seed_group: $project})
        MATCH (seed)-[:TAGGED_IN]->(post:Post)<-[:TAGGED_IN]-(rec:Blogger {username: $username})
        // 新增步骤：找到所有被标记在该帖子中的用户
        MATCH (taggedUser:Blogger)-[:TAGGED_IN]->(post)
        WITH post, COLLECT(DISTINCT seed.username) AS taggedSeeds, COLLECT(DISTINCT taggedUser.username) AS allTaggedUsers
        RETURN post.shortcode AS shortCode, post.taken_at AS takenAt, taggedSeeds AS taggedSeeds, allTaggedUsers as allTaggedUsers
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
     * 注意：会先删除已有的分组关系，确保一对一
     */
    @Query("""
        MATCH (b:Blogger {username: $username})
        OPTIONAL MATCH (b)-[r:BELONGS_TO]->(:SeedGroup)
        DELETE r
        WITH b
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

    /**
     * 优化的添加或更新博主方法（一次性完成所有操作，避免多次数据库往返）
     * 只返回基本字段，不加载关系，显著提升性能
     */
    @Query("""
        // 1. 创建或匹配博主节点
        MERGE (b:Blogger {username: $username})

        // 2. 如果博主已被放弃，自动恢复
        SET b.abandoned = CASE WHEN b.abandoned = true THEN false ELSE b.abandoned END,
            b.abandonedAt = CASE WHEN b.abandoned = true THEN null ELSE b.abandonedAt END,
            b.abandonedReason = CASE WHEN b.abandoned = true THEN null ELSE b.abandonedReason END

        // 更新 seed_reason (如果提供了，非null)
        SET b.seed_reason = CASE WHEN $seedReason IS NOT NULL THEN $seedReason ELSE b.seed_reason END

        // 3. 处理分组关系（删除旧关系）
        WITH b
        OPTIONAL MATCH (b)-[oldRel:BELONGS_TO]->(:SeedGroup)
        DELETE oldRel

        // 4. 使用 FOREACH 实现条件更新
        WITH b
        FOREACH (_ IN CASE WHEN $seedGroup IS NOT NULL AND $seedGroup <> '' THEN [1] ELSE [] END |
            SET b.seed_group = $seedGroup
        )
        FOREACH (_ IN CASE WHEN $seedGroup IS NULL OR $seedGroup = '' THEN [1] ELSE [] END |
            SET b.seed_group = null
        )

        // 5. 建立新的 BELONGS_TO 关系（如果提供了有效的 seedGroup）
        WITH b
        FOREACH (_ IN CASE WHEN $seedGroup IS NOT NULL AND $seedGroup <> '' THEN [1] ELSE [] END |
            MERGE (g:SeedGroup {name: $seedGroup})
            MERGE (b)-[:BELONGS_TO]->(g)
        )

        // 6. 返回博主基本信息（不加载关系）
        RETURN b
    """)
    Optional<Blogger> addOrUpdateBloggerOptimized(@Param("username") String username, @Param("seedGroup") String seedGroup, @Param("seedReason") String seedReason);

    /**
     * 优化的 getOrCreateBloggerByInstagramId，增强了对改名情况的处理
     * 1. 优先根据 instagramId 匹配，如果匹配到但用户名不同，会自动更新用户名（并删除旧用户名的占位节点）
     * 2. 如果没有 instagramId 或没匹配到，退回使用 username 匹配
     */
    @Query("""
        // 0. 预处理：查找可能存在的冲突节点
        OPTIONAL MATCH (b_by_id:Blogger)
        WHERE $instagramId IS NOT NULL AND b_by_id.instagram_id = $instagramId

        OPTIONAL MATCH (b_by_name:Blogger {username: $username})

        // 1. 解决冲突：如果 ID 匹配和 Name 匹配指向不同节点，删除 Name 匹配的那个（视为过时的或占位的）
        FOREACH (_ IN CASE WHEN b_by_id IS NOT NULL AND b_by_name IS NOT NULL AND b_by_id <> b_by_name THEN [1] ELSE [] END |
            DETACH DELETE b_by_name
        )

        // 2. 准备主节点：如果有 ID 匹配的（现在可能改名了），更新它的名字以便后续 MERGE 能够命中它
        FOREACH (_ IN CASE WHEN b_by_id IS NOT NULL THEN [1] ELSE [] END |
            SET b_by_id.username = $username
        )

        // 3. 标准 MERGE 流程 - 创建节点时记录是否新建
        MERGE (b:Blogger {username: $username})
        ON CREATE SET b.instagram_id = $instagramId

        // 总是尝试设置 instagramId (如果原来是 null)
        SET b.instagram_id = COALESCE(b.instagram_id, $instagramId)

        WITH b
        // 如果传入了 seedGroup 且不为空，则更新 seed_group 并建立 BELONGS_TO 关系
        // 注意：这里不再检查 b.seed_group IS NULL，允许覆盖现有分组
        FOREACH (_ IN CASE WHEN $seedGroup IS NOT NULL AND $seedGroup <> '' THEN [1] ELSE [] END |
            SET b.seed_group = $seedGroup
        )

        // 建立分组关系（仅当 seedGroup 有值时才处理关系：删除旧的，建立新的）
        // 使用 OPTIONAL MATCH + FOREACH 技巧来实现条件删除，避免 UNWIND 导致的流中断
        OPTIONAL MATCH (b)-[oldRel:BELONGS_TO]->(:SeedGroup)
        FOREACH (_ IN CASE WHEN $seedGroup IS NOT NULL AND $seedGroup <> '' AND oldRel IS NOT NULL THEN [1] ELSE [] END |
            DELETE oldRel
        )

        WITH b
        FOREACH (_ IN CASE WHEN $seedGroup IS NOT NULL AND $seedGroup <> '' THEN [1] ELSE [] END |
            MERGE (g:SeedGroup {name: $seedGroup})
            MERGE (b)-[:BELONGS_TO]->(g)
        )

        RETURN b
    """)
    Optional<Blogger> getOrCreateBloggerOptimized(@Param("instagramId") Long instagramId, 
                                                  @Param("username") String username, 
                                                  @Param("seedGroup") String seedGroup);

    /**
     * 动态分页查询博主，并返回每个博主被标记的帖子数量
     */
    @Query("""
        MATCH (b:Blogger)
        WHERE
            CASE
                WHEN $keyword IS NULL OR $keyword = '' THEN true
                ELSE toLower(b.username) CONTAINS toLower($keyword)
                     OR toLower(b.full_name) CONTAINS toLower($keyword)
            END
            AND CASE
                WHEN $seedGroup IS NULL OR $seedGroup = '' THEN true
                WHEN $seedGroup = '__NO_GROUP__' THEN b.seed_group IS NULL
                ELSE b.seed_group = $seedGroup
            END
            AND CASE
                WHEN $abandoned = true THEN b.abandoned = true
                WHEN $abandoned = false THEN (b.abandoned IS NULL OR b.abandoned = false)
                ELSE true
            END

        // 使用 OPTIONAL MATCH 以包含没有被标记在任何帖子中的博主
        OPTIONAL MATCH (b)-[:TAGGED_IN]->(p:Post)
        WITH b, COUNT(p) AS taggedPostCount

        RETURN b AS blogger, taggedPostCount
        ORDER BY b.username
        SKIP $skip LIMIT $limit
    """)
    List<BloggerWithTagCount> findByFiltersWithPaginationAndTagCount(
            @Param("keyword") String keyword,
            @Param("seedGroup") String seedGroup,
            @Param("abandoned") Boolean abandoned,
            @Param("skip") long skip,
            @Param("limit") int limit);
    
    /**
     * 根据用户名查找其所有被标记的帖子
     */
    @Query("MATCH (b:Blogger {username: $username})-[:TAGGED_IN]->(p:Post) RETURN p.shortcode AS shortcode, p.caption AS caption, p.timestamp AS timestamp ORDER BY p.timestamp DESC")
    List<PostDTO> findTaggedPostsByUsername(@Param("username") String username);

    @Query("MATCH (follower:Blogger {username: $follower}), (followed:Blogger {username: $followed}) MERGE (follower)-[:FOLLOWS]->(followed)")
    void createFollowRelationship(@Param("follower") String follower, @Param("followed") String followed);

    @Query("MATCH (blogger:Blogger {username: $username}), (post:Post {id: $postId}) MERGE (blogger)-[:POSTED]->(post)")
    void createPostedRelationship(@Param("username") String username, @Param("postId") String postId);

    @Query("MATCH (blogger:Blogger {username: $username}), (post:Post {id: $postId}) MERGE (blogger)-[:LIKED]->(post)")
    void createLikedRelationship(@Param("username") String username, @Param("postId") String postId);

    @Query("MATCH (blogger:Blogger {username: $username}), (post:Post {id: $postId}) MERGE (blogger)-[:TAGGED_IN]->(post)")
    void createTaggedInRelationship(@Param("username") String username, @Param("postId") String postId);

    /**
     * 修复所有缺失的 BELONGS_TO 关系
     * 为所有有 seed_group 属性但没有 BELONGS_TO 关系的博主建立关系
     * @return 修复的博主数量
     */
    @Query("""
        MATCH (b:Blogger)
        WHERE b.seed_group IS NOT NULL AND b.seed_group <> ''
        AND NOT EXISTS((b)-[:BELONGS_TO]->(:SeedGroup))
        WITH b
        MERGE (g:SeedGroup {name: b.seed_group})
        MERGE (b)-[:BELONGS_TO]->(g)
        RETURN count(b) AS fixedCount
    """)
    long fixMissingBelongsToRelationships();

    /**
     * 修复属性为空或不一致的情况
     * 从 BELONGS_TO 关系同步到 seed_group 属性
     * @return 修复的博主数量
     */
    @Query("""
        MATCH (b:Blogger)-[:BELONGS_TO]->(g:SeedGroup)
        WHERE b.seed_group IS NULL OR b.seed_group = '' OR b.seed_group <> g.name
        SET b.seed_group = g.name
        RETURN count(b) AS fixedCount
    """)
    long syncPropertyFromRelationship();

    /**
     * 修复关系不一致的情况
     * 当属性和关系都存在但不一致时，以属性为准，更新关系
     * @return 修复的博主数量
     */
    @Query("""
        MATCH (b:Blogger)-[r:BELONGS_TO]->(g:SeedGroup)
        WHERE b.seed_group IS NOT NULL AND b.seed_group <> '' AND b.seed_group <> g.name
        DELETE r
        WITH b
        MERGE (newG:SeedGroup {name: b.seed_group})
        MERGE (b)-[:BELONGS_TO]->(newG)
        RETURN count(b) AS fixedCount
    """)
    long fixInconsistentRelationships();

    /**
     * 综合修复所有不一致（一次性完成所有同步）
     * 1. 先处理有属性但关系不正确的（删除旧关系，创建新关系）
     * 2. 再处理有关系但属性不正确的（同步属性）
     * 3. 最后处理只有属性没有关系的（创建关系）
     * @return 修复统计
     */
    @Query("""
        // 第1步：处理属性和关系都存在但不一致的（以属性为准）
        MATCH (b:Blogger)-[r:BELONGS_TO]->(g:SeedGroup)
        WHERE b.seed_group IS NOT NULL AND b.seed_group <> '' AND b.seed_group <> g.name
        DELETE r
        WITH count(b) AS inconsistentFixed

        // 第2步：为所有有属性的博主确保有正确的关系
        MATCH (b:Blogger)
        WHERE b.seed_group IS NOT NULL AND b.seed_group <> ''
        MERGE (g:SeedGroup {name: b.seed_group})
        MERGE (b)-[:BELONGS_TO]->(g)
        WITH inconsistentFixed, count(b) AS relationshipEnsured

        // 第3步：同步有关系但属性为空或不一致的
        MATCH (b:Blogger)-[:BELONGS_TO]->(g:SeedGroup)
        WHERE b.seed_group IS NULL OR b.seed_group = '' OR b.seed_group <> g.name
        SET b.seed_group = g.name
        WITH inconsistentFixed, relationshipEnsured, count(b) AS propertySynced

        RETURN inconsistentFixed, relationshipEnsured, propertySynced
    """)
    Map<String, Object> fixAllInconsistencies();

    /**
     * 诊断：比较属性统计和关系统计的差异
     * 返回每个分组通过属性和关系统计的博主数量
     * 仅返回有差异的分组
     */
    @Query("""
        // 1. 获取所有可能的组名（来自 SeedGroup 节点 和 Blogger 属性）
        MATCH (g:SeedGroup) WITH COLLECT(DISTINCT g.name) AS knownGroups
        MATCH (b:Blogger) WHERE b.seed_group IS NOT NULL AND b.seed_group <> '' 
        WITH knownGroups, COLLECT(DISTINCT b.seed_group) AS bloggerGroups
        WITH knownGroups + bloggerGroups AS allGroups
        UNWIND allGroups AS groupName
        WITH DISTINCT groupName
        
        // 2. 统计
        // 统计属性 (seed_group = groupName)
        OPTIONAL MATCH (bProp:Blogger) WHERE bProp.seed_group = groupName
        WITH groupName, count(bProp) AS propertyCount
        
        // 统计关系 (BELONGS_TO -> groupName)
        OPTIONAL MATCH (bRel:Blogger)-[:BELONGS_TO]->(g:SeedGroup {name: groupName})
        WITH groupName, propertyCount, count(bRel) AS relationshipCount
        
        // 3. 计算差异
        WITH groupName, propertyCount, relationshipCount, (propertyCount - relationshipCount) AS difference
        WHERE difference <> 0
        RETURN groupName, propertyCount, relationshipCount, difference
        ORDER BY abs(difference) DESC, groupName
    """)
    List<Map<String, Object>> diagnoseGroupCountMismatch();
}

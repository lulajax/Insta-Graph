package com.lulajax.instagraph.service;

import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.SeedGroup;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.SeedGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataFixService {

    private final BloggerRepository bloggerRepository;
    private final SeedGroupRepository seedGroupRepository;

    public DataFixService(BloggerRepository bloggerRepository, 
                         SeedGroupRepository seedGroupRepository) {
        this.bloggerRepository = bloggerRepository;
        this.seedGroupRepository = seedGroupRepository;
    }

    /**
     * 修复所有博主的分组关系（智能双向同步）
     * 优先级：如果属性和关系都存在但不一致，以属性为准
     * 使用原生 Cypher 查询确保关系正确更新
     */
    @Transactional("transactionManager")
    public Map<String, Object> fixBloggerGroupRelationships() {
        log.info("开始修复博主分组关系（使用原生 Cypher）...");

        Map<String, Object> result = new HashMap<>();
        int fixedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        int syncedFromRelationship = 0;
        int syncedFromProperty = 0;
        Map<String, Integer> groupStats = new HashMap<>();

        try {
            List<Blogger> allBloggers = bloggerRepository.findAll();
            log.info("找到 {} 个博主", allBloggers.size());

            for (Blogger blogger : allBloggers) {
                try {
                    String seedGroup = blogger.getSeedGroup();
                    SeedGroup belongsToGroup = blogger.getBelongsToGroup();

                    boolean hasProperty = seedGroup != null && !seedGroup.trim().isEmpty();
                    boolean hasRelationship = belongsToGroup != null;

                    // 情况1：属性和关系都存在且一致 - 跳过
                    if (hasProperty && hasRelationship &&
                        seedGroup.equals(belongsToGroup.getName())) {
                        skippedCount++;
                        continue;
                    }

                    // 情况2：属性和关系都存在但不一致 - 以属性为准，使用 Cypher 更新关系
                    if (hasProperty && hasRelationship &&
                        !seedGroup.equals(belongsToGroup.getName())) {
                        log.info("博主 {} 属性({})和关系({})不一致，使用 Cypher 修复",
                                blogger.getUsername(), seedGroup, belongsToGroup.getName());

                        // 使用 Cypher 直接修复关系
                        bloggerRepository.fixBelongsToRelationship(blogger.getUsername(), seedGroup);

                        fixedCount++;
                        syncedFromProperty++;
                        groupStats.put(seedGroup, groupStats.getOrDefault(seedGroup, 0) + 1);
                        continue;
                    }

                    // 情况3：只有属性，没有关系 - 使用 Cypher 建立关系
                    if (hasProperty && !hasRelationship) {
                        log.debug("博主 {} 只有属性({})，使用 Cypher 建立关系", blogger.getUsername(), seedGroup);

                        bloggerRepository.createBelongsToRelationship(blogger.getUsername(), seedGroup);

                        fixedCount++;
                        syncedFromProperty++;
                        groupStats.put(seedGroup, groupStats.getOrDefault(seedGroup, 0) + 1);
                        continue;
                    }

                    // 情况4：只有关系，没有属性 - 同步属性
                    if (!hasProperty && hasRelationship) {
                        String groupName = belongsToGroup.getName();
                        blogger.setSeedGroup(groupName);
                        bloggerRepository.save(blogger);
                        fixedCount++;
                        syncedFromRelationship++;
                        groupStats.put(groupName, groupStats.getOrDefault(groupName, 0) + 1);
                        log.debug("博主 {} 同步分组属性: {}", blogger.getUsername(), groupName);
                        continue;
                    }

                    // 情况5：既没有属性也没有关系 - 跳过
                    if (!hasProperty && !hasRelationship) {
                        skippedCount++;
                        continue;
                    }

                    if (fixedCount % 100 == 0 && fixedCount > 0) {
                        log.info("已修复 {} 个博主...", fixedCount);
                    }

                } catch (Exception e) {
                    errorCount++;
                    log.error("修复博主 {} 失败: {}", blogger.getUsername(), e.getMessage());
                }
            }

            log.info("修复完成！总计: {}, 修复: {}, 跳过: {}, 错误: {}",
                    allBloggers.size(), fixedCount, skippedCount, errorCount);
            log.info("  - 从属性同步到关系: {}", syncedFromProperty);
            log.info("  - 从关系同步到属性: {}", syncedFromRelationship);

            result.put("success", true);
            result.put("total", allBloggers.size());
            result.put("fixed", fixedCount);
            result.put("skipped", skippedCount);
            result.put("errors", errorCount);
            result.put("syncedFromProperty", syncedFromProperty);
            result.put("syncedFromRelationship", syncedFromRelationship);
            result.put("groupStats", groupStats);

        } catch (Exception e) {
            log.error("修复过程发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("fixed", fixedCount);
            result.put("skipped", skippedCount);
            result.put("errors", errorCount);
        }

        return result;
    }

    /**
     * 列出所有不一致的博主详情
     */
    public Map<String, Object> listInconsistentBloggers() {
        Map<String, Object> report = new HashMap<>();
        List<Map<String, String>> inconsistentList = new java.util.ArrayList<>();

        try {
            List<Blogger> allBloggers = bloggerRepository.findAll();

            for (Blogger blogger : allBloggers) {
                String seedGroup = blogger.getSeedGroup();
                SeedGroup belongsTo = blogger.getBelongsToGroup();

                boolean hasProperty = seedGroup != null && !seedGroup.trim().isEmpty();
                boolean hasRelationship = belongsTo != null;

                // 只有属性，没有关系
                if (hasProperty && !hasRelationship) {
                    Map<String, String> item = new HashMap<>();
                    item.put("username", blogger.getUsername());
                    item.put("property", seedGroup);
                    item.put("relationship", "NULL");
                    item.put("issue", "缺少 BELONGS_TO 关系");
                    inconsistentList.add(item);
                }
                // 只有关系，没有属性
                else if (!hasProperty && hasRelationship) {
                    Map<String, String> item = new HashMap<>();
                    item.put("username", blogger.getUsername());
                    item.put("property", "NULL");
                    item.put("relationship", belongsTo.getName());
                    item.put("issue", "缺少 seed_group 属性");
                    inconsistentList.add(item);
                }
                // 属性和关系都存在但不一致
                else if (hasProperty && hasRelationship && !seedGroup.equals(belongsTo.getName())) {
                    Map<String, String> item = new HashMap<>();
                    item.put("username", blogger.getUsername());
                    item.put("property", seedGroup);
                    item.put("relationship", belongsTo.getName());
                    item.put("issue", "属性和关系不匹配");
                    inconsistentList.add(item);
                }
            }

            report.put("totalInconsistent", inconsistentList.size());
            report.put("inconsistentBloggers", inconsistentList);

        } catch (Exception e) {
            log.error("检查不一致博主失败", e);
            report.put("error", e.getMessage());
        }

        return report;
    }

    /**
     * 检查数据一致性
     */
    public Map<String, Object> checkDataConsistency() {
        Map<String, Object> report = new HashMap<>();

        try {
            List<Blogger> allBloggers = bloggerRepository.findAll();

            int totalBloggers = allBloggers.size();
            int withPropertyOnly = 0;
            int withRelationshipOnly = 0;
            int withBoth = 0;
            int withNeither = 0;
            int inconsistent = 0;

            Map<String, Integer> propertyGroups = new HashMap<>();
            Map<String, Integer> relationshipGroups = new HashMap<>();

            for (Blogger blogger : allBloggers) {
                String seedGroup = blogger.getSeedGroup();
                SeedGroup belongsTo = blogger.getBelongsToGroup();

                boolean hasProperty = seedGroup != null && !seedGroup.trim().isEmpty();
                boolean hasRelationship = belongsTo != null;

                if (hasProperty && hasRelationship) {
                    withBoth++;
                    propertyGroups.put(seedGroup, propertyGroups.getOrDefault(seedGroup, 0) + 1);
                    relationshipGroups.put(belongsTo.getName(), relationshipGroups.getOrDefault(belongsTo.getName(), 0) + 1);

                    // 检查是否一致
                    if (!seedGroup.equals(belongsTo.getName())) {
                        inconsistent++;
                    }
                } else if (hasProperty) {
                    withPropertyOnly++;
                    propertyGroups.put(seedGroup, propertyGroups.getOrDefault(seedGroup, 0) + 1);
                } else if (hasRelationship) {
                    withRelationshipOnly++;
                    relationshipGroups.put(belongsTo.getName(), relationshipGroups.getOrDefault(belongsTo.getName(), 0) + 1);
                } else {
                    withNeither++;
                }
            }

            report.put("totalBloggers", totalBloggers);
            report.put("withPropertyOnly", withPropertyOnly);
            report.put("withRelationshipOnly", withRelationshipOnly);
            report.put("withBoth", withBoth);
            report.put("withNeither", withNeither);
            report.put("inconsistent", inconsistent);
            report.put("propertyGroups", propertyGroups);
            report.put("relationshipGroups", relationshipGroups);
            report.put("needsFix", withPropertyOnly > 0 || withRelationshipOnly > 0 || inconsistent > 0);

        } catch (Exception e) {
            report.put("error", e.getMessage());
        }

        return report;
    }
}


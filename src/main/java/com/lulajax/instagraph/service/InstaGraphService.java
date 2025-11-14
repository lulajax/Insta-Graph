package com.lulajax.instagraph.service;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.dto.EnhancedAnalysisResult;
import com.lulajax.instagraph.dto.PageResponse;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.model.SeedGroup;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.PostRepository;
import com.lulajax.instagraph.repository.SeedGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstaGraphService {

    private final BloggerRepository bloggerRepository;
    private final PostRepository postRepository;
    private final SeedGroupRepository seedGroupRepository;

    public InstaGraphService(BloggerRepository bloggerRepository,
                            PostRepository postRepository,
                            SeedGroupRepository seedGroupRepository) {
        this.bloggerRepository = bloggerRepository;
        this.postRepository = postRepository;
        this.seedGroupRepository = seedGroupRepository;
    }

    @Transactional("transactionManager")
    public Blogger addBlogger(String username, String seedGroup) {
        // 先查找是否已存在该博主
        Blogger blogger = bloggerRepository.findById(username)
                .orElse(null);

        // 如果博主不存在，先创建基础节点
        if (blogger == null) {
            blogger = new Blogger(username, seedGroup);
            blogger = bloggerRepository.save(blogger);
        }

        // 如果博主已被放弃，自动恢复
        if (Boolean.TRUE.equals(blogger.getAbandoned())) {
            blogger.setAbandoned(false);
            blogger.setAbandonedAt(null);
            blogger.setAbandonedReason(null);
            blogger = bloggerRepository.save(blogger);
        }

        // 使用 Cypher 更新分组属性和关系（更可靠）
        if (seedGroup != null && !seedGroup.isEmpty()) {
            // 确保分组存在
            seedGroupRepository.findById(seedGroup)
                    .orElseGet(() -> {
                        SeedGroup newGroup = new SeedGroup(seedGroup);
                        return seedGroupRepository.save(newGroup);
                    });
            
            // 使用 Cypher 修复关系（删除旧关系、更新属性、创建新关系）
            bloggerRepository.fixBelongsToRelationship(username, seedGroup);
        } else {
            // 如果分组为空，删除关系
            bloggerRepository.deleteBelongsToRelationship(username);
        }

        // 重新加载以获取最新的完整数据
        return bloggerRepository.findById(username).orElse(blogger);
    }

    public List<AnalysisResult> findCommonFollows(String project, int minFollows) {
        return bloggerRepository.findCommonFollows(project, minFollows);
    }

    public List<AnalysisResult> findCoTagged(String project, int minCoTags) {
        return bloggerRepository.findCoTagged(project, minCoTags);
    }

    /**
     * 增强的 co-tagged 分析，返回多维度评分
     * @param project 种子项目组名称
     * @param minCoTags 最少共同标记次数阈值
     * @param minCoverage 最小种子博主覆盖率（0.0-1.0），例如 0.15 表示至少与 15% 的种子博主有连接
     * @return 增强的分析结果列表，按综合评分降序排列
     */
    public List<EnhancedAnalysisResult> findCoTaggedEnhanced(String project, int minCoTags, double minCoverage) {
        return bloggerRepository.findCoTaggedEnhanced(project, minCoTags, minCoverage);
    }

    public List<Blogger> getAllBloggers() {
        return bloggerRepository.findAll();
    }

    /**
     * 分页查询博主（支持搜索和筛选）
     */
    public PageResponse<Blogger> getBloggersByPage(int page, int size, String keyword, String seedGroup) {
        // 计算跳过的记录数（页码从1开始）
        long skip = (long) (page - 1) * size;

        // 查询当前页的数据
        List<Blogger> content = bloggerRepository.findByFiltersWithPagination(keyword, seedGroup, skip, size);

        // 查询总记录数
        long totalElements = bloggerRepository.countByFilters(keyword, seedGroup);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // 判断是否为首页和末页
        boolean isFirst = page == 1;
        boolean isLast = page >= totalPages;

        // 返回自定义分页响应
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            isFirst,
            isLast
        );
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Blogger getBloggerById(String id) {
        // Using username as the ID in this context
        return bloggerRepository.findById(id).orElse(null);
    }

    @Transactional("transactionManager")
    public void deleteBlogger(String username) {
        bloggerRepository.deleteById(username);
    }

    /**
     * 放弃博主
     * @param username 博主用户名
     * @param reason 放弃原因（可选）
     * @return 更新后的博主
     */
    @Transactional("transactionManager")
    public Blogger abandonBlogger(String username, String reason) {
        Blogger blogger = bloggerRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("博主不存在: " + username));

        blogger.setAbandoned(true);
        blogger.setAbandonedAt(System.currentTimeMillis());
        blogger.setAbandonedReason(reason);

        return bloggerRepository.save(blogger);
    }

    /**
     * 恢复已放弃的博主
     * @param username 博主用户名
     * @return 更新后的博主
     */
    @Transactional("transactionManager")
    public Blogger restoreBlogger(String username) {
        Blogger blogger = bloggerRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("博主不存在: " + username));

        blogger.setAbandoned(false);
        blogger.setAbandonedAt(null);
        blogger.setAbandonedReason(null);

        return bloggerRepository.save(blogger);
    }

    /**
     * 检查博主状态
     * @param username 博主用户名
     * @return 博主是否被放弃
     */
    public boolean isBloggerAbandoned(String username) {
        return bloggerRepository.findById(username)
                .map(blogger -> Boolean.TRUE.equals(blogger.getAbandoned()))
                .orElse(false);
    }
}

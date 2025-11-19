package com.lulajax.instagraph.service;

import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.repository.BloggerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 博主创建和更新的统一服务
 * 确保所有博主创建都会正确建立 BELONGS_TO 关系
 */
@Service
public class BloggerService {

    private final BloggerRepository bloggerRepository;

    public BloggerService(BloggerRepository bloggerRepository) {
        this.bloggerRepository = bloggerRepository;
    }

    /**
     * 获取或创建博主，确保正确建立分组关系
     * @param username 用户名
     * @return 博主对象
     */
    @Transactional("transactionManager")
    public Blogger getOrCreateBlogger(String username) {
        // 使用优化后的 Cypher 查询，默认分组为 "default"
        return bloggerRepository.getOrCreateBloggerOptimized(null, username, "default")
                .orElseThrow(() -> new RuntimeException("无法处理博主: " + username));
    }

    /**
     * 通过 Instagram ID 获取或创建博主
     */
    @Transactional("transactionManager")
    public Blogger getOrCreateBloggerByInstagramId(Long instagramId, String username, String seedGroup) {
        // 使用优化后的 Cypher 查询一次性完成查找、创建和关系更新
        return bloggerRepository.getOrCreateBloggerOptimized(instagramId, username, seedGroup)
                .orElseThrow(() -> new RuntimeException("无法处理博主: " + username));
    }
}


package com.lulajax.instagraph.service;

import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.SeedGroup;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.SeedGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 博主创建和更新的统一服务
 * 确保所有博主创建都会正确建立 BELONGS_TO 关系
 */
@Service
public class BloggerService {

    private final BloggerRepository bloggerRepository;
    private final SeedGroupRepository seedGroupRepository;

    public BloggerService(BloggerRepository bloggerRepository, 
                         SeedGroupRepository seedGroupRepository) {
        this.bloggerRepository = bloggerRepository;
        this.seedGroupRepository = seedGroupRepository;
    }

    /**
     * 获取或创建博主，确保正确建立分组关系
     * @param username 用户名
     * @param seedGroup 分组名称（可以为null）
     * @return 博主对象
     */
    @Transactional("transactionManager")
    public Blogger getOrCreateBlogger(String username) {
        Blogger blogger = bloggerRepository.findById(username)
                .orElse(new Blogger(username, "default"));

        // 如果是新博主或者未设置分组，设置默认分组
        if (blogger.getSeedGroup() == null) {
            blogger.setSeedGroup("default");
        }

        return bloggerRepository.save(blogger);
    }

    /**
     * 通过 Instagram ID 获取或创建博主
     */
    @Transactional("transactionManager")
    public Blogger getOrCreateBloggerByInstagramId(Long instagramId, String username, String seedGroup) {
        return bloggerRepository.findByInstagramId(instagramId)
                .map(blogger -> {
                    // 如果博主存在但分组为null，更新分组
                    if (blogger.getSeedGroup() == null && seedGroup != null) {
                        blogger.setSeedGroup(seedGroup);
                        updateBelongsToRelationship(blogger, seedGroup);
                        return bloggerRepository.save(blogger);
                    }
                    return blogger;
                })
                .orElseGet(() -> {
                    Blogger newBlogger = new Blogger(username, seedGroup);
                    newBlogger.setInstagramId(instagramId);
                    updateBelongsToRelationship(newBlogger, seedGroup);
                    return bloggerRepository.save(newBlogger);
                });
    }

    /**
     * 更新博主的 BELONGS_TO 关系
     */
    private void updateBelongsToRelationship(Blogger blogger, String seedGroup) {
        if (seedGroup != null && !seedGroup.trim().isEmpty()) {
            // 查找或创建分组
            SeedGroup group = seedGroupRepository.findById(seedGroup)
                    .orElseGet(() -> {
                        SeedGroup newGroup = new SeedGroup(seedGroup);
                        return seedGroupRepository.save(newGroup);
                    });
            blogger.setBelongsToGroup(group);
        } else {
            // 如果分组为空，移除关系
            blogger.setBelongsToGroup(null);
        }
    }
}


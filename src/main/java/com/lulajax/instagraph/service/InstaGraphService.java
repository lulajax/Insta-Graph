package com.lulajax.instagraph.service;

import com.lulajax.instagraph.dto.AnalysisResult;
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
                .orElse(new Blogger(username, seedGroup));

        // 更新分组属性
        blogger.setSeedGroup(seedGroup);

        // 如果指定了分组，建立关系
        if (seedGroup != null && !seedGroup.isEmpty()) {
            SeedGroup group = seedGroupRepository.findById(seedGroup)
                    .orElseGet(() -> {
                        // 如果分组不存在，自动创建
                        SeedGroup newGroup = new SeedGroup(seedGroup);
                        return seedGroupRepository.save(newGroup);
                    });
            blogger.setBelongsToGroup(group);
        } else {
            // 如果分组为空，移除关系
            blogger.setBelongsToGroup(null);
        }

        return bloggerRepository.save(blogger);
    }

    public List<AnalysisResult> findCommonFollows(String project, int minFollows) {
        return bloggerRepository.findCommonFollows(project, minFollows);
    }

    public List<AnalysisResult> findCoTagged(String project, int minCoTags) {
        return bloggerRepository.findCoTagged(project, minCoTags);
    }

    public List<Blogger> getAllBloggers() {
        return bloggerRepository.findAll();
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
}

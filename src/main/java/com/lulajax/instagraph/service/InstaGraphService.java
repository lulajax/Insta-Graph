package com.lulajax.instagraph.service;

import com.lulajax.instagraph.dto.AnalysisResult;
import com.lulajax.instagraph.model.Blogger;
import com.lulajax.instagraph.model.Post;
import com.lulajax.instagraph.repository.BloggerRepository;
import com.lulajax.instagraph.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstaGraphService {

    private final BloggerRepository bloggerRepository;
    private final PostRepository postRepository;

    public InstaGraphService(BloggerRepository bloggerRepository, PostRepository postRepository) {
        this.bloggerRepository = bloggerRepository;
        this.postRepository = postRepository;
    }

    public Blogger addBlogger(String username, String seedGroup) {
        Blogger blogger = new Blogger(username, seedGroup);
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
}

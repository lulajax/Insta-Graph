package com.lulajax.instagraph.service;

import com.lulajax.instagraph.model.SeedGroup;
import com.lulajax.instagraph.repository.SeedGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SeedGroupService {

    private final SeedGroupRepository seedGroupRepository;

    public SeedGroupService(SeedGroupRepository seedGroupRepository) {
        this.seedGroupRepository = seedGroupRepository;
    }

    /**
     * 创建新分组
     */
    @Transactional("transactionManager")
    public SeedGroup createGroup(String name, String description) {
        // 检查是否已存在
        Optional<SeedGroup> existing = seedGroupRepository.findById(name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("分组 '" + name + "' 已存在");
        }

        SeedGroup group = new SeedGroup(name, description);
        return seedGroupRepository.save(group);
    }

    /**
     * 获取所有分组
     */
    public List<SeedGroup> getAllGroups() {
        return seedGroupRepository.findAll();
    }

    /**
     * 根据名称获取分组
     */
    public Optional<SeedGroup> getGroupByName(String name) {
        return seedGroupRepository.findById(name);
    }

    /**
     * 更新分组信息
     */
    @Transactional("transactionManager")
    public SeedGroup updateGroup(String name, String description) {
        SeedGroup group = seedGroupRepository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("分组 '" + name + "' 不存在"));

        group.setDescription(description);
        group.setUpdatedAt(LocalDateTime.now());

        return seedGroupRepository.save(group);
    }

    /**
     * 删除分组（仅当没有博主时）
     */
    @Transactional("transactionManager")
    public void deleteGroup(String name) {
        SeedGroup group = seedGroupRepository.findById(name)
                .orElseThrow(() -> new IllegalArgumentException("分组 '" + name + "' 不存在"));

        // 检查是否有博主
        if (seedGroupRepository.hasAnyBloggers(name)) {
            long count = seedGroupRepository.countBloggers(name);
            throw new IllegalStateException(
                "无法删除分组 '" + name + "'，该分组下还有 " + count + " 个博主"
            );
        }

        seedGroupRepository.delete(group);
    }

    /**
     * 获取分组下的博主数量
     */
    public long countBloggers(String groupName) {
        return seedGroupRepository.countBloggers(groupName);
    }
}

package com.lulajax.instagraph.service;

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
    // private final SeedGroupRepository seedGroupRepository; // Currently unused

    public DataFixService(BloggerRepository bloggerRepository, 
                         SeedGroupRepository seedGroupRepository) {
        this.bloggerRepository = bloggerRepository;
        // this.seedGroupRepository = seedGroupRepository;
    }

    /**
     * 修复所有博主的分组关系（综合版本 - 确保属性和关系完全一致）
     * 优先级：以 seed_group 属性为准，seed_group为空 使用关系更新seed_group
     * 使用优化的 Cypher 查询确保关系正确更新
     */
    @Transactional("transactionManager")
    public Map<String, Object> fixBloggerGroupRelationships() {
        log.info("开始修复博主分组关系（综合修复）...");
        try {
            Map<String, Object> result = bloggerRepository.fixAllInconsistencies();
            log.info("修复完成: {}", result);
            return result;
        } catch (Exception e) {
            log.error("修复博主分组关系时发生错误", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("status", "FAILED");
            return errorResult;
        }
    }

    /**
     * 诊断分组统计不一致问题
     * 比较通过 seed_group 属性和 BELONGS_TO 关系统计的博主数量
     * @return 诊断报告
     */
    public Map<String, Object> diagnoseGroupCountMismatch() {
        log.info("开始诊断分组统计不一致问题...");
        List<Map<String, Object>> discrepancies = bloggerRepository.diagnoseGroupCountMismatch();
        
        Map<String, Object> report = new HashMap<>();
        report.put("discrepancies", discrepancies);
        report.put("totalDiscrepantGroups", discrepancies.size());
        
        if (discrepancies.isEmpty()) {
            log.info("未发现分组统计不一致问题。");
            report.put("status", "OK");
            report.put("message", "所有分组的属性统计与关系统计完全一致");
        } else {
            log.warn("发现 {} 个分组存在统计不一致。", discrepancies.size());
            report.put("status", "MISMATCH_FOUND");
            report.put("message", "发现分组统计不一致，请查看 discrepancies 字段");
        }
        
        return report;
    }
}


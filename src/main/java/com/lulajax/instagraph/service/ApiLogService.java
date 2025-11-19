package com.lulajax.instagraph.service;

import com.lulajax.instagraph.model.ApiLog;
import com.lulajax.instagraph.repository.ApiLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ApiLogService {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogService.class);

    private final ApiLogRepository apiLogRepository;

    public ApiLogService(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }

    /**
     * 异步保存API响应日志
     *
     * @param serviceName 服务名称
     * @param url         请求URL
     * @param response    响应内容
     */
    @Async
    public void saveLog(String serviceName, String url, String response) {
        try {
            ApiLog log = new ApiLog();
            log.setServiceName(serviceName);
            log.setUrl(url);
            log.setResponse(response);
            log.setTimestamp(System.currentTimeMillis());
            apiLogRepository.save(log);
            logger.debug("已保存API日志: {} -> {}", serviceName, url);
        } catch (Exception e) {
            logger.error("保存API日志失败: {}", e.getMessage(), e);
        }
    }
}


package com.lulajax.instagraph.service;

import com.lulajax.instagraph.api.service.AggregationService;
import com.lulajax.instagraph.model.AggregationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聚合任务队列服务
 * 确保同时只有一个任务在运行，其他任务排队等待
 */
@Service
@Slf4j
public class AggregationTaskService {

    private final AggregationService aggregationService;

    // 任务队列：等待执行的任务
    private final Queue<AggregationTask> taskQueue = new ConcurrentLinkedQueue<>();

    // 用于去重：记录已在队列中的用户名
    private final Set<String> usernamesInQueue = ConcurrentHashMap.newKeySet();

    // 当前正在运行的任务
    private final AtomicReference<AggregationTask> runningTask = new AtomicReference<>(null);

    // 已完成的任务历史（最多保留50条）
    private final Queue<AggregationTask> completedTasks = new ConcurrentLinkedQueue<>();
    private static final int MAX_COMPLETED_HISTORY = 50;

    // 后台任务执行器
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AggregationTaskExecutor");
        thread.setDaemon(true);
        return thread;
    });

    public AggregationTaskService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
        // 启动后台任务处理线程
        startTaskProcessor();
    }

    /**
     * 添加新任务到队列
     * @param username Instagram 用户名
     * @return 创建的任务对象，如果用户名已在队列中则返回 null
     */
    public AggregationTask addTask(String username) {
        // 检查是否已在运行
        AggregationTask running = runningTask.get();
        if (running != null && running.getUsername().equals(username)) {
            log.warn("用户 {} 的任务正在执行中，无法重复添加", username);
            return null;
        }

        // 检查是否已在队列中（去重）
        if (!usernamesInQueue.add(username)) {
            log.warn("用户 {} 的任务已在队列中，无法重复添加", username);
            return null;
        }

        // 创建新任务
        AggregationTask task = new AggregationTask(username);
        taskQueue.offer(task);

        log.info("任务已加入队列：{}, 当前队列长度：{}", username, taskQueue.size());
        return task;
    }

    /**
     * 取消待执行的任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelPendingTask(String taskId) {
        Iterator<AggregationTask> iterator = taskQueue.iterator();
        while (iterator.hasNext()) {
            AggregationTask task = iterator.next();
            if (task.getId().equals(taskId)) {
                iterator.remove();
                usernamesInQueue.remove(task.getUsername());

                // 标记为失败
                task.setStatus(AggregationTask.TaskStatus.FAILED);
                task.setCompletedAt(LocalDateTime.now());
                task.setErrorMessage("任务被用户取消");
                addToCompletedHistory(task);

                log.info("任务已取消：{}", task.getUsername());
                return true;
            }
        }
        log.warn("未找到待取消的任务：{}", taskId);
        return false;
    }

    /**
     * 获取队列状态
     * @return 包含当前运行任务、队列任务和历史任务的状态对象
     */
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();

        // 当前运行的任务
        AggregationTask running = runningTask.get();
        status.put("runningTask", running);

        // 待执行的任务列表
        List<AggregationTask> pending = new ArrayList<>(taskQueue);
        status.put("pendingTasks", pending);
        status.put("pendingCount", pending.size());

        // 最近完成的任务
        List<AggregationTask> completed = new ArrayList<>(completedTasks);
        status.put("completedTasks", completed);

        return status;
    }

    /**
     * 启动后台任务处理线程
     */
    private void startTaskProcessor() {
        executor.submit(() -> {
            log.info("任务队列处理器已启动");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    processNextTask();
                    Thread.sleep(1000); // 每秒检查一次队列
                } catch (InterruptedException e) {
                    log.info("任务队列处理器被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("处理任务时发生错误", e);
                }
            }
        });
    }

    /**
     * 处理队列中的下一个任务
     */
    private void processNextTask() throws InterruptedException {
        // 如果有任务正在运行，跳过
        if (runningTask.get() != null) {
            return;
        }

        // 从队列中取出下一个任务
        AggregationTask task = taskQueue.poll();
        if (task == null) {
            return;
        }

        // 从去重集合中移除
        usernamesInQueue.remove(task.getUsername());

        // 标记为运行中
        task.setStatus(AggregationTask.TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        runningTask.set(task);

        log.info("开始执行任务：{}", task.getUsername());

        try {
            // 执行聚合操作
            aggregationService.aggregateUserData(task.getUsername());

            // 标记为完成
            task.setStatus(AggregationTask.TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());

            log.info("任务执行成功：{}", task.getUsername());
        } catch (Exception e) {
            // 标记为失败
            task.setStatus(AggregationTask.TaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            log.error("任务执行失败：{}, 错误：{}", task.getUsername(), e.getMessage(), e);
        } finally {
            // 清除运行中的任务引用
            runningTask.set(null);

            // 添加到完成历史
            addToCompletedHistory(task);
        }
    }

    /**
     * 添加任务到完成历史（保留最近的50条）
     */
    private void addToCompletedHistory(AggregationTask task) {
        completedTasks.offer(task);

        // 保持历史记录数量在限制内
        while (completedTasks.size() > MAX_COMPLETED_HISTORY) {
            completedTasks.poll();
        }
    }

    /**
     * 清空所有待执行的任务
     */
    public void clearPendingTasks() {
        int clearedCount = taskQueue.size();

        // 标记所有待执行任务为已取消
        taskQueue.forEach(task -> {
            task.setStatus(AggregationTask.TaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorMessage("队列已清空");
            addToCompletedHistory(task);
        });

        taskQueue.clear();
        usernamesInQueue.clear();

        log.info("已清空 {} 个待执行任务", clearedCount);
    }
}

// 全局状态
const state = {
    currentProject: null,
    seedBloggers: [],
    allProjects: new Set(),
    projectStats: {},  // 存储分组统计信息
    queueRefreshInterval: null,  // 队列刷新定时器
    // 分页相关（后端分页）
    currentPage: 1,
    pageSize: 20,
    // 当前分析结果（用于导出）
    currentAnalysisResults: [],
    currentAnalysisType: null
};

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    loadAllProjects();
    loadAllBloggers();
    // 初始加载队列状态以显示badge
    loadQueueStatus();
    // 启动队列状态定时刷新（全局，每5秒刷新一次badge）
    setInterval(() => {
        loadQueueStatus();
    }, 5000);
});

// 标签页切换
function switchTab(tabName, project = null) {
    // 移除所有active状态
    document.querySelectorAll('.nav-tab').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    // 设置对应的tab和content为active
    const tabs = document.querySelectorAll('.nav-tab');
    const tabNames = ['workflow', 'projects', 'analysis', 'queue', 'data'];
    const targetIndex = tabNames.indexOf(tabName);
    if (targetIndex !== -1 && tabs[targetIndex]) {
        tabs[targetIndex].classList.add('active');
    }

    const tabContent = document.getElementById(tabName + '-tab');
    if (tabContent) {
        tabContent.classList.add('active');
    }

    // 特定标签页的初始化逻辑
    if (tabName === 'analysis') {
        populateAnalysisProjects();
        // 如果传入了项目参数或当前有选中的项目，自动选中
        if (project || state.currentProject) {
            const projectToSelect = state.currentProject;
            if (projectToSelect) {
                setTimeout(() => {
                    document.getElementById('analysis-project').value = projectToSelect;
                }, 100);
            }
        }
    } else if (tabName === 'projects') {
        renderProjectsList();
    } else if (tabName === 'queue') {
        // 加载队列状态
        loadQueueStatus();
        // 启动自动刷新（每3秒刷新一次）
        if (state.queueRefreshInterval) {
            clearInterval(state.queueRefreshInterval);
        }
        state.queueRefreshInterval = setInterval(loadQueueStatus, 3000);
    } else if (tabName === 'data') {
        updateFilterGroupOptions();
    }

    // 离开任务队列标签页时，停止自动刷新
    if (tabName !== 'queue' && state.queueRefreshInterval) {
        clearInterval(state.queueRefreshInterval);
        state.queueRefreshInterval = null;
    }
}

// 工作流程步骤切换
function activateStep(stepNumber) {
    if (stepNumber === 2 && !state.currentProject) {
        showToast('请先选择或创建一个分组！', 'warning');
        return;
    }

    // 更新步骤状态
    for (let i = 1; i <= 3; i++) {
        const stepEl = document.getElementById('step-' + i);
        const contentEl = document.getElementById('step-content-' + i);

        stepEl.classList.remove('active', 'completed');
        contentEl.style.display = 'none';

        if (i < stepNumber) {
            stepEl.classList.add('completed');
        } else if (i === stepNumber) {
            stepEl.classList.add('active');
            contentEl.style.display = 'block';
        }
    }
}

// 创建分组
async function createProject() {
    const projectName = document.getElementById('new-project-name').value.trim();
    if (!projectName) {
        showError('请输入分组名称！');
        return;
    }

    try {
        console.log('正在创建分组:', projectName);

        // 调用后端API创建分组节点
        const response = await fetch('/api/instagraph/groups', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: projectName,
                description: ''
            })
        });

        console.log('响应状态:', response.status);

        if (!response.ok) {
            const contentType = response.headers.get('content-type');
            let errorMessage = '创建失败';

            if (contentType && contentType.includes('application/json')) {
                const error = await response.json();
                errorMessage = error.error || error.message || '创建失败';
                console.error('错误详情:', error);
            } else {
                const text = await response.text();
                errorMessage = text || '创建失败';
                console.error('错误响应:', text);
            }

            throw new Error(errorMessage);
        }

        const result = await response.json();
        console.log('创建成功:', result);

        state.currentProject = projectName;
        state.allProjects.add(projectName);
        state.seedBloggers = [];
        // 初始化新分组的统计信息
        state.projectStats[projectName] = { bloggerCount: 0 };

        updateProjectSelects();
        document.getElementById('new-project-name').value = '';

        showSuccess('分组 "' + projectName + '" 创建成功！');
        renderProjectsList();  // 更新分组列表显示
        activateStep(2);
    } catch (error) {
        console.error('创建分组失败:', error);
        showError('创建分组失败：' + error.message);
    }
}

// 批量添加种子博主
async function bulkAddSeedBloggers() {
    const list = document.getElementById('seed-username-list').value.trim();
    if (!list) {
        showError('请输入要添加的用户名列表！');
        return;
    }

    if (!state.currentProject) {
        showToast('请先选择分组！', 'warning');
        return;
    }

    const usernames = list.split('\n').map(u => u.trim()).filter(u => u);
    const total = usernames.length;

    const confirmed = await showConfirm(
        `确定要将 ${total} 个用户添加为种子博主吗？<br><br>如果列表中包含已被放弃的博主，系统将自动恢复它们。`,
        '批量添加确认',
        '🌱'
    );

    if (!confirmed) {
        return;
    }

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < total; i++) {
        const username = usernames[i];
        try {
            showToast(`(${i + 1}/${total}) 正在处理 @${username}...`, 'warning');
            
            // 检查状态，如果已放弃则自动恢复
            const status = await checkBloggerStatus(username);
            if (status && status.exists && status.abandoned) {
                const restoreResponse = await fetch(`/api/instagraph/blogger/${username}/restore`, {
                    method: 'PUT'
                });
                if (!restoreResponse.ok) throw new Error('恢复失败');
            }

            // 添加为种子
            const addResponse = await fetch('/api/instagraph/blogger', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: username,
                    seedGroup: state.currentProject
                })
            });

            if (!addResponse.ok) throw new Error('添加失败');

            successCount++;

            // 自动采集数据（不等待，并行触发）
            aggregateUserData(username).catch(aggError => {
                console.error(`自动采集 @${username} 数据失败:`, aggError);
            });

        } catch (error) {
            failCount++;
            showError(`添加 @${username} 失败: ${error.message}`);
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    showSuccess(`批量添加完成！成功 ${successCount} 个，失败 ${failCount} 个。`);
    document.getElementById('seed-username-list').value = '';

    // 刷新分组统计和UI
    await loadAllProjects();
    await loadProjectSeeds();
    // 刷新任务队列状态以更新角标
    await loadQueueStatus();
}

// 渲染种子列表
function renderSeedList() {
    const container = document.getElementById('seed-list');
    const countEl = document.getElementById('seed-count');

    // 获取当前分组的种子数量
    const currentProject = state.currentProject;
    const stats = currentProject ? state.projectStats[currentProject] : null;
    const totalSeeds = stats ? stats.bloggerCount : state.seedBloggers.length;

    countEl.textContent = totalSeeds + ' 个种子';

    // 只显示种子数量统计，不显示详细列表
    if (totalSeeds === 0) {
        container.innerHTML = '<p style="color: var(--gray);">当前分组暂无种子博主</p>';
    } else {
        container.innerHTML = `<div style="padding: 20px; background: var(--light-gray); border-radius: 8px; text-align: center;">
            <div style="font-size: 2rem; color: var(--primary); margin-bottom: 10px;">🌱</div>
            <div style="font-size: 1.2rem; font-weight: 600; color: var(--dark);">该分组已有 ${totalSeeds} 个种子博主</div>
            <div style="margin-top: 10px; color: var(--gray); font-size: 0.9rem;">您可以继续添加更多种子或直接查看分析结果</div>
        </div>`;
    }
}

// 移除种子
function removeSeed(username) {
    state.seedBloggers = state.seedBloggers.filter(u => u !== username);
    renderSeedList();
}

// 提交共同标记数据
async function submitCoTagData() {
    const postId = document.getElementById('post-id').value.trim();
    const taggedText = document.getElementById('tagged-usernames').value.trim();
    const notes = document.getElementById('post-notes').value.trim();

    if (!postId || !taggedText) {
        showError('请填写帖子ID和被标记的用户名！');
        return;
    }

    const taggedUsernames = taggedText.split('\n').map(u => u.trim()).filter(u => u);

    try {
        const response = await fetch('/api/instagraph/relationship/co_tag', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                postId: postId,
                taggedUsernames: taggedUsernames,
                postNotes: notes
            })
        });

        if (!response.ok) throw new Error('提交失败');

        showSuccess('共同标记数据提交成功！');
        document.getElementById('post-id').value = '';
        document.getElementById('tagged-usernames').value = '';
        document.getElementById('post-notes').value = '';
    } catch (error) {
        showError('提交失败：' + error.message);
    }
}

// 运行增强的共同标记分析
async function runEnhancedAnalysis() {
    const project = document.getElementById('analysis-project').value;
    const minCoTags = document.getElementById('min-co-tags').value;
    const minSeedCount = parseInt(document.getElementById('min-seed-count').value);

    if (!project) {
        showToast('请选择分组！', 'warning');
        return;
    }

    showLoading('智能分析中...');

    try {
        // 优化：直接从 state.projectStats 获取种子总数，避免重新请求
        const projectStats = state.projectStats[project];
        let totalSeeds = projectStats ? projectStats.bloggerCount : 0;

        // 如果 state 中没有数据，则回退到 API 请求（健壮性）
        if (totalSeeds === 0) {
            console.warn('无法从 state 获取种子总数，回退到 API 请求');
            const bloggersResponse = await fetch('/api/instagraph/bloggers');
            const allBloggers = await bloggersResponse.json();
            const seedBloggers = allBloggers.filter(b => b.seedGroup === project);
            
            if (seedBloggers.length === 0) {
                showError('该分组暂无种子博主，请先添加种子！');
                return;
            }
            totalSeeds = seedBloggers.length;
        }

        // 第二步：将人数转换为覆盖率
        const minCoverage = minSeedCount / totalSeeds;

        // 验证参数合理性
        if (minSeedCount > totalSeeds) {
            showToast(`最小种子覆盖人数 (${minSeedCount}) 不能大于总种子数 (${totalSeeds})，已自动调整为 ${totalSeeds}`, 'warning');
            document.getElementById('min-seed-count').value = totalSeeds;
            return;
        }

        // 添加3秒等待时间，防止请求过于频繁
        await new Promise(resolve => setTimeout(resolve, 3000));

        // 第三步：调用增强分析 API
        const response = await fetch(
            `/api/instagraph/analysis/co-tagged-enhanced?project=${encodeURIComponent(project)}&min_co_tags=${minCoTags}&min_coverage=${minCoverage}`
        );

        if (!response.ok) {
            throw new Error('分析请求失败');
        }

        const data = await response.json();

        // 更新结果标题，显示实际使用的参数
        document.getElementById('results-title').textContent =
            `智能分析结果`;

        renderEnhancedAnalysisResults(data);

        // 显示分析成功提示
        if (data.length > 0) {
            showSuccess(`分析完成！发现 ${data.length} 个推荐博主`);
        } else {
            showToast('未发现符合条件的推荐博主，尝试降低筛选条件', 'warning');
        }
    } catch (error) {
        showError('分析失败：' + error.message);
        console.error('分析错误：', error);
    }
}

// 运行共同标记分析（简单版，保留兼容）
async function runCoTaggedAnalysis() {
    const project = document.getElementById('analysis-project').value;
    const minCoTags = document.getElementById('min-co-tags').value;

    if (!project) {
        showToast('请选择分组！', 'warning');
        return;
    }

    showLoading('分析中...');

    try {
        const response = await fetch(
            `/api/instagraph/analysis/co-tagged?project=${encodeURIComponent(project)}&min_co_tags=${minCoTags}`
        );
        const data = await response.json();

        document.getElementById('results-title').textContent = '共同标记分析结果';
        renderAnalysisResults(data);
    } catch (error) {
        showError('分析失败：' + error.message);
    }
}

// 运行共同关注分析（保留但不在新UI中使用）
async function runCommonFollowsAnalysis() {
    const project = document.getElementById('analysis-project').value;
    const minFollows = document.getElementById('min-follows').value;

    if (!project) {
        showToast('请选择分组！', 'warning');
        return;
    }

    showLoading('分析中...');

    try {
        const response = await fetch(
            `/api/instagraph/analysis/common-follows?project=${encodeURIComponent(project)}&min_follows=${minFollows}`
        );
        const data = await response.json();

        document.getElementById('results-title').textContent = '共同关注分析结果';
        renderAnalysisResults(data);
    } catch (error) {
        showError('分析失败：' + error.message);
    }
}

// 渲染增强分析结果
function renderEnhancedAnalysisResults(data) {
    const container = document.getElementById('analysis-results');
    const countEl = document.getElementById('results-count');
    const exportBtn = document.getElementById('export-btn');

    if (!data || data.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🔍</div>
                <p>未发现符合条件的新博主</p>
                <p style="font-size: 0.9rem; color: var(--gray); margin-top: 10px;">
                    尝试降低"最小种子覆盖率"或"最小共同标记次数"参数
                </p>
            </div>
        `;
        countEl.textContent = '0 个结果';
        exportBtn.style.display = 'none';
        state.currentAnalysisResults = [];
        state.currentAnalysisType = 'enhanced';
        return;
    }

    // 保存当前结果用于导出
    state.currentAnalysisResults = data;
    state.currentAnalysisType = 'enhanced';

    countEl.textContent = data.length + ' 个结果';
    exportBtn.style.display = 'inline-flex';

    container.innerHTML = data.map((item, index) => {
        const rank = index + 1;

        // 计算各维度得分
        const coverageScore = item.connectedSeeds * 10;
        const coTagScore = item.coTaggedCount * 5;
        const totalScore = item.compositeScore;

        // 计算各维度占比
        const coveragePercent = (coverageScore / totalScore * 100).toFixed(1);
        const coTagPercent = (coTagScore / totalScore * 100).toFixed(1);

        return `
            <div class="result-card" id="result-card-${item.username}" style="position: relative; overflow: visible;">
                <div style="position: relative; z-index: 1;">
                    <!-- 头部：排名、用户名和综合评分 -->
                    <div class="result-header" style="margin-bottom: 20px;">
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <span style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">#${rank}</span>
                            <div>
                                <a href="https://www.instagram.com/${item.username}/" target="_blank"
                                   class="username-link"
                                   style="font-size: 1.1rem; font-weight: 600;"
                                   onclick="event.stopPropagation()">@${item.username}</a>
                            </div>
                        </div>
                        <div style="text-align: right;">
                            <div style="font-size: 0.75rem; color: var(--gray); margin-bottom: 2px;">综合评分</div>
                            <div style="font-size: 1.5rem; font-weight: 700; color: var(--primary);">
                                ${totalScore.toFixed(1)}
                            </div>
                        </div>
                    </div>

                    <!-- 评分维度明细 -->
                    <div style="background: linear-gradient(135deg, rgba(99, 102, 241, 0.05), rgba(236, 72, 153, 0.05));
                                padding: 10px; border-radius: 12px; margin-bottom: 10px;
                                border: 2px solid rgba(99, 102, 241, 0.2);">
                        <div style="font-size: 0.875rem; font-weight: 600; color: var(--dark); margin-bottom: 6px;">
                            📊 评分明细
                        </div>
                        <!-- 覆盖人数得分 -->
                        <div style="margin-bottom: 8px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px;">
                                <span style="font-size: 0.8rem; color: var(--dark);">
                                    🎯 <strong>覆盖人数</strong> (10分/人)
                                </span>
                                <span style="font-size: 0.875rem; font-weight: 600; color: var(--primary);">
                                    ${coverageScore.toFixed(1)} 分
                                </span>
                            </div>
                            <div style="font-size: 0.7rem; color: var(--gray); margin-top: 1px;">
                                与 <a href="javascript:void(0)" onclick="showConnectedSeeds('${item.username}', '${document.getElementById('analysis-project').value}')" style="color: var(--primary); text-decoration: underline; cursor: pointer;">${item.connectedSeeds} 个种子</a>有连接
                            </div>
                        </div>

                        <!-- 共同标记得分 -->
                        <div style="margin-bottom: 8px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 3px;">
                                <span style="font-size: 0.8rem; color: var(--dark);">
                                    📌 <strong>共同标记</strong> (5分/次)
                                </span>
                                <span style="font-size: 0.875rem; font-weight: 600; color: var(--success);">
                                    ${coTagScore.toFixed(1)} 分
                                </span>
                            </div>
                            <div style="font-size: 0.7rem; color: var(--gray); margin-top: 1px;">
                                在 <a href="javascript:void(0)" onclick="showCoTaggedPosts('${item.username}', '${document.getElementById('analysis-project').value}')" style="color: var(--success); text-decoration: underline; cursor: pointer;">${item.coTaggedCount} 个帖子</a>中被共同标记
                            </div>
                        </div>
                    </div>

                    <!-- 操作按钮 -->
                    <div style="display: flex; gap: 8px;">
                        <button class="btn btn-sm btn-success"
                                onclick="promoteToSeed('${item.username}')"
                                style="flex: 1;">
                            🌱 晋升
                        </button>
                        <button class="btn btn-sm"
                                onclick="abandonBlogger('${item.username}')"
                                style="flex: 1; background: var(--warning); color: white;">
                            ⛔ 放弃
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// 渲染分析结果
function renderAnalysisResults(data) {
    const container = document.getElementById('analysis-results');
    const countEl = document.getElementById('results-count');
    const exportBtn = document.getElementById('export-btn');

    if (!data || data.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🔍</div>
                <p>未发现新博主</p>
            </div>
        `;
        countEl.textContent = '0 个结果';
        exportBtn.style.display = 'none';
        state.currentAnalysisResults = [];
        state.currentAnalysisType = 'simple';
        return;
    }

    // 保存当前结果用于导出
    state.currentAnalysisResults = data;
    state.currentAnalysisType = 'simple';

    countEl.textContent = data.length + ' 个结果';
    exportBtn.style.display = 'inline-flex';

    container.innerHTML = data.map(item => `
        <div class="result-card">
            <div class="result-header">
                <div class="result-username">
                    <a href="https://www.instagram.com/${item.username}/" target="_blank" class="username-link" onclick="event.stopPropagation()">@${item.username}</a>
                </div>
                <div class="result-score">${item.count}</div>
            </div>
            <button class="btn btn-sm btn-success" onclick="promoteToSeed('${item.username}')" style="margin-top: 10px; width: 100%;">晋升为种子博主</button>
            <button class="btn btn-sm" onclick="abandonBlogger('${item.username}')" style="margin-top: 8px; width: 100%; background: var(--warning); color: white;">⛔ 放弃此博主</button>
        </div>
    `).join('');
}

// 显示连接的种子博主列表
async function showConnectedSeeds(username, project) {
    try {
        // 调用后端API获取连接的种子博主列表
        const response = await fetch(`/api/instagraph/analysis/connected-seeds?username=${encodeURIComponent(username)}&project=${encodeURIComponent(project)}`);
        if (!response.ok) throw new Error('获取数据失败');
        
        const seeds = await response.json();
        
        // 创建弹窗显示
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 600px; max-height: 80vh; overflow-y: auto;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">🌱 与 @${username} 有连接的种子博主</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px; color: var(--gray);">
                        共 <strong>${seeds.length}</strong> 个种子博主与 @${username} 在帖子中被共同标记
                    </p>
                    <div style="display: grid; gap: 10px;">
                        ${seeds.map(seed => `
                            <div style="padding: 12px; background: var(--light-gray); border-radius: 8px; display: flex; justify-content: space-between; align-items: center;">
                                <a href="https://www.instagram.com/${seed.username}/" target="_blank" class="username-link" style="font-weight: 600;">
                                    @${seed.username}
                                </a>
                                <span style="color: var(--gray); font-size: 0.9rem;">
                                    ${seed.coTagCount} 个共同帖子
                                </span>
                            </div>
                        `).join('')}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove();">
                        关闭
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    } catch (error) {
        showError('获取种子博主列表失败：' + error.message);
    }
}

async function showConnectedSeedsFromDataTab(username, seedGroup) {
    const defaultProject = seedGroup || null;
    const project = await showProjectSelectDialog(username, defaultProject);
    if (!project) return;
    await showConnectedSeeds(username, project);
}

// 显示共同标记的帖子列表
async function showCoTaggedPosts(username, project) {
    try {
        // 调用后端API获取共同标记的帖子列表
        const response = await fetch(`/api/instagraph/analysis/co-tagged-posts?username=${encodeURIComponent(username)}&project=${encodeURIComponent(project)}`);
        if (!response.ok) throw new Error('获取数据失败');
        
        const posts = await response.json();
        
        // 创建弹窗显示
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 700px; max-height: 80vh; overflow-y: auto;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--success);">📌 与 @${username} 共同标记的帖子</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px; color: var(--gray);">
                        共 <strong>${posts.length}</strong> 个帖子中，@${username} 与种子博主被共同标记
                    </p>
                    <div style="display: grid; gap: 12px;">
                        ${posts.map(post => `
                            <div style="padding: 15px; background: var(--light-gray); border-radius: 8px;">
                                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 10px;">
                                    <a href="https://www.instagram.com/p/${post.shortCode}/" target="_blank" 
                                       style="color: var(--primary); font-weight: 600; text-decoration: none;">
                                        📷 查看帖子
                                    </a>
                                </div>
                                <div style="font-size: 0.9rem; color: var(--gray); margin-bottom: 8px;">
                                    共同标记的种子博主：
                                </div>
                                <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                                    ${post.taggedSeeds.map(seed => `
                                        <a href="https://www.instagram.com/${seed}/" target="_blank" 
                                           class="username-link"
                                           style="font-size: 0.85rem; padding: 4px 8px; background: white; border-radius: 4px;">
                                            @${seed}
                                        </a>
                                    `).join('')}
                                </div>
                                
                                ${(() => {
                                    // 确保 allTaggedUsers 和 taggedSeeds 都是数组，且 allTaggedUsers 存在
                                    if (!Array.isArray(post.allTaggedUsers) || !Array.isArray(post.taggedSeeds)) {
                                        return '';
                                    }
                                    
                                    // 计算其他被标记的用户（排除种子和当前用户自己）
                                    const otherTagged = post.allTaggedUsers.filter(user => 
                                        !post.taggedSeeds.includes(user) && user !== username
                                    );

                                    // 如果没有其他被标记的用户，则不显示该部分
                                    if (otherTagged.length === 0) {
                                        return '';
                                    }

                                    // 如果有，则渲染标题和用户列表
                                    return `
                                        <div style="font-size: 0.9rem; color: var(--gray); margin-bottom: 8px; margin-top: 12px;">
                                            其他被标记：
                                        </div>
                                        <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                                            ${otherTagged.map(user => `
                                                <a href="https://www.instagram.com/${user}/" target="_blank"
                                                   class="username-link-secondary"
                                                   style="font-size: 0.85rem; padding: 4px 8px; background: #f0f0f0; border-radius: 4px;">
                                                    @${user}
                                                </a>
                                            `).join('')}
                                        </div>
                                    `;
                                })()}
                            </div>
                        `).join('')}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove();">
                        关闭
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    } catch (error) {
        showError('获取帖子列表失败：' + error.message);
    }
}

async function showCoTaggedPostsFromDataTab(username, seedGroup) {
    const defaultProject = seedGroup || null;
    const project = await showProjectSelectDialog(username, defaultProject);
    if (!project) return;
    await showCoTaggedPosts(username, project);
}

// 显示用户被标记的所有帖子
async function showTaggedPostsForBlogger(username) {
    try {
        const response = await fetch(`/api/instagraph/blogger/${username}/tagged-posts`);
        if (!response.ok) throw new Error('获取帖子失败');
        const posts = await response.json();

        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 700px; max-height: 80vh; overflow-y: auto;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">@${username} 被标记的帖子</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px; color: var(--gray);">
                        共在 <strong>${posts.length}</strong> 个帖子中被标记
                    </p>
                    <div style="display: grid; gap: 12px;">
                        ${posts.map(post => `
                            <div style="padding: 15px; background: var(--light-gray); border-radius: 8px;">
                                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 10px;">
                                    <a href="https://www.instagram.com/p/${post.shortcode}/" target="_blank"
                                       style="color: var(--primary); font-weight: 600; text-decoration: none;">
                                        📷 查看帖子
                                    </a>
                                    <span style="font-size: 0.8rem; color: var(--gray);">
                                        ${new Date(post.timestamp * 1000).toLocaleDateString()}
                                    </span>
                                </div>
                                <p style="font-size: 0.9rem; color: var(--dark); margin: 0; white-space: pre-wrap;">${post.caption || '<em>无描述</em>'}</p>
                            </div>
                        `).join('')}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove();">
                        关闭
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
            }
        });
    } catch (error) {
        showError('获取帖子列表失败: ' + error.message);
    }
}

// 显示晋升种子博主对话框
async function showPromoteDialog(username) {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        
        const reasons = [
            '舞蹈学校老师',
            '舞蹈社团账号',
            '模特公司账号',
            '啦啦队账号',
            '面试过的博主',
            '联系过的博主',
            '拒绝联系的博主',
            '其他'
        ];

        const reasonOptions = reasons.map((r, i) => {
            const isChecked = i === 0 ? 'checked' : '';
            return `
            <div style="margin-bottom: 0;">
                <label style="display: flex; align-items: center; cursor: pointer;">
                    <input type="radio" name="promote-reason" value="${r}" ${isChecked} 
                           onchange="document.getElementById('other-reason-container').style.display = this.value === '其他' ? 'block' : 'none'">
                    <span style="margin-left: 8px;">${r}</span>
                </label>
            </div>
            `;
        }).join('');

        modal.innerHTML = `
            <div class="modal-content" style="max-width: 500px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">🌱 晋升种子</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px;">确定要将 <strong>@${username}</strong> 晋升为种子博主吗？</p>
                    
                    <div style="margin-bottom: 15px;">
                        <p style="font-weight: 500; margin-bottom: 8px; color: var(--dark);">请选择备注标签：</p>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                            ${reasonOptions}
                        </div>
                    </div>

                    <div id="other-reason-container" style="display: none;">
                        <textarea id="promote-reason-text" 
                                  rows="2" 
                                  placeholder="请输入其他备注信息..."
                                  style="width: 100%; padding: 8px; border: 1px solid var(--border); border-radius: 4px; font-family: inherit;"></textarea>
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="this.closest('.modal-overlay').remove(); window.promoteDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn btn-primary" 
                            onclick="
                                const selected = document.querySelector('input[name=promote-reason]:checked').value;
                                let result = selected;
                                if (selected === '其他') {
                                    const otherText = document.getElementById('promote-reason-text').value.trim();
                                    result = otherText || '其他';
                                }
                                this.closest('.modal-overlay').remove(); 
                                window.promoteDialogResolve(result);
                            ">
                        确定晋升
                    </button>
                </div>
            </div>
        `;
        
        window.promoteDialogResolve = resolve;
        document.body.appendChild(modal);
        
        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
                resolve(null);
            }
        });
    });
}

// 晋升为种子博主
async function promoteToSeed(username) {
    if (!state.currentProject) {
        const project = document.getElementById('analysis-project').value;
        if (!project) {
            showToast('请先选择分组！', 'warning');
            return;
        }
        state.currentProject = project;
    }

    const seedReason = await showPromoteDialog(username);

    if (seedReason === null) {
        return;
    }

    try {
        const response = await fetch('/api/instagraph/blogger', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: username,
                seedGroup: state.currentProject,
                seedReason: seedReason
            })
        });

        if (!response.ok) throw new Error('晋升失败');

        showSuccess('@' + username + ' 已晋升为种子博主！');

        // 自动采集新种子的数据
        showToast('正在自动采集 @' + username + ' 的数据，这可能需要几分钟...', 'warning');
        
        // 从列表中移除该卡片，而不是刷新整个列表
        const card = document.getElementById(`result-card-${username}`);
        if (card) {
            // card.style.transition = 'opacity 0.5s, transform 0.5s';
            // card.style.opacity = '0';
            // card.style.transform = 'scale(0.9)';
            // setTimeout(() => {
            //     card.remove();
            //     // 更新计数
            //     const countEl = document.getElementById('results-count');
            //     if (countEl && state.currentAnalysisResults) {
            //         state.currentAnalysisResults = state.currentAnalysisResults.filter(item => item.username !== username);
            //         countEl.textContent = state.currentAnalysisResults.length + ' 个结果';
            //     }
            // }, 500);
        } else if (document.getElementById('data-tab')?.classList.contains('active')) {
            // 如果是在数据管理页面，则刷新列表
            await loadBloggersPage();
        }

        try {
            await aggregateUserData(username);
        } catch (aggError) {
            // 采集失败不阻断流程，但显示错误信息
            console.error('自动采集数据失败:', aggError);
            showError('自动采集 @' + username + ' 的数据失败：' + (aggError.message || '未知错误'));
        }
    } catch (error) {
        showError('晋升失败：' + error.message);
    }
}

// 放弃博主
async function abandonBlogger(username) {
    const reason = await showAbandonDialog(username);
    
    if (reason === null) {
        // 用户取消了操作
        return;
    }

    try {
        const url = reason.trim() 
            ? `/api/instagraph/blogger/${username}/abandon?reason=${encodeURIComponent(reason)}`
            : `/api/instagraph/blogger/${username}/abandon`;
        
        const response = await fetch(url, {
            method: 'PUT'
        });

        if (!response.ok) throw new Error('放弃失败');

        showSuccess(`@${username} 已标记为放弃状态！`);

        // 智能处理：如果是分析页面，直接移除卡片；如果是数据页面，刷新列表
        const card = document.getElementById(`result-card-${username}`);
        if (card && document.getElementById('analysis-tab')?.classList.contains('active')) {
            // card.style.transition = 'opacity 0.5s, transform 0.5s';
            // card.style.opacity = '0';
            // card.style.transform = 'scale(0.9)';
            // setTimeout(() => {
            //     card.remove();
            //     // 更新计数
            //     const countEl = document.getElementById('results-count');
            //     if (countEl && state.currentAnalysisResults) {
            //         state.currentAnalysisResults = state.currentAnalysisResults.filter(item => item.username !== username);
            //         countEl.textContent = state.currentAnalysisResults.length + ' 个结果';
            //     }
            // }, 500);
        } else if (document.getElementById('data-tab')?.classList.contains('active')) {
            await loadBloggersPage();
        }
    } catch (error) {
        showError('放弃失败：' + error.message);
    }
}

// 批量放弃博主
async function bulkAbandonBloggers() {
    const list = document.getElementById('bulk-abandon-list').value.trim();
    if (!list) {
        showError('请输入要放弃的博主列表！');
        return;
    }

    const lines = list.split('\n').filter(line => line.trim() !== '');
    const total = lines.length;

    const confirmed = await showConfirm(
        `确定要批量放弃 ${total} 个博主吗？<br><br>此操作将逐个处理，请耐心等待。`,
        '批量放弃确认',
        '⚠️'
    );

    if (!confirmed) {
        return;
    }

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < total; i++) {
        const line = lines[i].trim();
        const parts = line.split(/\s+/);
        const username = parts.shift();
        const reason = parts.join(' ');

        if (!username) continue;

        try {
            showToast(`(${i + 1}/${total}) 正在放弃 @${username}...`, 'warning');
            const url = reason
                ? `/api/instagraph/blogger/${username}/abandon?reason=${encodeURIComponent(reason)}`
                : `/api/instagraph/blogger/${username}/abandon`;

            const response = await fetch(url, { method: 'PUT' });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'API请求失败');
            }

            successCount++;
        } catch (error) {
            failCount++;
            showError(`放弃 @${username} 失败: ${error.message}`);
            // 等待一段时间，避免错误提示堆叠
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    showSuccess(`批量操作完成！成功 ${successCount} 个，失败 ${failCount} 个。`);

    // 清空文本框
    document.getElementById('bulk-abandon-list').value = '';

    // 刷新博主列表
    await loadBloggersPage();
}

// 显示批量放弃对话框
async function showBulkAbandonDialog() {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 600px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--warning);">⛔ 批量放弃博主</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px;">请输入要放弃的博主列表，每行一个用户名，后面可以跟上放弃原因（用空格隔开）。</p>
                    <div class="form-group">
                        <label class="form-label">博主列表</label>
                        <textarea id="bulk-abandon-list-modal" class="form-input" rows="8" placeholder="每行一个用户名，后面可以跟上放弃原因，用空格隔开。例如：&#10;user1 内容不符&#10;user2 已联系无回应"></textarea>
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove(); window.bulkAbandonDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn" style="background: var(--warning); color: white;"
                            onclick="
                                const list = document.getElementById('bulk-abandon-list-modal').value;
                                this.closest('.modal-overlay').remove();
                                window.bulkAbandonDialogResolve(list);
                            ">
                        确定批量放弃
                    </button>
                </div>
            </div>
        `;

        window.bulkAbandonDialogResolve = async (list) => {
            if (list && list.trim()) {
                const lines = list.split('\n').filter(line => line.trim() !== '');
                const total = lines.length;

                const confirmed = await showConfirm(
                    `确定要批量放弃 ${total} 个博主吗？<br><br>此操作将逐个处理，请耐心等待。`,
                    '批量放弃确认',
                    '⚠️'
                );

                if (!confirmed) {
                    resolve(null);
                    return;
                }

                let successCount = 0;
                let failCount = 0;

                for (let i = 0; i < total; i++) {
                    const line = lines[i].trim();
                    const parts = line.split(/\s+/);
                    const username = parts.shift();
                    const reason = parts.join(' ');

                    if (!username) continue;

                    try {
                        showToast(`(${i + 1}/${total}) 正在放弃 @${username}...`, 'warning');
                        const url = reason
                            ? `/api/instagraph/blogger/${username}/abandon?reason=${encodeURIComponent(reason)}`
                            : `/api/instagraph/blogger/${username}/abandon`;

                        const response = await fetch(url, { method: 'PUT' });

                        if (!response.ok) {
                            const errorData = await response.json();
                            throw new Error(errorData.error || 'API请求失败');
                        }

                        successCount++;
                    } catch (error) {
                        failCount++;
                        showError(`放弃 @${username} 失败: ${error.message}`);
                        await new Promise(resolve => setTimeout(resolve, 500));
                    }
                }

                showSuccess(`批量操作完成！成功 ${successCount} 个，失败 ${failCount} 个。`);

                // 刷新博主列表
                await loadBloggersPage();
            }
            resolve(null);
        };

        document.body.appendChild(modal);

        // 聚焦到输入框
        setTimeout(() => {
            document.getElementById('bulk-abandon-list-modal')?.focus();
        }, 100);

        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
                resolve(null);
            }
        });
    });
}

// 显示切换分组对话框
async function showSwitchGroupDialog(username, currentGroup) {
    const groups = Array.from(state.allProjects || []);

    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';

        const optionsHtml = `
            <option value="">-- 未分组 --</option>
            ${groups.map(group => {
                const selected = group === currentGroup ? 'selected' : '';
                return `<option value="${group}" ${selected}>${group}</option>`;
            }).join('')}
        `;

        modal.innerHTML = `
            <div class="modal-content" style="max-width: 480px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">🔄 切换分组</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 12px; color: var(--gray);">
                        为 <strong>@${username}</strong> 选择一个新的分组：
                    </p>
                    <div class="form-group">
                        <label class="form-label">目标分组</label>
                        <select id="switch-group-select" class="form-select">
                            ${optionsHtml}
                        </select>
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove(); window.switchGroupDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn btn-primary" onclick="
                        (function() {
                            const select = document.getElementById('switch-group-select');
                            const newGroup = select ? select.value : '';
                            const overlay = select.closest('.modal-overlay');
                            if (overlay) overlay.remove();
                            window.switchGroupDialogResolve(newGroup);
                        })();
                    ">
                        确定切换
                    </button>
                </div>
            </div>
        `;

        window.switchGroupDialogResolve = async (newGroup) => {
            if (newGroup !== null) {
                try {
                    const response = await fetch('/api/instagraph/blogger', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            username: username,
                            seedGroup: newGroup || null
                        })
                    });

                    if (!response.ok) {
                        throw new Error('更新失败');
                    }

                    showSuccess(`已将 @${username} 的分组更新为 ${newGroup || '未分组'}`);
                    await loadBloggersPage();
                } catch (error) {
                    showError('更新分组失败：' + error.message);
                    await loadBloggersPage();
                }
            }
            resolve(null);
        };

        document.body.appendChild(modal);

        setTimeout(() => {
            document.getElementById('switch-group-select')?.focus();
        }, 100);

        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
                resolve(null);
            }
        });
    });
}

// 从数据管理页面晋升为种子博主
async function promoteToSeedFromDataTab(username) {
    const groups = Array.from(state.allProjects || []);

    if (groups.length === 0) {
        showError('当前没有可用的分组，请先在"工作流程"中创建至少一个分组。');
        return;
    }

    // 首先选择分组
    const selectedGroup = await showGroupSelectDialogForPromote(username, groups);
    if (!selectedGroup) return;

    // 然后选择晋升原因
    const seedReason = await showPromoteDialog(username);
    if (seedReason === null) return;

    try {
        const response = await fetch('/api/instagraph/blogger', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: username,
                seedGroup: selectedGroup,
                seedReason: seedReason
            })
        });

        if (!response.ok) throw new Error('晋升失败');

        showSuccess('@' + username + ' 已晋升为种子博主！');

        // 自动采集新种子的数据
        showToast('正在自动采集 @' + username + ' 的数据，这可能需要几分钟...', 'warning');

        try {
            await aggregateUserData(username);
        } catch (aggError) {
            console.error('自动采集数据失败:', aggError);
            showError('自动采集 @' + username + ' 的数据失败：' + (aggError.message || '未知错误'));
        }

        // 刷新博主列表
        await loadBloggersPage();
    } catch (error) {
        showError('晋升失败：' + error.message);
    }
}

// 显示分组选择对话框（用于晋升）
async function showGroupSelectDialogForPromote(username, groups) {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';

        const optionsHtml = groups.map(group => {
            const stats = state.projectStats[group];
            const seedCount = stats ? stats.bloggerCount : 0;
            return `<option value="${group}">${group} (${seedCount} 个种子)</option>`;
        }).join('');

        modal.innerHTML = `
            <div class="modal-content" style="max-width: 480px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">选择目标分组</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 12px; color: var(--gray);">
                        将 <strong>@${username}</strong> 晋升为种子博主，请选择目标分组：
                    </p>
                    <select id="promote-group-select" class="form-select">
                        ${optionsHtml}
                    </select>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove(); window.promoteGroupDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn btn-primary" onclick="
                        (function() {
                            const select = document.getElementById('promote-group-select');
                            const group = select ? select.value : '';
                            const overlay = select.closest('.modal-overlay');
                            if (overlay) overlay.remove();
                            window.promoteGroupDialogResolve(group || null);
                        })();
                    ">
                        下一步
                    </button>
                </div>
            </div>
        `;

        window.promoteGroupDialogResolve = resolve;
        document.body.appendChild(modal);

        setTimeout(() => {
            document.getElementById('promote-group-select')?.focus();
        }, 100);

        // 点击遮罩层关闭
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.remove();
                resolve(null);
            }
        });
    });
}

// 恢复博主
async function restoreBlogger(username) {
    const confirmed = await showConfirm(
        `确定要恢复 @${username} 吗？\n恢复后该博主将重新出现在智能分析结果中。`,
        '恢复博主',
        '♻️'
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`/api/instagraph/blogger/${username}/restore`, {
            method: 'PUT'
        });

        if (!response.ok) throw new Error('恢复失败');

        showSuccess(`@${username} 已恢复！`);
        
        // 刷新博主列表（如果在数据管理页面）
        if (document.getElementById('data-tab')?.classList.contains('active')) {
            await loadBloggersPage();
        }
    } catch (error) {
        showError('恢复失败：' + error.message);
    }
}

// 检查博主状态
async function checkBloggerStatus(username) {
    try {
        const response = await fetch(`/api/instagraph/blogger/${username}/status`);
        if (!response.ok) throw new Error('检查状态失败');
        return await response.json();
    } catch (error) {
        console.error('检查博主状态失败:', error);
        return null;
    }
}

// 显示放弃原因输入对话框
async function showAbandonDialog(username) {
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 500px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--warning);">⛔ 放弃博主</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 15px;">确定要放弃 <strong>@${username}</strong> 吗？</p>
                    <p style="font-size: 0.9rem; color: var(--gray); margin-bottom: 15px;">
                        放弃后，该博主将不再出现在智能分析结果中。您可以随时在数据管理页面恢复。
                    </p>
                    <label for="abandon-reason" style="display: block; margin-bottom: 5px; font-weight: 500;">
                        放弃原因（可选）：
                    </label>
                    <textarea id="abandon-reason" 
                              rows="3" 
                              placeholder="例如：内容不符合项目定位、已联系无回应等..."
                              style="width: 100%; padding: 8px; border: 1px solid var(--border); border-radius: 4px; font-family: inherit;"></textarea>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="this.closest('.modal-overlay').remove(); window.abandonDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn" style="background: var(--warning); color: white;" 
                            onclick="
                                const reason = document.getElementById('abandon-reason').value;
                                this.closest('.modal-overlay').remove(); 
                                window.abandonDialogResolve(reason);
                            ">
                        确定放弃
                    </button>
                </div>
            </div>
        `;
        
        window.abandonDialogResolve = resolve;
        document.body.appendChild(modal);
        
        // 聚焦到输入框
        setTimeout(() => {
            document.getElementById('abandon-reason')?.focus();
        }, 100);
    });
}

// 显示分组选择对话框（用于"共同连接/共同帖子"）
async function showProjectSelectDialog(username, defaultProject) {
    const groups = Array.from(state.allProjects || []);
    if (!groups.length) {
        showError('当前没有可用的分组，请先在"工作流程"中创建至少一个分组。');
        return null;
    }

    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.className = 'modal-overlay';

        const optionsHtml = groups.map(group => {
            const selected = group === defaultProject ? 'selected' : '';
            return `<option value="${group}" ${selected}>${group}</option>`;
        }).join('');

        modal.innerHTML = `
            <div class="modal-content" style="max-width: 480px;">
                <div class="modal-header">
                    <h3 style="margin: 0; color: var(--primary);">选择分组</h3>
                </div>
                <div class="modal-body">
                    <p style="margin-bottom: 12px; color: var(--gray);">
                        为 <strong>@${username}</strong> 选择一个分组，用于计算与哪些种子有共同连接/共同帖子：
                    </p>
                    <select id="analysis-project-select" class="form-select">
                        ${optionsHtml}
                    </select>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove(); window.analysisProjectDialogResolve(null);">
                        取消
                    </button>
                    <button class="btn btn-primary" onclick="
                        (function() {
                            const select = document.getElementById('analysis-project-select');
                            const project = select ? select.value : '';
                            const overlay = select.closest('.modal-overlay');
                            if (overlay) overlay.remove();
                            window.analysisProjectDialogResolve(project || null);
                        })();
                    ">
                        确定
                    </button>
                </div>
            </div>
        `;

        window.analysisProjectDialogResolve = resolve;
        document.body.appendChild(modal);

        setTimeout(() => {
            document.getElementById('analysis-project-select')?.focus();
        }, 100);
    });
}

// 采集用户数据
async function aggregateUserData(username) {
    // 如果没有传参数，从输入框获取
    if (!username) {
        username = document.getElementById('aggregate-username').value.trim();
    }

    if (!username) {
        showError('请输入用户名！');
        return;
    }

    try {
        // 检查博主状态
        const status = await checkBloggerStatus(username);
        
        // 如果博主已被放弃，询问是否恢复
        if (status && status.exists && status.abandoned) {
            const reasonText = status.abandonedReason ? `\n\n放弃原因：${status.abandonedReason}` : '';
            const confirmed = await showConfirm(
                `该博主已被标记为放弃状态。${reasonText}\n\n是否要恢复该博主并继续采集？`,
                '博主已放弃',
                '⚠️'
            );
            
            if (!confirmed) {
                return;
            }
            
            // 先恢复博主
            try {
                const restoreResponse = await fetch(`/api/instagraph/blogger/${username}/restore`, {
                    method: 'PUT'
                });
                if (!restoreResponse.ok) throw new Error('恢复失败');
                showSuccess(`@${username} 已恢复！`);
            } catch (restoreError) {
                showError('恢复失败：' + restoreError.message);
                return;
            }
        }

        const response = await fetch('/api/aggregate/' + username);
        const data = await response.json();

        if (!response.ok) {
            // 可能是任务已在队列中或正在执行
            showError(data.error || '添加任务失败');
            return;
        }

        // 任务已加入队列
        showSuccess(`任务已加入队列：@${username}`);

        // 清空输入框
        const inputEl = document.getElementById('aggregate-username');
        if (inputEl) {
            inputEl.value = '';
        }

        // 刷新队列状态
        loadQueueStatus();
    } catch (error) {
        showError('添加任务失败：' + error.message);
    }
}

// 加载任务队列状态
async function loadQueueStatus() {
    try {
        const response = await fetch('/api/aggregate/queue/status');
        const data = await response.json();

        // 渲染运行中的任务
        renderRunningTask(data.runningTask);

        // 渲染待执行队列
        renderPendingTasks(data.pendingTasks);

        // 渲染已完成任务
        renderCompletedTasks(data.completedTasks);

        // 更新待执行任务计数
        const pendingCount = data.pendingCount || 0;
        const pendingCountEl = document.getElementById('pending-count');
        if (pendingCountEl) {
            pendingCountEl.textContent = pendingCount + ' 个任务';
        }

        // 更新tab badge
        const badge = document.getElementById('queue-badge');
        if (badge) {
            const runningCount = data.runningTask ? 1 : 0;
            const totalActiveCount = runningCount + pendingCount;

            if (totalActiveCount > 0) {
                badge.textContent = totalActiveCount;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('加载队列状态失败:', error);
    }
}

// 渲染运行中的任务
function renderRunningTask(task) {
    const container = document.getElementById('running-task-container');
    if (!container) return;

    if (!task) {
        container.innerHTML = `
            <div style="padding: 20px; text-align: center; color: var(--gray); background: var(--light-gray); border-radius: 8px;">
                暂无运行中的任务
            </div>
        `;
        return;
    }

    const startedAt = new Date(task.startedAt).toLocaleString('zh-CN');

    container.innerHTML = `
        <div style="padding: 15px; background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(236, 72, 153, 0.1));
                    border: 2px solid var(--primary); border-radius: 8px; position: relative;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-size: 1.1rem; font-weight: 600; color: var(--dark); margin-bottom: 5px;">
                        <span style="color: var(--primary);">🔄</span> @${task.username}
                    </div>
                    <div style="font-size: 0.875rem; color: var(--gray);">
                        开始时间：${startedAt}
                    </div>
                </div>
                <div class="loading-spinner" style="width: 30px; height: 30px;"></div>
            </div>
        </div>
    `;
}

// 渲染待执行任务列表
function renderPendingTasks(tasks) {
    const container = document.getElementById('pending-tasks-container');
    if (!container) return;

    if (!tasks || tasks.length === 0) {
        container.innerHTML = `
            <div style="padding: 20px; text-align: center; color: var(--gray); background: var(--light-gray); border-radius: 8px;">
                队列为空
            </div>
        `;
        return;
    }

    container.innerHTML = tasks.map((task, index) => {
        const createdAt = new Date(task.createdAt).toLocaleString('zh-CN');
        return `
            <div style="padding: 12px; background: var(--white); border: 1px solid var(--light-gray);
                        border-radius: 8px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <div style="font-weight: 600; color: var(--dark);">
                        <span style="color: var(--warning);">#${index + 1}</span> @${task.username}
                    </div>
                    <div style="font-size: 0.875rem; color: var(--gray); margin-top: 2px;">
                        创建时间：${createdAt}
                    </div>
                </div>
                <button class="btn btn-sm" style="background: var(--danger); color: white;"
                        onclick="cancelTask('${task.id}')">
                    取消
                </button>
            </div>
        `;
    }).join('');
}

// 渲染已完成任务
function renderCompletedTasks(tasks) {
    const container = document.getElementById('completed-tasks-container');
    if (!container) return;

    if (!tasks || tasks.length === 0) {
        container.innerHTML = `
            <div style="padding: 20px; text-align: center; color: var(--gray); background: var(--light-gray); border-radius: 8px;">
                暂无完成记录
            </div>
        `;
        return;
    }

    container.innerHTML = tasks.map(task => {
        const completedAt = new Date(task.completedAt).toLocaleString('zh-CN');
        const isSuccess = task.status === 'COMPLETED';
        const isFailed = task.status === 'FAILED';
        const statusIcon = isSuccess ? '✅' : '❌';
        const statusColor = isSuccess ? 'var(--success)' : 'var(--danger)';

        return `
            <div style="padding: 12px; background: var(--white); border: 1px solid var(--light-gray);
                        border-radius: 8px; margin-bottom: 8px;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="flex: 1;">
                        <div style="font-weight: 600; color: var(--dark);">
                            <span style="color: ${statusColor};">${statusIcon}</span> @${task.username}
                        </div>
                        <div style="font-size: 0.875rem; color: var(--gray); margin-top: 2px;">
                            完成时间：${completedAt}
                        </div>
                        ${task.errorMessage ? `
                            <div style="font-size: 0.875rem; color: var(--danger); margin-top: 4px;">
                                错误：${task.errorMessage}
                            </div>
                        ` : ''}
                    </div>
                    ${isFailed ? `
                        <button class="btn btn-sm btn-primary" onclick="retryAggregation('${task.username}')" style="margin-left: 10px;">
                            🔄 重新采集
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    }).join('');
}

// 批量采集用户数据并加入队列
async function bulkAggregateFromQueue() {
    const list = document.getElementById('queue-bulk-aggregate-usernames').value.trim();
    if (!list) {
        showError('请输入要采集的用户名列表！');
        return;
    }

    const usernames = list.split('\n').map(u => u.trim()).filter(u => u);
    const total = usernames.length;

    const confirmed = await showConfirm(
        `确定要将 ${total} 个用户加入采集队列吗？`,
        '批量采集确认',
        '📥'
    );

    if (!confirmed) {
        return;
    }

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < total; i++) {
        const username = usernames[i];
        try {
            showToast(`(${i + 1}/${total}) 正在处理 @${username}...`, 'warning');
            
            // 复用单个用户添加的逻辑，但不处理UI反馈，统一在循环结束后处理
            const response = await fetch('/api/aggregate/' + username);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || '添加任务失败');
            }
            
            successCount++;
        } catch (error) {
            failCount++;
            showError(`添加 @${username} 失败: ${error.message}`);
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    showSuccess(`批量操作完成！成功 ${successCount} 个，失败 ${failCount} 个。`);
    
    // 清空文本框
    document.getElementById('queue-bulk-aggregate-usernames').value = '';

    // 刷新队列状态
    await loadQueueStatus();
}


// 从队列标签页批量添加任务
async function bulkAggregateUserDataFromQueue() {
    const list = document.getElementById('queue-aggregate-username-list').value.trim();

    if (!list) {
        showError('请输入用户名列表！');
        return;
    }

    const usernames = list.split('\n').map(u => u.trim()).filter(u => u);
    const total = usernames.length;

    const confirmed = await showConfirm(
        `确定要将 ${total} 个用户加入采集队列吗？<br><br>如果列表中包含已被放弃的博主，系统将自动恢复它们。`,
        '批量采集确认',
        '📋'
    );

    if (!confirmed) {
        return;
    }

    let successCount = 0;
    let failCount = 0;

    for (let i = 0; i < total; i++) {
        const username = usernames[i];
        try {
            showToast(`(${i + 1}/${total}) 正在处理 @${username}...`, 'warning');
            
            // 检查博主状态
            const status = await checkBloggerStatus(username);
            
            // 如果博主已被放弃，自动恢复
            if (status && status.exists && status.abandoned) {
                const restoreResponse = await fetch(`/api/instagraph/blogger/${username}/restore`, {
                    method: 'PUT'
                });
                if (!restoreResponse.ok) throw new Error('恢复失败');
            }

            const response = await fetch('/api/aggregate/' + username);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'API请求失败');
            }

            successCount++;
        } catch (error) {
            failCount++;
            showError(`添加 @${username} 失败: ${error.message}`);
            await new Promise(resolve => setTimeout(resolve, 500));
        }
    }

    showSuccess(`批量操作完成！成功 ${successCount} 个，失败 ${failCount} 个。`);

    // 清空输入框
    document.getElementById('queue-aggregate-username-list').value = '';

    // 刷新队列状态
    loadQueueStatus();
}

// 重新采集失败的任务
async function retryAggregation(username) {
    // 检查博主状态
    const status = await checkBloggerStatus(username);
    
    // 如果博主已被放弃，询问是否恢复并继续采集
    if (status && status.exists && status.abandoned) {
        const reasonText = status.abandonedReason ? `\n\n放弃原因：${status.abandonedReason}` : '';
        const confirmed = await showConfirm(
            `该博主已被标记为放弃状态。${reasonText}\n\n是否要恢复该博主并重新采集数据？`,
            '博主已放弃',
            '⚠️'
        );
        
        if (!confirmed) {
            return;
        }
    } else {
        // 没有被放弃，正常确认
        const confirmed = await showConfirm(
            `确定要重新采集 @${username} 的数据吗？`,
            '重新采集',
            '🔄'
        );

        if (!confirmed) {
            return;
        }
    }

    try {
        const response = await fetch('/api/aggregate/' + username);
        const data = await response.json();

        if (!response.ok) {
            showError(data.error || '添加任务失败');
            return;
        }

        showSuccess(`任务已重新加入队列：@${username}`);

        // 刷新队列状态
        loadQueueStatus();
    } catch (error) {
        showError('添加任务失败：' + error.message);
    }
}

// 取消任务
async function cancelTask(taskId) {
    const confirmed = await showConfirm(
        '确定要取消这个待执行任务吗？',
        '取消任务',
        '⚠️'
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`/api/aggregate/queue/task/${taskId}`, {
            method: 'DELETE'
        });
        const data = await response.json();

        if (!response.ok) {
            showError(data.error || '取消任务失败');
            return;
        }

        showSuccess('任务已取消');
        loadQueueStatus();
    } catch (error) {
        showError('取消任务失败：' + error.message);
    }
}

// 更新博主分组
async function updateBloggerGroup(username, newGroup) {
    try {
        // 调用现有的 blogger API
        const response = await fetch('/api/instagraph/blogger', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: username,
                seedGroup: newGroup || null
            })
        });

        if (!response.ok) {
            throw new Error('更新失败');
        }

        showSuccess(`已将 @${username} 的分组更新为 ${newGroup || '未分组'}`);

        // 重新加载博主列表以反映更改
        await loadAllBloggers();
    } catch (error) {
        showError('更新分组失败：' + error.message);
        // 出错时重新加载以恢复下拉框状态
        await loadAllBloggers();
    }
}

// 删除博主
async function deleteBlogger(username) {
    const confirmed = await showConfirm(
        `确定要删除博主 @${username} 吗？<br><br>⚠️ 此操作将删除该博主及其所有关系，不可恢复！`,
        '删除博主',
        '🗑️'
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(`/api/instagraph/blogger/${encodeURIComponent(username)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || '删除失败');
        }

        showSuccess(`博主 @${username} 已删除`);

        // 重新加载博主列表
        await loadAllBloggers();
    } catch (error) {
        showError('删除博主失败：' + error.message);
    }
}

// 加载所有博主（改用后端分页）
async function loadAllBloggers() {
    // 更新分组筛选选项
    updateFilterGroupOptions();

    // 重置到第一页并加载数据
    state.currentPage = 1;
    await loadBloggersPage();
}

// 更新筛选分组选项
function updateFilterGroupOptions() {
    const select = document.getElementById('filter-group');
    if (!select) return;

    const currentValue = select.value;

    // 重建选项
    select.innerHTML = `
        <option value="">-- 所有分组 --</option>
        <option value="__NO_GROUP__">未分组</option>
    `;

    // 添加现有分组
    Array.from(state.allProjects).forEach(group => {
        const option = document.createElement('option');
        option.value = group;
        option.textContent = group;
        if (group === currentValue) option.selected = true;
        select.appendChild(option);
    });
}

// 筛选博主（改用后端分页API）
async function filterBloggers() {
    state.currentPage = 1; // 重置到第一页
    await loadBloggersPage();
}

// 从后端加载当前页的博主数据
async function loadBloggersPage() {
    try {
        const keyword = document.getElementById('filter-username')?.value.trim() || '';
        const seedGroup = document.getElementById('filter-group')?.value || '';
        const abandoned = document.getElementById('filter-abandoned')?.value || '';

        // 构建查询参数
        const params = new URLSearchParams({
            page: state.currentPage,
            size: state.pageSize
        });
        if (keyword) params.append('keyword', keyword);
        if (seedGroup) params.append('seedGroup', seedGroup);
        if (abandoned !== '') params.append('abandoned', abandoned);

        // 调用后端分页接口（使用包含被标记帖子数量的端点）
        const response = await fetch(`/api/instagraph/bloggers/page?${params}`);
        if (!response.ok) {
            throw new Error('加载失败');
        }

        const pageResponse = await response.json();

        // 渲染表格
        renderBloggersTable(pageResponse.content);

        // 渲染分页控件
        renderPagination(pageResponse.totalElements, pageResponse.totalPages);

        // 更新统计信息
        const isFiltered = keyword || seedGroup || abandoned !== '';
        updateBloggerStats(pageResponse.totalElements, pageResponse.totalElements, isFiltered);
    } catch (error) {
        console.error('加载博主失败：', error);
        document.getElementById('bloggers-table-body').innerHTML =
            '<tr><td colspan="8" class="empty-state">加载失败</td></tr>';
        const paginationContainer = document.getElementById('pagination-container');
        if (paginationContainer) {
            paginationContainer.style.display = 'none';
        }
    }
}

// 渲染博主表格
function renderBloggersTable(bloggerData) {
    const tbody = document.getElementById('bloggers-table-body');

    if (!bloggerData || bloggerData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="empty-state">暂无数据</td></tr>';
        return;
    }

    tbody.innerHTML = bloggerData.map(data => {
        // 处理 BloggerWithTagCount DTO 结构
        const blogger = data.blogger || data;
        const taggedPostCount = data.taggedPostCount !== undefined ? data.taggedPostCount : '-';

        // 状态显示和操作按钮
        const isAbandoned = blogger.abandoned === true;
        const statusDisplay = isAbandoned
            ? `<span style="color: var(--warning); cursor: help;" title="${blogger.abandonedReason ? '原因：' + blogger.abandonedReason : '已放弃'}">⛔ 已放弃</span>`
            : '<span style="color: var(--success);">✓ 活跃</span>';

        // 种子组显示（显示文本而不是下拉框）
        const seedGroupDisplay = blogger.seedGroup
            ? `<span style="color: var(--primary); font-weight: 500;">${blogger.seedGroup}</span>`
            : '<span style="color: var(--gray);">未分组</span>';

        const actionButtons = isAbandoned
            ? `<div style="display: flex; flex-wrap: wrap; gap: 4px;">
               <button class="btn btn-sm" style="background: #06b6d4; color: white; font-weight: 500;" onclick="restoreBlogger('${blogger.username}')">恢复</button>
               <button class="btn btn-sm" style="background: var(--danger); color: white; font-weight: 500;" onclick="deleteBlogger('${blogger.username}')">删除</button>
               </div>`
            : `<div style="display: flex; flex-wrap: wrap; gap: 4px;">
               <button class="btn btn-sm btn-success" onclick="promoteToSeedFromDataTab('${blogger.username}')" style="margin-right: 5px; margin-bottom: 4px;">晋升</button>
               <button class="btn btn-sm btn-primary" onclick="showSwitchGroupDialog('${blogger.username}', '${blogger.seedGroup || ''}')" style="margin-right: 5px; margin-bottom: 4px;">切换分组</button>
               <button class="btn btn-sm btn-primary" id="sync-btn-${blogger.username}" onclick="aggregateUserData('${blogger.username}')" style="margin-right: 5px; margin-bottom: 4px;">同步</button>
               <button class="btn btn-sm" style="background: var(--warning); color: white; margin-right: 5px; margin-bottom: 4px;" onclick="abandonBlogger('${blogger.username}')">放弃</button>
               <button class="btn btn-sm" style="background: var(--danger); color: white; margin-bottom: 4px;" onclick="deleteBlogger('${blogger.username}')">删除</button>
               </div>`;

        return `
            <tr ${isAbandoned ? 'style="background-color: #fff8f0;"' : ''}>
                <td><a href="https://www.instagram.com/${blogger.username}/" target="_blank" class="username-link">@${blogger.username}</a></td>
                <td>${blogger.fullName || '-'}</td>
                <td>${seedGroupDisplay}</td>
                <td>${statusDisplay}</td>
                <td style="text-align: center; font-weight: 500;">
                    <a href="javascript:void(0)" onclick="showTaggedPostsForBlogger('${blogger.username}')" style="color: var(--primary); text-decoration: underline;">
                        ${taggedPostCount}
                    </a>
                </td>
                <td style="text-align: center;">
                    <button class="btn btn-sm btn-outline" onclick="showConnectedSeedsFromDataTab('${blogger.username}', '${blogger.seedGroup || ''}')">查看</button>
                </td>
                <td style="text-align: center;">
                    <button class="btn btn-sm btn-outline" onclick="showCoTaggedPostsFromDataTab('${blogger.username}', '${blogger.seedGroup || ''}')">查看</button>
                </td>
                <td>${blogger.instagramId || '-'}</td>
                <td style="white-space: nowrap;" title="${blogger.abandonedReason || '-'}">${blogger.abandonedReason ? (blogger.abandonedReason.length > 20 ? blogger.abandonedReason.substring(0, 20) + '...' : blogger.abandonedReason) : '-'}</td>
                <td style="white-space: nowrap;" title="${blogger.seedReason || '-'}">${blogger.seedReason ? (blogger.seedReason.length > 20 ? blogger.seedReason.substring(0, 20) + '...' : blogger.seedReason) : '-'}</td>
                <td style="white-space: nowrap;" title="${blogger.aggregationReason || '-'}">${blogger.aggregationReason ? (blogger.aggregationReason.length > 30 ? blogger.aggregationReason.substring(0, 30) + '...' : blogger.aggregationReason) : '-'}</td>
                <td style="position: sticky; right: 0; background: ${isAbandoned ? '#fff8f0' : 'white'}; z-index: 5; box-shadow: -2px 0 4px rgba(0,0,0,0.05);">
                    ${actionButtons}
                </td>
            </tr>
        `;
    }).join('');
}

// 更新统计信息
function updateBloggerStats(filteredCount, totalCount, isFiltered) {
    const countEl = document.getElementById('blogger-count');
    const infoEl = document.getElementById('filter-info');

    if (!countEl || !infoEl) return;

    if (isFiltered) {
        countEl.textContent = `显示 ${filteredCount} 个博主`;
        infoEl.textContent = `（共 ${totalCount} 个）`;
    } else {
        countEl.textContent = `共 ${totalCount} 个博主`;
        infoEl.textContent = '';
    }
}

// 渲染分页控件
function renderPagination(totalItems, totalPages) {
    const pageInfoEl = document.getElementById('page-info');
    const pageNumbersEl = document.getElementById('page-numbers');
    const firstBtn = document.getElementById('first-page-btn');
    const prevBtn = document.getElementById('prev-page-btn');
    const nextBtn = document.getElementById('next-page-btn');
    const lastBtn = document.getElementById('last-page-btn');
    const paginationContainer = document.getElementById('pagination-container');

    if (totalItems === 0) {
        paginationContainer.style.display = 'none';
        return;
    }

    paginationContainer.style.display = 'flex';

    // 更新页面信息
    const startIndex = (state.currentPage - 1) * state.pageSize + 1;
    const endIndex = Math.min(state.currentPage * state.pageSize, totalItems);
    pageInfoEl.textContent = `显示第 ${startIndex}-${endIndex} 条，共 ${totalItems} 条`;

    // 更新按钮状态
    firstBtn.disabled = state.currentPage === 1;
    prevBtn.disabled = state.currentPage === 1;
    nextBtn.disabled = state.currentPage === totalPages || totalPages === 0;
    lastBtn.disabled = state.currentPage === totalPages || totalPages === 0;

    // 保存 totalPages 到 lastBtn 的 dataset，供 goToLastPage() 使用
    lastBtn.dataset.totalPages = totalPages;

    // 生成页码按钮
    const maxPageButtons = 5;
    let startPage = Math.max(1, state.currentPage - Math.floor(maxPageButtons / 2));
    let endPage = Math.min(totalPages, startPage + maxPageButtons - 1);

    if (endPage - startPage < maxPageButtons - 1) {
        startPage = Math.max(1, endPage - maxPageButtons + 1);
    }

    let pageButtonsHtml = '';
    for (let i = startPage; i <= endPage; i++) {
        const isActive = i === state.currentPage;
        pageButtonsHtml += `
            <button class="btn btn-sm ${isActive ? 'btn-primary' : 'btn-outline'}"
                    onclick="goToPage(${i})"
                    ${isActive ? 'disabled' : ''}>
                ${i}
            </button>
        `;
    }

    pageNumbersEl.innerHTML = pageButtonsHtml;
}

// 分页导航函数（改用后端API）
async function goToPage(page) {
    state.currentPage = page;
    await loadBloggersPage();
}

async function nextPage() {
    // totalPages will be determined by backend response
    state.currentPage++;
    await loadBloggersPage();
}

async function previousPage() {
    if (state.currentPage > 1) {
        state.currentPage--;
        await loadBloggersPage();
    }
}

async function goToLastPage() {
    // Get totalPages from the pagination UI which was set by backend
    const lastPageBtn = document.getElementById('last-page-btn');
    if (lastPageBtn && lastPageBtn.dataset.totalPages) {
        state.currentPage = parseInt(lastPageBtn.dataset.totalPages);
        await loadBloggersPage();
    }
}

async function changePageSize() {
    state.pageSize = parseInt(document.getElementById('page-size').value);
    state.currentPage = 1;
    await loadBloggersPage();
}

// 加载所有分组
async function loadAllProjects() {
    try {
        // 从带统计信息的分组API加载
        const response = await fetch('/api/instagraph/groups/with-stats');
        const groups = await response.json();

        state.allProjects.clear();
        state.projectStats = {}; // 存储分组统计信息
        groups.forEach(group => {
            state.allProjects.add(group.name);
            state.projectStats[group.name] = {
                bloggerCount: group.bloggerCount
            };
        });

        updateProjectSelects();
    } catch (error) {
        console.error('加载分组失败：', error);
    }
}

// 更新项目选择框
function updateProjectSelects() {
    const selects = [
        document.getElementById('current-project'),
        document.getElementById('analysis-project')
    ];

    selects.forEach(select => {
        const currentValue = select.value;
        select.innerHTML = '<option value="">-- 选择分组 --</option>';

        Array.from(state.allProjects).forEach(project => {
            const option = document.createElement('option');
            option.value = project;
            // 显示分组名称和种子数
            const stats = state.projectStats[project];
            const seedCount = stats ? stats.bloggerCount : 0;
            option.textContent = `${project} (${seedCount} 个种子)`;
            if (project === currentValue) option.selected = true;
            select.appendChild(option);
        });
    });
}

// 填充分析项目列表
function populateAnalysisProjects() {
    updateProjectSelects();
}

// 加载分组的种子博主
async function loadProjectSeeds() {
    const project = document.getElementById('current-project').value;
    if (!project) return;

    state.currentProject = project;

    try {
        // 只更新种子数量，不加载完整列表
        const stats = state.projectStats[project];
        if (stats) {
            state.seedBloggers = [];  // 清空种子列表
            renderSeedList();
            // 更新种子数量显示
            const countEl = document.getElementById('seed-count');
            if (countEl) {
                countEl.textContent = `${stats.bloggerCount} 个种子`;
            }
        }
    } catch (error) {
        console.error('加载种子失败：', error);
    }
}

// 渲染分组列表
async function renderProjectsList() {
    const container = document.getElementById('projects-list');

    if (state.allProjects.size === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📂</div>
                <p>暂无分组，请先创建一个分组</p>
            </div>
        `;
        return;
    }

    // 使用新的带统计信息的接口
    try {
        const response = await fetch('/api/instagraph/groups/with-stats');
        const groupsWithStats = await response.json();

        container.innerHTML = groupsWithStats.map(group => `
            <div class="project-card" onclick="selectProject('${group.name}')">
                <div class="project-name">${group.name}</div>
                <div class="project-stats">
                    <span>🌱 ${group.bloggerCount} 个种子</span>
                </div>
                <div class="project-actions">
                    <button class="btn btn-sm" style="background: rgba(255,255,255,0.2); color: white;"
                            onclick="event.stopPropagation(); viewProjectAnalysis('${group.name}')">
                        查看分析
                    </button>
                    ${group.bloggerCount === 0 ? `
                        <button class="btn btn-sm" style="background: var(--danger); color: white;"
                                onclick="event.stopPropagation(); deleteProject('${group.name}')">
                            删除
                        </button>
                    ` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('渲染分组列表失败：', error);
    }
}

// 选择分组
function selectProject(projectName) {
    state.currentProject = projectName;
    document.getElementById('current-project').value = projectName;
    loadProjectSeeds();
    switchTab('workflow');
    activateStep(2);
}

// 查看分组分析
function viewProjectAnalysis(projectName) {
    document.getElementById('analysis-project').value = projectName;
    switchTab('analysis');
}

// 删除分组
async function deleteProject(projectName) {
    const confirmed = await showConfirm(
        `确定要删除分组 "${projectName}" 吗？<br><br>⚠️ 此操作不可恢复！`,
        '删除分组',
        '🗑️'
    );

    if (!confirmed) {
        return;
    }

    try {
        // 调用后端API删除分组
        const response = await fetch(`/api/instagraph/groups/${encodeURIComponent(projectName)}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || '删除失败');
        }

        // 从状态中删除
        state.allProjects.delete(projectName);

        // 如果当前选中的是被删除的分组，清空当前分组
        if (state.currentProject === projectName) {
            state.currentProject = null;
            state.seedBloggers = [];
        }

        // 更新UI
        updateProjectSelects();
        renderProjectsList();

        showSuccess(`分组 "${projectName}" 已删除`);
    } catch (error) {
        showError('删除分组失败：' + error.message);
    }
}

// UI辅助函数
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const icons = {
        success: '✓',
        error: '✕',
        warning: '⚠'
    };

    toast.innerHTML = `
        <div class="toast-icon">${icons[type] || icons.success}</div>
        <div class="toast-content">${message}</div>
        <button class="toast-close" onclick="this.parentElement.remove()">×</button>
    `;

    container.appendChild(toast);

    // 自动移除
    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function showConfirm(message, title = '确认操作', icon = '⚠️') {
    return new Promise((resolve) => {
        const overlay = document.getElementById('confirm-overlay');
        const messageEl = document.getElementById('confirm-message');
        const titleEl = document.getElementById('confirm-title-text');
        const iconEl = document.getElementById('confirm-icon');
        const okBtn = document.getElementById('confirm-ok');
        const cancelBtn = document.getElementById('confirm-cancel');

        // 设置内容
        messageEl.innerHTML = message.replace(/\n/g, '<br>');
        titleEl.textContent = title;
        iconEl.textContent = icon;

        // 显示对话框
        overlay.classList.add('show');

        // 处理确认
        const handleOk = () => {
            cleanup();
            resolve(true);
        };

        // 处理取消
        const handleCancel = () => {
            cleanup();
            resolve(false);
        };

        // 清理事件监听器
        const cleanup = () => {
            overlay.classList.remove('show');
            okBtn.removeEventListener('click', handleOk);
            cancelBtn.removeEventListener('click', handleCancel);
            overlay.removeEventListener('click', handleOverlayClick);
        };

        // 点击遮罩层关闭
        const handleOverlayClick = (e) => {
            if (e.target === overlay) {
                handleCancel();
            }
        };

        // 绑定事件
        okBtn.addEventListener('click', handleOk);
        cancelBtn.addEventListener('click', handleCancel);
        overlay.addEventListener('click', handleOverlayClick);
    });
}

function showLoading(message) {
    const container = document.getElementById('analysis-results');
    if (container) {
        container.innerHTML = `
            <div class="loading" style="grid-column: 1 / -1;">
                <div class="loading-spinner"></div>
                <p style="margin-top: 20px; font-size: 1rem; color: var(--dark);">${message}</p>
            </div>
        `;
    }
}

function showSuccess(message) {
    showToast(message, 'success');
}

function showError(message) {
    showToast(message, 'error');
}

// 导出分析结果
function exportResults() {
    if (!state.currentAnalysisResults || state.currentAnalysisResults.length === 0) {
        showToast('没有可导出的结果', 'warning');
        return;
    }

    // 创建模态框选择导出格式
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 400px;">
            <div class="modal-header">
                <h3 style="margin: 0; color: var(--primary);">📥 导出结果</h3>
            </div>
            <div class="modal-body">
                <p style="margin-bottom: 15px;">选择导出格式：</p>
                <button class="btn btn-primary" onclick="exportAsCSV(); this.closest('.modal-overlay').remove();" style="width: 100%; margin-bottom: 10px;">
                    📊 导出为 CSV
                </button>
                <button class="btn btn-primary" onclick="exportAsJSON(); this.closest('.modal-overlay').remove();" style="width: 100%;">
                    📄 导出为 JSON
                </button>
            </div>
            <div class="modal-actions">
                <button class="btn btn-outline" onclick="this.closest('.modal-overlay').remove();">
                    取消
                </button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // 点击遮罩层关闭
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.remove();
        }
    });
}

// 导出为 CSV 格式
function exportAsCSV() {
    const data = state.currentAnalysisResults;
    const project = document.getElementById('analysis-project')?.value || 'analysis';
    const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    
    let csvContent = '';
    
    if (state.currentAnalysisType === 'enhanced') {
        // 增强分析结果的 CSV 格式
        csvContent = '排名,用户名,综合评分,种子覆盖人数,共同标记次数,Instagram链接\n';
        data.forEach((item, index) => {
            const rank = index + 1;
            const username = item.username || '';
            const score = item.compositeScore ? item.compositeScore.toFixed(1) : '0';
            const connectedSeeds = item.connectedSeeds || 0;
            const coTaggedCount = item.coTaggedCount || 0;
            const instagramUrl = `https://www.instagram.com/${username}/`;
            
            csvContent += `${rank},"${username}",${score},${connectedSeeds},${coTaggedCount},"${instagramUrl}"\n`;
        });
    } else {
        // 简单分析结果的 CSV 格式
        csvContent = '排名,用户名,计数,Instagram链接\n';
        data.forEach((item, index) => {
            const rank = index + 1;
            const username = item.username || '';
            const count = item.count || 0;
            const instagramUrl = `https://www.instagram.com/${username}/`;
            
            csvContent += `${rank},"${username}",${count},"${instagramUrl}"\n`;
        });
    }
    
    // 添加 BOM 以支持 Excel 正确显示中文
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `insta-graph-${project}-${timestamp}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showSuccess(`已导出 ${data.length} 条结果为 CSV 格式`);
}

// 导出为 JSON 格式
function exportAsJSON() {
    const data = state.currentAnalysisResults;
    const project = document.getElementById('analysis-project')?.value || 'analysis';
    const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    
    const exportData = {
        exportTime: new Date().toISOString(),
        project: project,
        analysisType: state.currentAnalysisType,
        totalCount: data.length,
        results: data.map((item, index) => {
            if (state.currentAnalysisType === 'enhanced') {
                return {
                    rank: index + 1,
                    username: item.username,
                    compositeScore: item.compositeScore,
                    connectedSeeds: item.connectedSeeds,
                    coTaggedCount: item.coTaggedCount,
                    instagramUrl: `https://www.instagram.com/${item.username}/`
                };
            } else {
                return {
                    rank: index + 1,
                    username: item.username,
                    count: item.count,
                    instagramUrl: `https://www.instagram.com/${item.username}/`
                };
            }
        })
    };
    
    const jsonContent = JSON.stringify(exportData, null, 2);
    const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `insta-graph-${project}-${timestamp}.json`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showSuccess(`已导出 ${data.length} 条结果为 JSON 格式`);
}

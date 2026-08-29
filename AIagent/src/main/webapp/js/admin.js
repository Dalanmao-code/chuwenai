(async function init() {
    try {
        const user = await API.getUserInfo();
        if (user.role !== 'admin') {
            window.location.href = 'index.html';
            return;
        }
        await loadStats();
    } catch (e) {
        window.location.href = 'index.html';
    }
})();

async function loadStats() {
    try {
        const stats = await API.getAdminStats();
        animateNumber('statUsers', stats.totalUsers);
        animateNumber('statConvs', stats.totalConversations);
        animateNumber('statTotalTokens', stats.totalTokens);
        animateNumber('statTodayTokens', stats.todayTokens);
        renderUserTable(stats.userSummaries, stats.totalTokens);
    } catch (e) {
        showToast('加载统计数据失败', 'error');
    }
}

function renderUserTable(summaries, totalTokens) {
    const tbody = document.getElementById('userTableBody');
    if (!summaries || summaries.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#b2bec3;padding:40px;">暂无数据</td></tr>';
        return;
    }
    const maxTokens = totalTokens > 0 ? totalTokens : 1;
    tbody.innerHTML = summaries.map(s => {
        const pct = ((s.totalTokens / maxTokens) * 100).toFixed(1);
        return `
        <tr>
            <td>#${s.userId}</td>
            <td><strong>${escapeHtml(s.username)}</strong></td>
            <td>${s.conversationCount}</td>
            <td>${s.apiCalls}</td>
            <td>${s.totalTokens.toLocaleString()}</td>
            <td>
                <div style="display:flex;align-items:center;gap:8px;">
                    <span style="font-size:12px;color:#636e72;">${pct}%</span>
                    <div class="progress-bar" style="flex:1;">
                        <div class="fill" style="width:${pct}%;"></div>
                    </div>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function animateNumber(id, target) {
    const el = document.getElementById(id);
    const start = 0;
    const duration = 800;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        // Ease out cubic
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = Math.round(eased * target);
        el.textContent = current.toLocaleString();
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    requestAnimationFrame(update);
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function showToast(msg, type) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = 'toast ' + (type || 'info');
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => { toast.remove(); }, 3000);
}

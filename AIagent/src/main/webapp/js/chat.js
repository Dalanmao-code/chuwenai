let currentUser = null;
let currentConv = null;
let conversations = [];

// Init
(async function init() {
    try {
        currentUser = await API.getUserInfo();
        document.getElementById('sidebarName').textContent = currentUser.username;
        const avatar = document.getElementById('sidebarAvatar');
        avatar.style.background = currentUser.avatarColor || '#C53D43';
        avatar.textContent = currentUser.username.charAt(0).toUpperCase();
        document.getElementById('promptInput').value = currentUser.systemPrompt || '';
        await loadConversations();
    } catch (e) {
        window.location.href = 'index.html';
    }
})();

async function loadConversations() {
    try {
        conversations = await API.getConversations();
        renderConvList();
    } catch (e) {
        showToast('加载对话列表失败', 'error');
    }
}

function renderConvList() {
    const container = document.getElementById('convList');
    if (conversations.length === 0) {
        container.innerHTML = '<div style="text-align:center;color:rgba(255,255,255,0.2);padding:40px 0;font-size:13px;">暂无对话<br>点击上方按钮开始</div>';
        return;
    }
    container.innerHTML = conversations.map(c => `
        <div class="conv-item${currentConv && currentConv.id === c.id ? ' active' : ''}"
             onclick="selectConversation(${c.id})">
            <div class="conv-icon">&#128172;</div>
            <div class="conv-info">
                <div class="conv-title">${escapeHtml(c.title || '新的对话')}</div>
                <div class="conv-time">${formatTime(c.updatedAt)}</div>
            </div>
            <button class="conv-delete" onclick="event.stopPropagation();deleteConversation(${c.id})"
                title="删除">&#10005;</button>
        </div>
    `).join('');
}

async function createNewChat() {
    try {
        const conv = await API.createConversation('新的对话');
        conversations.unshift(conv);
        renderConvList();
        selectConversation(conv.id);
    } catch (e) {
        showToast('创建对话失败', 'error');
    }
}

async function selectConversation(id) {
    currentConv = conversations.find(c => c.id === id);
    if (!currentConv) return;

    document.getElementById('convTitle').textContent = currentConv.title || '新的对话';
    document.getElementById('userInput').disabled = false;
    document.getElementById('sendBtn').disabled = false;
    renderConvList();

    try {
        const messages = await API.getMessages(id);
        renderMessages(messages);
    } catch (e) {
        showToast('加载消息失败', 'error');
    }
}

async function deleteConversation(id) {
    if (!confirm('确定删除此对话？')) return;
    try {
        await API.deleteConversation(id);
        conversations = conversations.filter(c => c.id !== id);
        if (currentConv && currentConv.id === id) {
            currentConv = null;
            document.getElementById('convTitle').textContent = '灵感顾问';
            document.getElementById('messageArea').innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">&#9758;</div>
                    <p>选择或创建一个对话开始与灵感顾问交流</p>
                </div>`;
            document.getElementById('userInput').disabled = true;
            document.getElementById('sendBtn').disabled = true;
        }
        renderConvList();
        showToast('对话已删除', 'info');
    } catch (e) {
        showToast('删除失败', 'error');
    }
}

function renderMessages(messages) {
    const area = document.getElementById('messageArea');
    if (!messages || messages.length === 0) {
        area.innerHTML = `
            <div class="empty-state" style="height:auto;padding-top:60px;">
                <div class="empty-icon">&#9758;</div>
                <p>开始一段关于纹样的对话吧</p>
            </div>`;
        return;
    }

    let html = '';
    let lastDate = '';
    for (const msg of messages) {
        const date = formatDate(msg.createdAt);
        if (date !== lastDate) {
            html += `<div class="msg-time">${date}</div>`;
            lastDate = date;
        }
        if (msg.role === 'user') {
            html += `
            <div class="message-row user">
                <div class="msg-avatar" style="background:${currentUser.avatarColor || '#C53D43'};color:#fff;">
                    ${currentUser.username.charAt(0).toUpperCase()}
                </div>
                <div class="msg-bubble">${formatContent(msg.content)}</div>
            </div>`;
        } else {
            html += `
            <div class="message-row assistant">
                <div class="msg-avatar ai-avatar">纹</div>
                <div class="msg-bubble">${formatContent(msg.content)}</div>
            </div>`;
        }
    }
    area.innerHTML = html;
    area.scrollTop = area.scrollHeight;
}

async function sendMessage() {
    const input = document.getElementById('userInput');
    const content = input.value.trim();
    if (!content || !currentConv) return;

    input.value = '';
    input.style.height = 'auto';
    document.getElementById('sendBtn').disabled = true;
    input.disabled = true;

    const area = document.getElementById('messageArea');
    if (area.querySelector('.empty-state')) area.innerHTML = '';

    area.innerHTML += `
        <div class="message-row user">
            <div class="msg-avatar" style="background:${currentUser.avatarColor || '#C53D43'};color:#fff;">
                ${currentUser.username.charAt(0).toUpperCase()}
            </div>
            <div class="msg-bubble">${formatContent(content)}</div>
        </div>`;

    const typingEl = document.createElement('div');
    typingEl.className = 'typing-indicator';
    typingEl.innerHTML = `
        <div class="msg-avatar ai-avatar">纹</div>
        <div class="typing-dots"><span></span><span></span><span></span></div>`;
    area.appendChild(typingEl);
    area.scrollTop = area.scrollHeight;

    try {
        const aiMsg = await API.sendMessage(currentConv.id, content);
        typingEl.remove();

        area.innerHTML += `
            <div class="message-row assistant">
                <div class="msg-avatar ai-avatar">纹</div>
                <div class="msg-bubble">${formatContent(aiMsg.content)}</div>
            </div>`;
        area.scrollTop = area.scrollHeight;

        await loadConversations();
        const updatedConv = conversations.find(c => c.id === currentConv.id);
        if (updatedConv) {
            currentConv = updatedConv;
            document.getElementById('convTitle').textContent = currentConv.title || '新的对话';
        }
    } catch (e) {
        typingEl.remove();
        showToast('发送失败: ' + e.message, 'error');
    }

    document.getElementById('sendBtn').disabled = false;
    input.disabled = false;
    input.focus();
}

function handleInputKey(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
    setTimeout(() => {
        e.target.style.height = 'auto';
        e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
    }, 0);
}

async function savePrompt() {
    const prompt = document.getElementById('promptInput').value.trim();
    if (!prompt) return;
    try {
        await API.updatePrompt(prompt);
        currentUser.systemPrompt = prompt;
        showToast('提示词已保存', 'success');
        closePromptSettings();
    } catch (e) {
        showToast('保存失败: ' + e.message, 'error');
    }
}

function openPromptSettings() {
    document.getElementById('promptModal').style.display = 'flex';
}

function closePromptSettings() {
    document.getElementById('promptModal').style.display = 'none';
}

async function handleLogout() {
    try { await API.logout(); } catch (e) {}
    window.location.href = 'index.html';
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

function switchToChat() {
    // Already on chat page
}

// Helpers
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function formatContent(text) {
    let escaped = escapeHtml(text);
    escaped = escaped.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
    escaped = escaped.replace(/`([^`]+)`/g, '<code>$1</code>');
    escaped = escaped.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    escaped = escaped.replace(/\n/g, '<br>');
    return escaped;
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    const diff = now - d;
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    return (d.getMonth() + 1) + '/' + d.getDate();
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) return '今天';
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    if (d.toDateString() === yesterday.toDateString()) return '昨天';
    return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日';
}

function showToast(msg, type) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = 'toast ' + (type || 'info');
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => { toast.remove(); }, 3000);
}

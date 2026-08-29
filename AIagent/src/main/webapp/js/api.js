// Auto-detect context path from current page URL
const BASE = (function() {
    var p = window.location.pathname;
    var ctx = p.substring(0, p.lastIndexOf('/'));
    if (ctx.endsWith('/js')) ctx = ctx.substring(0, ctx.lastIndexOf('/'));
    return ctx;
})();

const API = {
    async request(method, url, body) {
        const opts = {
            method,
            headers: { 'Content-Type': 'application/json' },
        };
        if (body) opts.body = JSON.stringify(body);
        const resp = await fetch(BASE + url, opts);
        const data = await resp.json();
        if (!resp.ok) throw new Error(data.error || 'Request failed');
        return data;
    },

    register(username, password) {
        return this.request('POST', '/api/register', { username, password });
    },
    login(username, password) {
        return this.request('POST', '/api/login', { username, password });
    },
    logout() {
        return this.request('POST', '/api/logout');
    },
    getUserInfo() {
        return this.request('GET', '/api/user/info');
    },
    updatePrompt(systemPrompt) {
        return this.request('PUT', '/api/user/prompt', { systemPrompt });
    },
    getConversations() {
        return this.request('GET', '/api/conversations');
    },
    createConversation(title) {
        return this.request('POST', '/api/conversations', { title });
    },
    deleteConversation(id) {
        return this.request('DELETE', '/api/conversations/' + id);
    },
    getMessages(convId) {
        return this.request('GET', '/api/conversations/' + convId + '/messages');
    },
    sendMessage(conversationId, content) {
        return this.request('POST', '/api/chat', { conversationId, content });
    },
    getPatterns(category) {
        const url = category ? '/api/patterns?category=' + encodeURIComponent(category) : '/api/patterns';
        return this.request('GET', url);
    },
    getPatternCategories() {
        return this.request('GET', '/api/patterns/categories');
    },
    generateImage(prompt, referenceImagePath) {
        return this.request('POST', '/api/generate-image', { prompt, referenceImagePath });
    },
    generateImageWithBase64(prompt, referenceImageBase64) {
        return this.request('POST', '/api/generate-image', { prompt, referenceImageBase64 });
    },
    generateImageAdvanced(prompt, referenceImagePath, size, sampleImageBase64) {
        var body = { prompt: prompt, size: size };
        if (referenceImagePath) { body.referenceImagePath = referenceImagePath; }
        if (sampleImageBase64) { body.sampleImageBase64 = sampleImageBase64; }
        return this.request('POST', '/api/generate-image', body);
    },
    getSamples(category) {
        const url = category ? '/api/samples?category=' + encodeURIComponent(category) : '/api/samples';
        return this.request('GET', url);
    },
    getSampleCategories() {
        return this.request('GET', '/api/samples/categories');
    },
    getAdminStats() {
        return this.request('GET', '/api/admin/stats');
    },
    getAdminUsers() {
        return this.request('GET', '/api/admin/users');
    },
    generateVideo(params) {
        return this.request('POST', '/api/generate-video', params);
    },
    getVideoTaskStatus(taskId) {
        return this.request('GET', '/api/generate-video/status?taskId=' + encodeURIComponent(taskId));
    }
};

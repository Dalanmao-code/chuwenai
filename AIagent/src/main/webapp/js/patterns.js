let currentUser = null;
let patterns = [];
let allPatterns = [];
let selectedPattern = null;
let lastGeneratedBase64 = null;
let progressTimer = null;
let currentMode = 'design';
let selectedSample = null;
let samples = [];
let allSamples = [];

// Init
(async function init() {
    try {
        currentUser = await API.getUserInfo();
        document.getElementById('sidebarName').textContent = currentUser.username;
        const avatar = document.getElementById('sidebarAvatar');
        avatar.style.background = currentUser.avatarColor || '#C53D43';
        avatar.textContent = currentUser.username.charAt(0).toUpperCase();
        setupChoiceButtons();
        await loadCategories();
        await loadPatterns();
        await loadSamples();
    } catch (e) {
        window.location.href = 'index.html';
    }
})();

function setupChoiceButtons() {
    var groups = ['resolutionGroup', 'styleGroup', 'colorGroup', 'densityGroup'];
    groups.forEach(function(groupId) {
        var group = document.getElementById(groupId);
        if (!group) return;
        group.addEventListener('click', function(e) {
            var btn = e.target.closest('.choice-btn');
            if (!btn) return;
            var buttons = group.querySelectorAll('.choice-btn');
            buttons.forEach(function(b) { b.classList.remove('active'); });
            btn.classList.add('active');
        });
    });
}

function getChoiceValue(groupId) {
    var group = document.getElementById(groupId);
    if (!group) return '';
    var activeBtn = group.querySelector('.choice-btn.active');
    return activeBtn ? (activeBtn.getAttribute('data-value') || '') : '';
}

async function loadCategories() {
    try {
        const categories = await API.getPatternCategories();
        const select = document.getElementById('galleryCategorySelect');
        if (!select) return;
        categories.forEach(function(cat) {
            const opt = document.createElement('option');
            opt.value = cat.name;
            opt.textContent = cat.name + ' (' + cat.count + ')';
            select.appendChild(opt);
        });
    } catch (e) {}
}

async function loadPatterns(category) {
    try {
        allPatterns = await API.getPatterns(category || '');
        patterns = allPatterns;
        renderPatternGrid();
    } catch (e) {
        showToast('加载纹样库失败: ' + e.message, 'error');
        var grid = document.getElementById('patternGrid');
        if (grid) {
            grid.innerHTML =
                '<div style="grid-column:1/-1;text-align:center;padding:40px;color:var(--text-light);">纹样库加载失败</div>';
        }
    }
}

function onCategoryChange() {
    const cat = document.getElementById('galleryCategorySelect').value;
    loadPatterns(cat);
}

function renderPatternGrid() {
    const grid = document.getElementById('patternGrid');
    if (!grid) return;
    if (patterns.length === 0) {
        grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:40px;color:var(--text-light);">暂无纹样图片</div>';
        return;
    }
    grid.innerHTML = patterns.map(p => `
        <div class="pattern-thumb${selectedPattern && selectedPattern.filename === p.filename ? ' selected' : ''}"
             onclick="selectPattern('${p.filename}', '${p.path}', '${escapeHtml(p.name)}')">
            <img src="${BASE + p.path}" alt="${escapeHtml(p.name)}" loading="lazy">
            <div class="thumb-name">${escapeHtml(p.name)}</div>
        </div>
    `).join('');
}

function selectPattern(filename, path, name) {
    selectedPattern = { filename, path, name };
    renderPatternGrid();
    document.getElementById('noSelection').style.display = 'none';
    const previewImg = document.getElementById('previewImage');
    previewImg.src = BASE + path;
    previewImg.style.display = 'block';
    updateGenerateButton();
}

function startProgressBar() {
    const fill = document.getElementById('progressFill');
    fill.style.animation = 'none';
    fill.style.width = '0%';
    let percent = 0;
    clearInterval(progressTimer);
    progressTimer = setInterval(function () {
        // speed: fast at first, slow down as it approaches 90%
        percent += (90 - percent) * 0.08 + 0.3;
        if (percent >= 89.5) percent = 89.5;
        fill.style.width = percent + '%';
    }, 300);
}

function finishProgressBar() {
    clearInterval(progressTimer);
    const fill = document.getElementById('progressFill');
    fill.style.width = '100%';
    setTimeout(function () {
        fill.style.width = '0%';
    }, 600);
}

async function loadSamples() {
    var grid = document.getElementById('templateGrid');
    if (!grid) return;
    try {
        allSamples = await API.getSamples();
        samples = allSamples;
        await loadSampleCategories();
        renderTemplateGrid();
    } catch (e) {
        grid.innerHTML =
            '<div style="grid-column:1/-1;text-align:center;padding:20px;color:var(--text-light);">模板加载失败</div>';
    }
}

async function loadSampleCategories() {
    try {
        var categories = await API.getSampleCategories();
        var select = document.getElementById('sampleCategorySelect');
        if (!select) return;
        select.innerHTML = '<option value="">全部类别</option>';
        categories.forEach(function(cat) {
            var opt = document.createElement('option');
            opt.value = cat.name;
            opt.textContent = cat.name + ' (' + cat.count + ')';
            select.appendChild(opt);
        });
    } catch (e) {}
}

function onSampleCategoryChange() {
    var cat = document.getElementById('sampleCategorySelect').value;
    if (cat) {
        samples = allSamples.filter(function(s) { return s.category === cat; });
    } else {
        samples = allSamples;
    }
    selectedSample = null;
    renderTemplateGrid();
    updateGenerateButton();
}

function renderTemplateGrid() {
    const grid = document.getElementById('templateGrid');
    const btn = document.getElementById('btnExpandSamples');
    if (!grid) return;
    if (samples.length === 0) {
        grid.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:20px;color:var(--text-light);">暂无样品图片</div>';
        if (btn) btn.style.display = 'none';
        return;
    }
    grid.innerHTML = samples.map(function(s) { return `
        <div class="template-thumb${selectedSample && selectedSample.filename === s.filename ? ' selected' : ''}"
             onclick="selectSample('${s.filename}', '${s.path}', '${escapeHtml(s.name)}', '${escapeHtml(s.category || '')}')">
            <img src="${BASE + s.path}" alt="${escapeHtml(s.name)}" loading="lazy">
            <div class="thumb-name">${escapeHtml(s.name)}</div>
            <div class="thumb-category">${escapeHtml(s.category || '')}</div>
        </div>
    `; }).join('');
    grid.classList.add('collapsed');
    if (btn) {
        btn.style.display = samples.length > 6 ? '' : 'none';
        btn.innerHTML = '<span id="btnExpandSamplesIcon">▼</span> 展开更多';
    }
}

function toggleSampleExpand() {
    var grid = document.getElementById('templateGrid');
    var btn = document.getElementById('btnExpandSamples');
    var icon = document.getElementById('btnExpandSamplesIcon');
    if (!grid || !btn) return;
    var collapsed = grid.classList.contains('collapsed');
    if (collapsed) {
        grid.classList.remove('collapsed');
        icon.textContent = '▲';
        btn.innerHTML = '<span id="btnExpandSamplesIcon">▲</span> 收起';
    } else {
        grid.classList.add('collapsed');
        icon.textContent = '▼';
        btn.innerHTML = '<span id="btnExpandSamplesIcon">▼</span> 展开更多';
    }
}

function selectSample(filename, path, name, category) {
    selectedSample = { filename, path, name, category };
    renderTemplateGrid();
    updateGenerateButton();
}

function switchMode(mode) {
    currentMode = mode;
    var designBtn = document.getElementById('modeDesignBtn');
    var applyBtn = document.getElementById('modeApplyBtn');
    if (designBtn) designBtn.classList.toggle('active', mode === 'design');
    if (applyBtn) applyBtn.classList.toggle('active', mode === 'apply');

    var optionsDiv = document.getElementById('generateOptions');
    var promptSection = document.getElementById('promptSection');
    var promptTitle = document.getElementById('promptSectionTitle');
    var promptLabel = document.getElementById('promptLabel');
    var promptInput = document.getElementById('promptInput');
    var templateDiv = document.getElementById('templateLibrary');
    var generateBtn = document.getElementById('generateBtn');
    var toggleBtn = document.getElementById('btnTogglePrompt');

    if (mode === 'design') {
        if (optionsDiv) optionsDiv.style.display = 'flex';
        if (promptSection) promptSection.style.display = '';
        if (toggleBtn) toggleBtn.style.display = 'none';
        if (promptTitle) promptTitle.textContent = '三、描述修改效果';
        if (promptLabel) promptLabel.textContent = '你想要如何改变这个纹样？';
        if (promptInput) promptInput.placeholder = '例如：将纹样改为金色配色，增加云纹元素，背景改为深蓝色...';
        if (templateDiv) templateDiv.style.display = 'none';
        if (generateBtn) generateBtn.textContent = '生成纹样';
    } else {
        if (optionsDiv) optionsDiv.style.display = 'none';
        if (promptSection) promptSection.style.display = 'none';
        if (toggleBtn) toggleBtn.style.display = 'inline-flex';
        if (promptTitle) promptTitle.textContent = '二、描述应用效果（可选）';
        if (promptLabel) promptLabel.textContent = '对纹样覆盖效果有特殊要求吗？';
        if (promptInput) promptInput.placeholder = '例如：纹样放大一些、调整纹样角度、增加阴影效果...';
        if (templateDiv) templateDiv.style.display = 'flex';
        if (generateBtn) generateBtn.textContent = '应用场景';
    }
    updateGenerateButton();
}

function togglePrompt() {
    var section = document.getElementById('promptSection');
    var btn = document.getElementById('btnTogglePrompt');
    if (!section || !btn) return;
    var visible = section.style.display !== 'none';
    if (visible) {
        section.style.display = 'none';
        btn.textContent = '+ 展开描述选项';
    } else {
        section.style.display = '';
        btn.textContent = '收起描述选项';
    }
}

async function generateImage() {
    var prompt = document.getElementById('promptInput').value.trim();
    if (!selectedPattern) {
        showToast('请先在纹样库中选择一张纹样图片', 'error');
        return;
    }

    if (currentMode === 'apply') {
        if (!selectedSample) {
            showToast('请在模板库中选择一张样品图片', 'error');
            return;
        }
        if (!prompt) {
            prompt = '将纹样图案自然覆盖到样品上，保持样品的形状轮廓和光影效果';
        } else {
            prompt = '将纹样图案自然覆盖到样品上，保持样品的形状轮廓和光影效果。额外要求：' + prompt;
        }
    } else {
        if (!prompt) {
            showToast('请输入修改效果的描述', 'error');
            return;
        }
        var prefix = '';
        prefix += getChoiceValue('styleGroup');
        prefix += getChoiceValue('colorGroup');
        prefix += getChoiceValue('densityGroup');
        if (prefix) prompt = prefix + prompt;
    }

    var btn = document.getElementById('generateBtn');
    var indicator = document.getElementById('generatingIndicator');
    var noResult = document.getElementById('noResult');
    var resultImg = document.getElementById('resultImage');
    var resultActions = document.getElementById('resultActions');

    btn.disabled = true;
    btn.textContent = '生成中...';
    indicator.style.display = 'flex';
    noResult.style.display = 'none';
    resultImg.style.display = 'none';
    resultActions.style.display = 'none';
    startProgressBar();

    try {
        var size = currentMode === 'design' ? (getChoiceValue('resolutionGroup') || '2K') : '2K';
        var samplePath = (currentMode === 'apply' && selectedSample) ? selectedSample.path : null;
        var result;
        if (samplePath) {
            var sampleResp = await fetch(BASE + samplePath);
            var sampleBlob = await sampleResp.blob();
            var reader = new FileReader();
            var sampleBase64 = await new Promise(function(resolve) {
                reader.onloadend = function() {
                    var base64 = reader.result.split(',')[1];
                    resolve(base64);
                };
                reader.readAsDataURL(sampleBlob);
            });
            result = await API.generateImageAdvanced(prompt, selectedPattern.path, size, sampleBase64);
        } else {
            result = await API.generateImageAdvanced(prompt, selectedPattern.path, size, null);
        }
        lastGeneratedBase64 = result.imageBase64;
        finishProgressBar();
        resultImg.src = 'data:image/png;base64,' + result.imageBase64;
        resultImg.style.display = 'block';
        resultActions.style.display = 'flex';
        noResult.style.display = 'none';
        showToast(currentMode === 'apply' ? '应用场景生成成功！' : '纹样生成成功！', 'success');
    } catch (e) {
        clearInterval(progressTimer);
        showToast('生成失败: ' + e.message, 'error');
        noResult.style.display = 'block';
        noResult.textContent = '生成失败，请稍后重试';
    }

    btn.disabled = false;
    btn.textContent = currentMode === 'apply' ? '应用场景' : '生成纹样';
    indicator.style.display = 'none';
}

function updateGenerateButton() {
    var btn = document.getElementById('generateBtn');
    if (!selectedPattern) {
        btn.disabled = true;
        return;
    }
    if (currentMode === 'apply') {
        btn.disabled = !selectedSample;
    } else {
        btn.disabled = false;
    }
}

function downloadImage() {
    if (!lastGeneratedBase64) {
        showToast('没有可下载的图片', 'error');
        return;
    }
    const link = document.createElement('a');
    link.download = '纹样_' + new Date().toISOString().slice(0, 10) + '.png';
    link.href = 'data:image/png;base64,' + lastGeneratedBase64;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('下载成功', 'success');
}

async function handleLogout() {
    try { await API.logout(); } catch (e) {}
    window.location.href = 'index.html';
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

function toggleGallery() {
    document.getElementById('galleryPanel').classList.toggle('open');
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function showToast(msg, type) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = 'toast ' + (type || 'info');
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => { toast.remove(); }, 3000);
}

(() => {
    const form = document.getElementById('aiSettingsForm');
    if (!form) return;
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const notice = document.getElementById('settingsNotice');
    const showNotice = (message, success) => { notice.textContent = message; notice.className = `alert ${success ? 'alert-success' : 'alert-danger'}`; };
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const payload = {
            enabled: document.getElementById('enabled').checked,
            adminEnabled: document.getElementById('adminEnabled').checked,
            teacherEnabled: document.getElementById('teacherEnabled').checked,
            parentEnabled: document.getElementById('parentEnabled').checked,
            perUserDailyRequests: Number(document.getElementById('perUserDailyRequests').value),
            globalDailyRequests: Number(document.getElementById('globalDailyRequests').value),
            dailyTokenLimit: Number(document.getElementById('dailyTokenLimit').value),
            maxPromptCharacters: Number(document.getElementById('maxPromptCharacters').value),
            maxResponseTokens: Number(document.getElementById('maxResponseTokens').value),
            maxConcurrentRequests: Number(document.getElementById('maxConcurrentRequests').value),
            selectedModel: document.getElementById('selectedModel').value
        };
        try {
            const response = await fetch('/api/ai/admin/settings', { method: 'PUT', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', ...(token ? { [header]: token } : {}) }, body: JSON.stringify(payload) });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || 'Không thể lưu cài đặt.');
            showNotice('Đã lưu cài đặt Hùng Cường AI.', true);
        } catch (error) { showNotice(error.message, false); }
    });
})();

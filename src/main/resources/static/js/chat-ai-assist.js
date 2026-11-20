(() => {
    const chatWindow = document.querySelector('.chat-thread-window');
    const composer = document.querySelector('.chat-composer');
    const textarea = document.getElementById('messageContent');
    const status = document.getElementById('chatAiStatus');
    if (!chatWindow || !composer || !textarea) return;
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    document.querySelectorAll('[data-ai-chat-action]').forEach(button => button.addEventListener('click', async () => {
        const action = button.dataset.aiChatAction;
        if (!textarea.value.trim() && !['SUGGEST_REPLY'].includes(action)) {
            status.textContent = 'Nhập nội dung cần chỉnh sửa trước.'; textarea.focus(); return;
        }
        const previous = button.textContent;
        button.disabled = true; button.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang soạn';
        status.textContent = 'AI đang tạo bản nháp. Tin nhắn sẽ không tự động gửi.';
        try {
            const response = await fetch('/api/ai/chat-assist', { method: 'POST', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', ...(token ? { [header]: token } : {}) },
                body: JSON.stringify({ conversationId: Number(chatWindow.dataset.conversationId), action, draft: textarea.value }) });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || 'Không thể tạo bản nháp.');
            textarea.value = (result.draft || '').slice(0, 2000); textarea.dispatchEvent(new Event('input', { bubbles: true })); textarea.focus();
            status.textContent = 'Bản nháp đã được điền. Hãy kiểm tra, chỉnh sửa nếu cần và tự nhấn Gửi.';
        } catch (error) { status.textContent = error.message; }
        finally { button.disabled = false; button.textContent = previous; }
    }));
})();

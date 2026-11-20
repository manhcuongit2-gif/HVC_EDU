(() => {
    const app = document.getElementById('aiApp');
    if (!app) return;
    const role = app.dataset.role;
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const list = document.getElementById('conversationList');
    const search = document.getElementById('conversationSearch');
    const welcome = document.getElementById('welcomePanel');
    const messages = document.getElementById('messageList');
    const scrollArea = document.getElementById('aiScroll');
    const input = document.getElementById('chatInput');
    const form = document.getElementById('chatForm');
    const working = document.getElementById('workingIndicator');
    const sendButton = document.getElementById('sendButton');
    const stopButton = document.getElementById('stopGeneration');
    const context = document.getElementById('contextId');
    const resultPanel = document.getElementById('toolResultPanel');
    const resultContent = document.getElementById('toolResult');
    const contextId = () => context?.value ? Number(context.value) : null;
    let currentConversationId = null;
    let pendingEditMessageId = null;
    let controller = null;
    let resultTitle = 'Hùng Cường AI';
    let documentText = '';
    let documentName = '';
    let searchTimer = null;

    const api = async (url, options = {}) => {
        const headers = new Headers(options.headers || {});
        if (csrf && options.method && !['GET', 'HEAD'].includes(options.method.toUpperCase())) headers.set(csrfHeader, csrf);
        if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
        const response = await fetch(url, { credentials: 'same-origin', ...options, headers });
        if (!response.ok) {
            let detail = {};
            try { detail = await response.json(); } catch (_) {}
            throw new Error(detail.message || `Yêu cầu thất bại (${response.status})`);
        }
        return response;
    };

    const escapeHtml = (value) => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
    const renderMarkdown = (value) => {
        let html = window.marked?.parse ? window.marked.parse(value || '', { breaks: true, gfm: true }) : `<p>${escapeHtml(value || '').replace(/\n/g, '<br>')}</p>`;
        if (window.DOMPurify?.sanitize) html = window.DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
        else html = `<pre>${escapeHtml(value || '')}</pre>`;
        return html;
    };
    const maybeTypeset = (node) => {
        if (window.MathJax?.typesetPromise) window.MathJax.typesetPromise([node]).catch(() => {});
    };

    const scrollToBottom = () => {
        const closeToBottom = scrollArea.scrollHeight - scrollArea.scrollTop - scrollArea.clientHeight < 180;
        if (closeToBottom) requestAnimationFrame(() => { scrollArea.scrollTop = scrollArea.scrollHeight; });
    };

    const createMessage = (message, previousUserId) => {
        const isUser = message.senderRole === 'USER';
        const wrapper = document.createElement('article');
        wrapper.className = `ai-message ${isUser ? 'user' : 'assistant'}`;
        if (message.id) wrapper.dataset.messageId = message.id;
        const avatar = document.createElement('div');
        avatar.className = 'ai-message-avatar';
        avatar.innerHTML = isUser ? '<i class="bi bi-person"></i>' : '<i class="bi bi-stars"></i>';
        const body = document.createElement('div');
        body.className = 'ai-message-body';
        const content = document.createElement('div');
        content.className = 'ai-message-text';
        if (isUser) content.textContent = message.content || '';
        else { content.innerHTML = renderMarkdown(message.content || ''); maybeTypeset(content); }
        body.append(content);
        const actions = document.createElement('div');
        actions.className = 'ai-message-actions';
        if (isUser) {
            const edit = document.createElement('button'); edit.type = 'button'; edit.dataset.action = 'edit';
            edit.dataset.messageId = message.id || ''; edit.dataset.content = message.content || '';
            edit.innerHTML = '<i class="bi bi-pencil"></i> Chỉnh sửa'; actions.append(edit);
        } else {
            const copy = document.createElement('button'); copy.type = 'button'; copy.dataset.action = 'copy';
            copy.dataset.content = message.content || ''; copy.innerHTML = '<i class="bi bi-copy"></i> Sao chép'; actions.append(copy);
            if (previousUserId) {
                const retry = document.createElement('button'); retry.type = 'button'; retry.dataset.action = 'retry'; retry.dataset.messageId = previousUserId;
                retry.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Tạo lại'; actions.append(retry);
            }
        }
        if (actions.childElementCount) body.append(actions);
        wrapper.append(avatar, body);
        return wrapper;
    };

    const renderMessages = (items) => {
        messages.replaceChildren();
        let lastUserId = null;
        items.forEach(item => {
            if (item.senderRole === 'USER') lastUserId = item.id;
            messages.append(createMessage(item, lastUserId));
        });
        welcome.classList.add('d-none');
        messages.classList.remove('d-none');
        maybeTypeset(messages);
        scrollArea.scrollTop = scrollArea.scrollHeight;
    };

    const loadConversation = async (id) => {
        const response = await api(`/api/ai/conversations/${id}/messages`);
        currentConversationId = Number(id);
        const data = await response.json();
        renderMessages(data);
        await refreshConversations();
    };

    const refreshConversations = async () => {
        try {
            const query = search.value.trim();
            const response = await api(`/api/ai/conversations${query ? `?search=${encodeURIComponent(query)}` : ''}`);
            const items = await response.json();
            list.replaceChildren();
            if (!items.length) {
                const empty = document.createElement('div'); empty.className = 'small text-muted px-2 py-2'; empty.textContent = 'Chưa có cuộc trò chuyện.'; list.append(empty); return;
            }
            items.forEach(item => {
                const row = document.createElement('div'); row.className = `ai-conversation ${Number(item.id) === currentConversationId ? 'active' : ''}`;
                const open = document.createElement('button'); open.type = 'button'; open.className = 'ai-conversation-title'; open.title = item.preview || item.title;
                open.textContent = `${item.pinned ? '📌 ' : ''}${item.title}`; open.addEventListener('click', () => {
                    if (context && item.contextRole === role) context.value = item.contextId ?? '';
                    loadConversation(item.id).catch(showError);
                });
                const actions = document.createElement('button'); actions.type = 'button'; actions.className = 'ai-conversation-action'; actions.title = 'Tùy chọn'; actions.innerHTML = '<i class="bi bi-three-dots"></i>';
                actions.addEventListener('click', async () => {
                    const choice = window.prompt('Nhập 1 để đổi tên, 2 để ghim/bỏ ghim, 3 để xóa:', '1');
                    if (choice === '1') {
                        const title = window.prompt('Tên cuộc trò chuyện:', item.title);
                        if (title?.trim()) await api(`/api/ai/conversations/${item.id}`, { method: 'PATCH', body: JSON.stringify({ title: title.trim() }) });
                    } else if (choice === '2') await api(`/api/ai/conversations/${item.id}`, { method: 'PATCH', body: JSON.stringify({ pinned: !item.pinned }) });
                    else if (choice === '3' && window.confirm('Xóa cuộc trò chuyện này?')) {
                        await api(`/api/ai/conversations/${item.id}`, { method: 'DELETE' });
                        if (Number(item.id) === currentConversationId) showWelcome();
                    }
                    await refreshConversations();
                });
                row.append(open, actions); list.append(row);
            });
        } catch (error) { list.textContent = error.message; }
    };

    const showWelcome = (clearCurrent = true) => {
        if (clearCurrent) currentConversationId = null;
        pendingEditMessageId = null;
        messages.replaceChildren(); messages.classList.add('d-none'); welcome.classList.remove('d-none');
    };

    const showError = (error) => {
        const alert = document.createElement('div'); alert.className = 'alert alert-warning py-2'; alert.textContent = error.message || 'Không thể thực hiện yêu cầu.';
        scrollArea.prepend(alert); setTimeout(() => alert.remove(), 7000);
    };

    const parseSseChunk = (raw, onEvent) => {
        raw.split(/\r?\n/).forEach(line => {
            if (!line.startsWith('event:')) return;
            const event = line.slice(6).trim();
            const dataLine = raw.split(/\r?\n/).find(value => value.startsWith('data:'));
            if (!dataLine) return;
            try { onEvent(event, JSON.parse(dataLine.slice(5).trim())); } catch (_) {}
        });
    };

    const submitChat = async (text, editMessageId = null) => {
        if (!text.trim() || controller) return;
        try {
            if (!currentConversationId) {
                const created = await (await api('/api/ai/conversations', { method: 'POST' })).json();
                currentConversationId = created.id;
            }
            welcome.classList.add('d-none'); messages.classList.remove('d-none');
            if (editMessageId) {
                const prior = messages.querySelector(`[data-message-id="${CSS.escape(String(editMessageId))}"]`);
                if (prior) {
                    let node = prior;
                    while (node) { const next = node.nextElementSibling; node.remove(); node = next; }
                }
            }
            const userTempId = `temp-${Date.now()}`;
            const userNode = createMessage({ id: userTempId, senderRole: 'USER', content: text }, null);
            messages.append(userNode);
            const assistantNode = createMessage({ senderRole: 'ASSISTANT', content: '' }, null);
            const assistantText = assistantNode.querySelector('.ai-message-text');
            messages.append(assistantNode);
            scrollArea.scrollTop = scrollArea.scrollHeight;
            working.classList.remove('d-none'); sendButton.disabled = true;
            if (context) context.disabled = true;
            controller = new AbortController();
            const response = await api('/api/ai/chat', { method: 'POST', signal: controller.signal,
                body: JSON.stringify({ conversationId: currentConversationId, contextId: contextId(), editMessageId, message: text }) });
            const reader = response.body.getReader(); const decoder = new TextDecoder('utf-8');
            let buffer = ''; let answer = ''; let completed = false;
            while (true) {
                const { value, done } = await reader.read();
                if (done) break;
                buffer += decoder.decode(value, { stream: true });
                const events = buffer.split(/\r?\n\r?\n/); buffer = events.pop() || '';
                events.forEach(raw => parseSseChunk(raw, (type, data) => {
                    if (type === 'conversation') {
                        currentConversationId = data.id;
                        userNode.dataset.messageId = data.userMessageId;
                        userNode.querySelector('[data-action="edit"]').dataset.messageId = data.userMessageId;
                    } else if (type === 'delta') {
                        answer += data; assistantText.innerHTML = renderMarkdown(answer); maybeTypeset(assistantText); scrollToBottom();
                    } else if (type === 'error') showError(new Error(data.message));
                    else if (type === 'done') {
                        completed = true;
                        assistantNode.dataset.userMessageId = userNode.dataset.messageId;
                        const retry = document.createElement('button'); retry.type = 'button'; retry.dataset.action = 'retry';
                        retry.dataset.messageId = userNode.dataset.messageId; retry.innerHTML = '<i class="bi bi-arrow-clockwise"></i> Tạo lại';
                        assistantNode.querySelector('.ai-message-body').append(Object.assign(document.createElement('div'), { className: 'ai-message-actions' }));
                        assistantNode.querySelector('.ai-message-actions').append(retry);
                    }
                }));
            }
            if (completed) await loadConversation(currentConversationId);
            else if (editMessageId && currentConversationId) await loadConversation(currentConversationId);
            else if (!answer) assistantNode.remove();
        } catch (error) {
            if (editMessageId && currentConversationId) await loadConversation(currentConversationId).catch(() => {});
            if (error.name !== 'AbortError') showError(error);
        } finally {
            controller = null; working.classList.add('d-none'); sendButton.disabled = false; if (context) context.disabled = false; input.focus();
        }
    };

    document.getElementById('newConversation').addEventListener('click', () => { showWelcome(); refreshConversations(); input.focus(); });
    context?.addEventListener('change', () => {
        if (!currentConversationId) return;
        showWelcome();
        showError(new Error('Phạm vi đã đổi. Câu hỏi tiếp theo sẽ bắt đầu cuộc trò chuyện mới.'));
        refreshConversations();
    });
    form.addEventListener('submit', event => { event.preventDefault(); const text = input.value; const editId = pendingEditMessageId; input.value = ''; resizeInput(); pendingEditMessageId = null; submitChat(text, editId); });
    input.addEventListener('keydown', event => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); form.requestSubmit(); } });
    input.addEventListener('input', resizeInput);
    function resizeInput() { input.style.height = 'auto'; input.style.height = `${Math.min(input.scrollHeight, 180)}px`; }
    stopButton.addEventListener('click', () => controller?.abort());
    search.addEventListener('input', () => { clearTimeout(searchTimer); searchTimer = setTimeout(refreshConversations, 250); });
    document.querySelectorAll('.ai-suggestion').forEach(button => button.addEventListener('click', () => { input.value = button.dataset.prompt || ''; input.focus(); resizeInput(); }));

    messages.addEventListener('click', async (event) => {
        const button = event.target.closest('[data-action]'); if (!button) return;
        const action = button.dataset.action;
        if (action === 'edit') { input.value = button.dataset.content || ''; pendingEditMessageId = Number(button.dataset.messageId); input.focus(); resizeInput(); }
        if (action === 'retry') {
            const userNode = messages.querySelector(`[data-message-id="${CSS.escape(String(button.dataset.messageId))}"]`);
            const content = userNode?.querySelector('.ai-message-text')?.textContent || '';
            if (content) await submitChat(content, Number(button.dataset.messageId));
        }
        if (action === 'copy') await navigator.clipboard?.writeText(button.dataset.content || '').catch(() => {});
    });

    const runTool = async (url, data) => {
        try {
            const response = await api(url, { method: 'POST', body: JSON.stringify(data) });
            const body = await response.json();
            resultContent.textContent = body.content || JSON.stringify(body, null, 2);
            resultTitle = url.includes('exam') ? 'Đề kiểm tra do Hùng Cường AI soạn' : 'Bài tập do Hùng Cường AI soạn';
            resultPanel.classList.remove('d-none'); resultContent.classList.remove('d-none');
            scrollArea.scrollTop = scrollArea.scrollHeight;
        } catch (error) { showError(error); }
    };
    const formData = (formElement) => {
        const data = Object.fromEntries(new FormData(formElement).entries());
        formElement.querySelectorAll('input[type="number"]').forEach(item => { data[item.name] = Number(item.value); });
        formElement.querySelectorAll('input[type="checkbox"]').forEach(item => { data[item.name] = item.checked; });
        return data;
    };
    document.getElementById('exerciseForm')?.addEventListener('submit', event => { event.preventDefault(); runTool('/api/ai/generate-exercises', formData(event.currentTarget)); });
    document.getElementById('examForm')?.addEventListener('submit', event => { event.preventDefault(); runTool('/api/ai/generate-exam', formData(event.currentTarget)); });
    document.getElementById('useResultInChat')?.addEventListener('click', () => { input.value = resultContent.textContent; input.focus(); resizeInput(); });
    document.querySelectorAll('[data-export]').forEach(button => button.addEventListener('click', async () => {
        try {
            const response = await api('/api/ai/export', { method: 'POST', body: JSON.stringify({ title: resultTitle, content: resultContent.textContent, format: button.dataset.export }) });
            const blob = await response.blob(); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `hung-cuong-ai.${button.dataset.export}`; link.click(); URL.revokeObjectURL(url);
        } catch (error) { showError(error); }
    }));

    const fileInput = document.getElementById('documentFile');
    fileInput?.addEventListener('change', async () => {
        const file = fileInput.files?.[0]; if (!file) return;
        const formData = new FormData(); formData.append('file', file);
        const preview = document.getElementById('documentPreview');
        try {
            working.classList.remove('d-none');
            const response = await api('/api/ai/documents/extract', { method: 'POST', body: formData });
            const extracted = await response.json(); documentText = extracted.text; documentName = file.name;
            document.getElementById('documentName').textContent = `${file.name} (${documentText.length.toLocaleString('vi-VN')} ký tự)`;
            preview.classList.remove('d-none');
            if (!input.value.trim()) input.value = 'Hãy giúp tôi hiểu nội dung tài liệu sau:\n\n';
            input.value += `\n\n[Nội dung trích xuất từ tài liệu]\n${documentText}`; resizeInput(); input.focus();
        } catch (error) { showError(error); fileInput.value = ''; }
        finally { working.classList.add('d-none'); }
    });
    document.getElementById('removeDocument')?.addEventListener('click', () => {
        if (documentText) input.value = input.value.replace(`\n\n[Nội dung trích xuất từ tài liệu]\n${documentText}`, '');
        documentText = ''; documentName = ''; fileInput.value = ''; document.getElementById('documentPreview').classList.add('d-none'); resizeInput();
    });

    const sidebar = document.getElementById('aiSidebar'); const scrim = document.getElementById('sidebarScrim');
    const closeSidebar = () => { sidebar.classList.remove('open'); scrim.classList.remove('show'); };
    document.getElementById('openSidebar').addEventListener('click', () => { sidebar.classList.add('open'); scrim.classList.add('show'); });
    document.getElementById('closeSidebar').addEventListener('click', closeSidebar); scrim.addEventListener('click', closeSidebar);

    api('/api/ai/availability').then(r => r.json()).then(data => {
        if (!data.ready) {
            const note = document.createElement('div'); note.className = 'alert alert-info py-2 small';
            note.textContent = data.configured ? 'Hùng Cường AI hiện đang tắt hoặc chưa được Admin cấu hình hạn mức.' : 'Hùng Cường AI chưa được cấu hình khóa API ở máy chủ.';
            scrollArea.prepend(note); sendButton.disabled = true; input.disabled = true;
        }
    }).catch(() => {});
    refreshConversations();
})();

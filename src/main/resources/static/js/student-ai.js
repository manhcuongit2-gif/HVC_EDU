(() => {
  const app = document.getElementById('studentAiApp');
  if (!app) return;
  const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  const status = document.getElementById('studentAiStatus');
  const list = document.getElementById('studentConversationList');
  const messages = document.getElementById('studentAiMessages');
  const input = document.getElementById('studentAiInput');
  const attachmentInput = document.getElementById('studentAiAttachment');
  const preview = document.getElementById('studentAiAttachmentPreview');
  const previewImage = document.getElementById('studentAiAttachmentImage');
  const previewName = document.getElementById('studentAiAttachmentName');
  const previewPdfIcon = document.getElementById('studentAiAttachmentPdfIcon');
  const working = document.getElementById('studentAiWorking');
  const send = document.getElementById('studentAiSend');
  let conversationId = null;
  let busy = false;
  let aiReady = true;
  let previewUrl = null;
  const maxUploadBytes = Number(app.dataset.maxUploadBytes) || 10 * 1024 * 1024;
  const maxUploadLabel = `${Math.max(1, Math.round(maxUploadBytes / (1024 * 1024)))} MB`;

  const api = async (url, options = {}) => {
    const headers = new Headers(options.headers || {});
    if (options.method && !['GET', 'HEAD'].includes(options.method.toUpperCase())) headers.set(csrfHeader, csrf);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
    const response = await fetch(url, { credentials: 'same-origin', ...options, headers });
    if (!response.ok) {
      const detail = await response.json().catch(() => ({}));
      throw new Error(detail.message || `Yêu cầu thất bại (${response.status}).`);
    }
    return response;
  };

  const addMessage = (senderRole, content, transient = false) => {
    document.getElementById('studentAiWelcome')?.remove();
    const article = document.createElement('article');
    article.className = `student-ai-message ${senderRole === 'USER' ? 'user' : 'assistant'} rounded p-3`;
    const safeContent = content || '';
    article.textContent = senderRole === 'USER' || !window.HungCuongAIMath
      ? safeContent : window.HungCuongAIMath.normalize(safeContent);
    if (transient) article.dataset.transient = 'true';
    messages.append(article);
    messages.scrollTop = messages.scrollHeight;
    return article;
  };

  const clearTransient = () => messages.querySelectorAll('[data-transient="true"]').forEach(node => node.remove());
  const renderConversationList = async () => {
    const items = await (await api('/api/ai/student/conversations')).json();
    list.replaceChildren();
    items.forEach(item => {
      const button = document.createElement('button');
      button.type = 'button'; button.className = `list-group-item list-group-item-action text-start ${item.id === conversationId ? 'active' : ''}`;
      button.textContent = item.title || 'Cuộc trò chuyện mới';
      button.addEventListener('click', () => openConversation(item.id));
      list.append(button);
    });
    if (!items.length) {
      const empty = document.createElement('div'); empty.className = 'list-group-item small text-secondary'; empty.textContent = 'Chưa có cuộc trò chuyện.'; list.append(empty);
    }
  };

  const openConversation = async id => {
    conversationId = id;
    const rows = await (await api(`/api/ai/student/conversations/${id}/messages`)).json();
    messages.replaceChildren();
    if (!rows.length) {
      const welcome = document.createElement('div'); welcome.id = 'studentAiWelcome'; welcome.className = 'text-center text-secondary py-5'; welcome.textContent = 'Em có thể nhập câu hỏi hoặc tải ảnh/PDF bài tập lên.'; messages.append(welcome);
    } else rows.forEach(row => addMessage(row.senderRole, row.content));
    await window.HungCuongAIMath?.typeset(messages);
    renderConversationList();
  };

  const createConversation = async () => {
    const item = await (await api('/api/ai/student/conversations', { method: 'POST' })).json();
    await openConversation(item.id);
  };

  const readSse = async response => {
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let assistantNode = null;
    let assistantAnswer = '';
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
      const frames = buffer.split('\n\n'); buffer = frames.pop() || '';
      for (const frame of frames) {
        const eventName = frame.match(/^event:\s*(.+)$/m)?.[1];
        const dataText = frame.match(/^data:\s*(.+)$/m)?.[1];
        if (!eventName || !dataText) continue;
        const data = JSON.parse(dataText);
        if (eventName === 'conversation') {
          conversationId = data.id;
          clearTransient();
          await renderConversationList();
        } else if (eventName === 'delta') {
          if (!assistantNode) assistantNode = addMessage('ASSISTANT', '', true);
          assistantAnswer += typeof data === 'string' ? data : String(data);
          assistantNode.textContent = assistantAnswer;
          messages.scrollTop = messages.scrollHeight;
        } else if (eventName === 'error') {
          throw new Error(data.message || 'Không thể tạo câu trả lời.');
        } else if (eventName === 'done') {
          if (!assistantNode) addMessage('ASSISTANT', 'Đã hoàn tất.');
          if (assistantNode) {
            assistantNode.textContent = window.HungCuongAIMath?.normalize(assistantAnswer) ?? assistantAnswer;
            delete assistantNode.dataset.transient;
            await window.HungCuongAIMath?.typeset(assistantNode);
          }
        }
      }
      if (done) break;
    }
  };

  document.getElementById('newStudentConversation').addEventListener('click', () => createConversation().catch(error => { status.textContent = error.message; }));
  const clearAttachment = () => {
    attachmentInput.value = ''; preview.classList.add('d-none');
    previewImage.classList.add('d-none'); previewPdfIcon.classList.add('d-none'); previewImage.removeAttribute('src');
    previewName.textContent = '';
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewUrl = null;
  };
  attachmentInput.addEventListener('change', () => {
    const file = attachmentInput.files?.[0];
    if (!file) { clearAttachment(); return; }
    const name = file.name.toLowerCase();
    const isPdf = name.endsWith('.pdf') && (!file.type || file.type === 'application/pdf' || file.type === 'application/octet-stream');
    const isImage = /\.(jpe?g|png)$/.test(name) && (!file.type || /^image\/(jpeg|png)$/.test(file.type));
    if ((!isPdf && !isImage) || file.size <= 0 || file.size > maxUploadBytes) {
      status.textContent = `Chọn một ảnh JPG/PNG hoặc PDF hợp lệ, dung lượng tối đa ${maxUploadLabel}.`;
      clearAttachment(); return;
    }
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    previewName.textContent = file.name;
    if (isPdf) {
      previewImage.classList.add('d-none'); previewImage.removeAttribute('src');
      previewPdfIcon.classList.remove('d-none'); previewUrl = null;
    } else {
      previewPdfIcon.classList.add('d-none'); previewUrl = URL.createObjectURL(file);
      previewImage.src = previewUrl; previewImage.classList.remove('d-none');
    }
    preview.classList.remove('d-none');
  });
  document.getElementById('removeStudentAiAttachment').addEventListener('click', clearAttachment);

  document.getElementById('studentAiForm').addEventListener('submit', async event => {
    event.preventDefault();
    if (busy) return;
    const prompt = input.value.trim();
    const attachment = attachmentInput.files?.[0];
    const isPdf = attachment?.name.toLowerCase().endsWith('.pdf');
    if (!prompt && !attachment) { status.textContent = 'Nhập câu hỏi hoặc chọn ảnh/PDF trước khi gửi.'; return; }
    busy = true; send.disabled = true; working.classList.remove('d-none'); status.textContent = 'Đang gửi câu hỏi…';
    addMessage('USER', `${prompt || (isPdf ? 'Hỏi về PDF bài tập' : 'Hỏi về ảnh bài tập')}${attachment ? `\n[Đã gửi kèm ${isPdf ? 'PDF' : 'ảnh'}: ${attachment.name}]` : ''}`);
    const form = new FormData();
    form.append('message', prompt);
    form.append('action', document.getElementById('studentAiAction').value);
    if (conversationId) form.append('conversationId', String(conversationId));
    if (attachment) form.append(isPdf ? 'pdf' : 'image', attachment);
    try {
      const response = await api('/api/ai/student/chat', { method: 'POST', body: form, headers: { Accept: 'text/event-stream' } });
      await readSse(response);
      input.value = '';
      clearAttachment();
      status.textContent = 'Đã nhận câu trả lời.';
      await renderConversationList();
    } catch (error) {
      clearTransient(); addMessage('ASSISTANT', error.message);
      status.textContent = error.message;
    } finally {
      busy = false; send.disabled = !aiReady; working.classList.add('d-none'); input.focus();
    }
  });

  const refreshAvailability = async () => {
    try {
      const data = await (await api('/api/ai/student/availability')).json();
      const quotaAvailable = Number(data.usage?.remainingRequests ?? 0) > 0 && Number(data.usage?.remainingTokens ?? 0) > 0;
      status.textContent = `${data.message} Model hiện tại: ${data.selectedModel || 'chưa chọn'}. `
        + (data.visionReady ? 'Có thể gửi ảnh.' : 'Gửi ảnh cần Admin chọn model Qwen hỗ trợ hình ảnh.')
        + ` Lượt còn hôm nay: ${data.usage?.remainingRequests ?? '—'}.`
        + (data.ready && !quotaAvailable ? ' Hạn mức hiện tại đã dùng hết.' : '');
      status.className = `alert py-2 small ${data.ready && quotaAvailable ? 'alert-success' : 'alert-warning'}`;
      aiReady = Boolean(data.ready) && quotaAvailable;
      send.disabled = !aiReady;
    } catch (_) { aiReady = false; send.disabled = true; status.textContent = 'Không thể kiểm tra trạng thái AI lúc này.'; }
  };
  refreshAvailability();
  renderConversationList().catch(() => {});
})();

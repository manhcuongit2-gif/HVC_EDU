(() => {
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const run = async (button, studentId) => {
        const row = button.closest('tr');
        const additional = window.prompt('Thông tin học tập bổ sung (không bắt buộc):', '') || '';
        button.disabled = true;
        const previous = button.innerHTML;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang tạo…';
        try {
            const response = await fetch('/api/ai/generate-report', { method: 'POST', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', ...(csrf ? { [csrfHeader]: csrf } : {}) },
                body: JSON.stringify({ sessionId: Number(button.dataset.sessionId), studentId: studentId || null, additionalContext: additional }) });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || 'Không thể tạo nhận xét bằng AI.');
            Object.entries(data).forEach(([id, comment]) => {
                const targetRow = id === String(studentId) ? row : document.querySelector(`input[name="studentIds"][value="${CSS.escape(id)}"]`)?.closest('tr');
                const textarea = targetRow?.querySelector('textarea[name="comments"]');
                if (textarea) textarea.value = comment;
            });
            button.classList.remove('btn-link'); button.classList.add('btn-success');
            button.innerHTML = '<i class="bi bi-check2 me-1"></i>Đã điền bản nháp';
            window.setTimeout(() => { button.innerHTML = previous; button.classList.remove('btn-success'); button.classList.add('btn-link'); button.disabled = false; }, 2200);
        } catch (error) {
            window.alert(error.message);
            button.innerHTML = previous; button.disabled = false;
        }
    };
    document.querySelectorAll('[data-ai-report-one]').forEach(button => button.addEventListener('click', () => run(button, button.dataset.studentId)));
    document.querySelector('[data-ai-report-all]')?.addEventListener('click', event => run(event.currentTarget, null));
})();

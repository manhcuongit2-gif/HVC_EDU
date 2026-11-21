(() => {
  const plan = document.getElementById('studyPlan');
  if (!plan) return;
  const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  const notice = document.getElementById('studyPlanNotice');
  const toast = document.getElementById('studyPlanToast');
  const refreshButton = document.getElementById('refreshStudyPlan');
  let refreshing = false;

  const showToast = (message, success) => {
    toast.textContent = message;
    toast.className = `alert mt-3 ${success ? 'alert-success' : 'alert-warning'}`;
  };
  const refresh = async () => {
    if (refreshing) return;
    refreshing = true;
    refreshButton.disabled = true;
    refreshButton.textContent = 'Đang cập nhật…';
    if (notice) notice.textContent = 'Đang tổng hợp dữ liệu học tập đã lưu…';
    try {
      const response = await fetch('/api/ai/student/study-plan/refresh', {
        method: 'POST', credentials: 'same-origin', headers: { [csrfHeader]: csrf, Accept: 'application/json' }
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.message || 'Không thể cập nhật kế hoạch lúc này.');
      if (notice) notice.textContent = result.message || 'Đã kiểm tra dữ liệu học tập.';
      showToast(result.message || 'Đã kiểm tra dữ liệu học tập.', result.refreshed);
      if (result.refreshed) window.location.reload();
    } catch (error) {
      const message = error.message || 'Không thể cập nhật kế hoạch lúc này.';
      if (notice) notice.textContent = message;
      showToast(message, false);
    } finally {
      refreshing = false;
      refreshButton.disabled = false;
      refreshButton.textContent = 'Làm mới từ tiến độ';
    }
  };

  refreshButton?.addEventListener('click', refresh);
  if (plan.dataset.refreshNeeded === 'true') refresh();

  document.querySelectorAll('.study-plan-status').forEach(select => {
    select.addEventListener('change', async () => {
      const item = select.closest('[data-item-id]');
      try {
        const response = await fetch(`/api/ai/student/study-plan/items/${item.dataset.itemId}`, {
          method: 'PATCH', credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json', [csrfHeader]: csrf },
          body: JSON.stringify({ status: select.value })
        });
        if (!response.ok) throw new Error('Không thể lưu trạng thái kế hoạch.');
        showToast('Đã lưu trạng thái mục ôn tập.', true);
      } catch (error) {
        showToast(error.message, false);
      }
    });
  });
})();

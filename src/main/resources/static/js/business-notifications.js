(() => {
  const region = document.getElementById('businessToastRegion');
  const badge = document.getElementById('businessNotificationCount');
  if (!region || !badge) return;

  const csrfToken = region.dataset.csrfToken || '';
  const csrfHeader = region.dataset.csrfHeader || 'X-CSRF-TOKEN';
  const seen = new Set();
  const queue = [];
  let showing = false;

  const categoryStyle = (category) => {
    const styles = {
      SCHEDULE: ['bi-calendar-event', '#1769e0', 'Lịch học'],
      SCHEDULE_CHANGE: ['bi-calendar2-check', '#1769e0', 'Thay đổi lịch'],
      ATTENDANCE: ['bi-person-check', '#16845b', 'Điểm danh'],
      HOMEWORK: ['bi-journal-check', '#7653c5', 'Bài tập'],
      EXAM: ['bi-award', '#a45b11', 'Đề thi'],
      EXAM_RESULT: ['bi-patch-check', '#a45b11', 'Kết quả thi'],
      TUITION: ['bi-credit-card', '#157347', 'Học phí'],
      TUITION_REMINDER: ['bi-cash-coin', '#157347', 'Nhắc học phí'],
      REGISTRATION: ['bi-card-checklist', '#0b7285', 'Đăng ký học'],
      MESSAGE_PRIVATE: ['bi-chat-dots', '#5a53b8', 'Tin nhắn'],
      MESSAGE_GROUP: ['bi-people', '#5a53b8', 'Tin nhắn'],
      GENERAL: ['bi-bell', '#155eef', 'Thông báo chung']
    };
    return styles[category] || styles.GENERAL;
  };

  const updateBadge = async (data) => {
    const unread = Number(data?.unreadCount || 0);
    badge.textContent = unread > 99 ? '99+' : String(unread);
    badge.classList.toggle('d-none', unread === 0);
    try {
      const messagesResponse = await fetch('/api/notifications', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
      const messages = messagesResponse.ok ? await messagesResponse.json() : null;
      const messageCount = Number(messages?.unreadCount || 0);
      const total = unread + messageCount;
      if (total > 0 && navigator.setAppBadge) await navigator.setAppBadge(total);
      else if (navigator.clearAppBadge) await navigator.clearAppBadge();
    } catch (_) { }
  };

  const removeToast = (toast) => {
    if (!toast?.isConnected) return;
    toast.classList.remove('show');
    window.setTimeout(() => toast.remove(), 180);
  };

  const openNotification = async (notification, finish) => {
    finish();
    if (notification.id != null) {
      try {
        await fetch(`/api/business-notifications/${encodeURIComponent(notification.id)}/read`, {
          method: 'POST', credentials: 'same-origin',
          headers: { [csrfHeader]: csrfToken, Accept: 'application/json' }
        });
      } catch (_) { }
    }
    const target = typeof notification.targetPath === 'string' && notification.targetPath.startsWith('/')
      && !notification.targetPath.startsWith('//') ? notification.targetPath : '/notifications';
    window.location.assign(target);
  };

  const showNext = () => {
    if (showing || !queue.length) return;
    showing = true;
    const notification = queue.shift();
    const [iconName, accent, label] = categoryStyle(notification.category);
    const toast = document.createElement('article');
    toast.className = 'toast business-toast show';
    toast.setAttribute('role', 'status');
    toast.style.setProperty('--toast-accent', accent);

    const header = document.createElement('div');
    header.className = 'toast-header';
    const icon = document.createElement('i');
    icon.className = `bi ${iconName} me-2`;
    icon.style.color = accent;
    icon.setAttribute('aria-hidden', 'true');
    const title = document.createElement('strong');
    title.className = 'me-auto text-truncate';
    title.textContent = notification.title || label;
    const category = document.createElement('small');
    category.className = 'text-secondary me-2';
    category.textContent = label;
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'btn-close';
    close.setAttribute('aria-label', 'Đóng thông báo');
    let timer;
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      window.clearTimeout(timer);
      removeToast(toast);
      showing = false;
      window.setTimeout(showNext, 200);
    };
    close.addEventListener('click', finish);
    header.append(icon, title, category, close);

    const body = document.createElement('button');
    body.type = 'button';
    body.className = 'toast-body btn text-start w-100';
    body.textContent = notification.body || 'Bạn có thông báo mới.';
    body.addEventListener('click', () => openNotification(notification, finish));

    const footer = document.createElement('div');
    footer.className = 'small text-secondary px-3 pb-2';
    footer.textContent = notification.createdAt ? new Date(notification.createdAt).toLocaleString('vi-VN') : 'Bạn có thông báo mới';
    toast.append(header, body, footer);
    region.append(toast);

    timer = window.setTimeout(finish, 6500);
  };

  const receive = (notification) => {
    if (!notification || notification.id == null) return;
    const id = String(notification.id);
    if (seen.has(id)) return;
    seen.add(id);
    queue.push(notification);
    showNext();
  };

  const refresh = async (showNew) => {
    try {
      const response = await fetch('/api/business-notifications', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
      if (!response.ok) return;
      const data = await response.json();
      await updateBadge(data);
      const items = Array.isArray(data.items) ? data.items : [];
      if (!showNew) items.forEach(item => seen.add(String(item.id)));
      else [...items].reverse().forEach(receive);
    } catch (_) { }
  };

  const connect = () => {
    if (!window.StompJs?.Client) return;
    const client = new window.StompJs.Client({
      brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        client.subscribe('/user/queue/business-notifications', frame => {
          try { receive(JSON.parse(frame.body)); refresh(false); } catch (_) { }
        });
        refresh(true);
      },
      onStompError: () => { }
    });
    client.activate();
    window.addEventListener('pagehide', () => client.deactivate(), { once: true });
  };

  refresh(false).finally(connect);
  window.setInterval(() => refresh(true), 20000);
})();

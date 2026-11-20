(() => {
  const head = document.head;
  if (head && !head.querySelector('link[rel="manifest"]')) {
    const manifest = document.createElement('link'); manifest.rel = 'manifest'; manifest.href = '/manifest.webmanifest'; head.append(manifest);
    const theme = document.createElement('meta'); theme.name = 'theme-color'; theme.content = '#1769e0'; head.append(theme);
  }
  if (!('serviceWorker' in navigator)) return;
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js').catch(() => {}));

  const csrf = document.querySelector('meta[name="csrf-token"]')?.content || document.querySelector('input[name="_csrf"]')?.value || '';
  const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.content || 'X-CSRF-TOKEN';
  const request = (url, options = {}) => fetch(url, { credentials: 'same-origin', ...options, headers: {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(csrf ? { [csrfHeader]: csrf } : {}), ...(options.headers || {})
  }});
  const fromBase64Url = value => {
    const padded = value.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - value.length % 4) % 4);
    return Uint8Array.from(atob(padded), c => c.charCodeAt(0));
  };

  const enable = document.getElementById('enablePushButton');
  const status = document.getElementById('pushStatus');
  if (enable) enable.addEventListener('click', async () => {
    enable.disabled = true;
    try {
      if (!('PushManager' in window) || !('Notification' in window)) throw new Error('Trình duyệt này chưa hỗ trợ thông báo đẩy.');
      const keyResponse = await fetch('/api/push/public-key', { credentials: 'same-origin' });
      const config = await keyResponse.json();
      if (!config.enabled || !config.publicKey) throw new Error('Máy chủ chưa cấu hình khóa VAPID.');
      const registration = await navigator.serviceWorker.ready;
      let subscription = await registration.pushManager.getSubscription();
      if (!subscription) {
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') throw new Error('Bạn chưa cho phép gửi thông báo.');
        subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: fromBase64Url(config.publicKey) });
      }
      const json = subscription.toJSON();
      const response = await request('/api/push/subscriptions', { method: 'POST', body: JSON.stringify({
        endpoint: json.endpoint, keys: json.keys, deviceName: navigator.userAgentData?.platform || navigator.platform || 'Thiết bị của tôi',
        platform: navigator.userAgentData?.platform || navigator.platform || 'Web'
      }) });
      if (!response.ok) throw new Error('Không thể lưu thiết bị nhận thông báo.');
      if (status) status.textContent = 'Thiết bị này đã bật thông báo đẩy.';
      window.location.reload();
    } catch (error) {
      if (status) status.textContent = error.message || 'Không thể bật thông báo.';
      enable.disabled = false;
    }
  });

  document.querySelectorAll('[data-remove-device]').forEach(button => button.addEventListener('click', async () => {
    const id = button.dataset.removeDevice;
    const response = await request(`/api/push/subscriptions/${encodeURIComponent(id)}`, { method: 'DELETE' });
    if (response.ok) button.closest('[data-device-row]')?.remove();
  }));

  const installButton = document.getElementById('installAppButton');
  let installPrompt;
  window.addEventListener('beforeinstallprompt', event => { event.preventDefault(); installPrompt = event; installButton?.classList.remove('d-none'); });
  installButton?.addEventListener('click', async () => {
    if (!installPrompt) return;
    installPrompt.prompt(); await installPrompt.userChoice; installPrompt = null; installButton.classList.add('d-none');
  });
})();

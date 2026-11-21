(() => {
  const head = document.head;
  if (head) {
    if (!head.querySelector('link[rel="manifest"]')) {
      const manifest = document.createElement('link');
      manifest.rel = 'manifest';
      manifest.href = '/manifest.webmanifest';
      head.append(manifest);
    }
    if (!head.querySelector('link[rel="apple-touch-icon"]')) {
      const appleIcon = document.createElement('link');
      appleIcon.rel = 'apple-touch-icon';
      appleIcon.href = '/icons/apple-touch-icon.png';
      head.append(appleIcon);
    }
    if (!head.querySelector('meta[name="theme-color"]')) {
      const theme = document.createElement('meta');
      theme.name = 'theme-color';
      theme.content = '#1769e0';
      head.append(theme);
    }
    if (!head.querySelector('meta[name="apple-mobile-web-app-capable"]')) {
      const capable = document.createElement('meta');
      capable.name = 'apple-mobile-web-app-capable';
      capable.content = 'yes';
      head.append(capable);
    }
  }

  const csrf = document.querySelector('meta[name="csrf-token"]')?.content
    || document.querySelector('input[name="_csrf"]')?.value || '';
  const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.content || 'X-CSRF-TOKEN';
  const request = (url, options = {}) => fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(csrf ? { [csrfHeader]: csrf } : {}),
      ...(options.headers || {})
    }
  });
  const fromBase64Url = value => {
    const padded = value.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - value.length % 4) % 4);
    return Uint8Array.from(atob(padded), character => character.charCodeAt(0));
  };
  const sha256 = async value => {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    return [...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('');
  };

  let registrationPromise = null;
  if ('serviceWorker' in navigator && window.isSecureContext) {
    registrationPromise = navigator.serviceWorker.register('/sw.js')
      .then(() => navigator.serviceWorker.ready)
      .catch(() => null);
  }

  const enableButton = document.getElementById('enablePushButton');
  const disableButton = document.getElementById('disablePushButton');
  const status = document.getElementById('pushStatus');
  const deviceList = document.getElementById('deviceList');
  let localSubscription = null;
  let localEndpointHash = null;
  let ownedCurrentDevice = null;
  let knownDevices = [];
  let pushConfig = null;
  let pushSetupError = '';
  let deviceLoadError = '';
  let subscriptionChanged = false;
  let localUnsubscribeWarning = '';

  const setStatus = (message, kind = 'secondary') => {
    if (!status) return;
    status.textContent = message;
    status.className = `small alert alert-${kind} py-2`;
  };

  const isAppleMobile = /iPhone|iPad|iPod/i.test(navigator.userAgent)
    || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const isStandalone = Boolean(navigator.standalone)
    || window.matchMedia?.('(display-mode: standalone)').matches === true;

  async function loadPushState() {
    if (!enableButton && !deviceList) return;
    if (!window.isSecureContext) {
      pushSetupError = 'Thông báo đẩy cần HTTPS hoặc địa chỉ localhost. Hãy mở hệ thống bằng tên miền HTTPS trên điện thoại.';
    } else if (!('serviceWorker' in navigator)) {
      pushSetupError = 'Trình duyệt này chưa hỗ trợ service worker để nhận thông báo đẩy.';
    } else if (!('PushManager' in window) || !('Notification' in window)) {
      pushSetupError = 'Trình duyệt này chưa hỗ trợ thông báo đẩy.';
    } else if (isAppleMobile && !isStandalone) {
      pushSetupError = 'Trên iPhone/iPad, hãy thêm website vào Màn hình chính rồi mở ứng dụng để bật thông báo.';
    }

    const keyPromise = fetch('/api/push/public-key', { credentials: 'same-origin' })
      .then(response => response.ok ? response.json() : Promise.reject(new Error('Không tải được cấu hình thông báo.')))
      .then(config => {
        if (!config.enabled || !config.publicKey) throw new Error('Máy chủ chưa cấu hình khóa VAPID.');
        return config;
      });

    const [registration, config, devices] = await Promise.all([
      registrationPromise || Promise.resolve(null),
      keyPromise.catch(error => { if (!pushSetupError) pushSetupError = error.message; return null; }),
      request('/api/push/subscriptions').then(response => response.ok ? response.json() : Promise.reject(new Error('Không tải được danh sách thiết bị.')))
        .catch(() => { deviceLoadError = 'Không tải được danh sách thiết bị. Hãy tải lại trang để kiểm tra trạng thái.'; return null; })
    ]);
    pushConfig = config;
    if (Array.isArray(devices)) deviceLoadError = '';
    if (!registration && !pushSetupError) pushSetupError = 'Không thể khởi tạo service worker trên thiết bị này.';

    if (registration && window.isSecureContext && 'PushManager' in window) {
      try {
        localSubscription = await registration.pushManager.getSubscription();
        if (localSubscription) localEndpointHash = await sha256(localSubscription.endpoint);
      } catch (_) {
        localSubscription = null;
        localEndpointHash = null;
      }
    }

    knownDevices = Array.isArray(devices) ? devices : [];
    ownedCurrentDevice = knownDevices.find(device => device.endpointHash === localEndpointHash) || null;
    renderDevices();
    updateControls();
  }

  function updateControls() {
    if (!enableButton) return;
    const canRequest = window.isSecureContext && 'serviceWorker' in navigator
      && 'PushManager' in window && 'Notification' in window;
    const permission = 'Notification' in window ? Notification.permission : 'unsupported';
    const permissionBlocked = canRequest && permission === 'denied';
    const foreignSubscription = localSubscription && !ownedCurrentDevice;
    const unavailable = pushSetupError || permissionBlocked;
    enableButton.disabled = Boolean(unavailable || ownedCurrentDevice);
    enableButton.classList.toggle('d-none', Boolean(ownedCurrentDevice));
    if (disableButton) {
      disableButton.classList.toggle('d-none', !ownedCurrentDevice);
      disableButton.disabled = !ownedCurrentDevice;
    }

    if (ownedCurrentDevice && pushSetupError) {
      setStatus(`Máy chủ đang lưu thiết bị này nhưng hiện chưa thể gửi thông báo: ${pushSetupError}`, 'warning');
    } else if (ownedCurrentDevice && permission !== 'granted') {
      setStatus('Máy chủ đang lưu thiết bị này nhưng trình duyệt chưa cấp quyền thông báo. Hãy kiểm tra quyền trang web hoặc gỡ thiết bị.', 'warning');
    } else if (ownedCurrentDevice) {
      setStatus('Thông báo đang bật trên thiết bị này. Nội dung trên màn hình khóa luôn được hiển thị ở dạng chung.', 'success');
    } else if (localUnsubscribeWarning && localSubscription) {
      setStatus(localUnsubscribeWarning, 'warning');
    } else if (subscriptionChanged) {
      setStatus('Đăng ký thông báo trên trình duyệt đã hết hạn hoặc thay đổi. Bấm bật để liên kết lại thiết bị này.', 'warning');
    } else if (foreignSubscription) {
      setStatus('Trình duyệt này đã có đăng ký push. Bấm bật để chủ động liên kết tài khoản hiện tại; liên kết của các tài khoản khác vẫn được giữ.', 'warning');
    } else if (pushSetupError) {
      setStatus(pushSetupError, 'warning');
    } else if (permissionBlocked) {
      setStatus('Thông báo đang bị chặn trong cài đặt trình duyệt. Hãy cho phép tại cài đặt trang web rồi thử lại.', 'warning');
    } else if (permission === 'granted') {
      setStatus('Thông báo đã được cho phép. Bấm nút bên dưới để liên kết thiết bị này với tài khoản.', 'secondary');
    } else {
      setStatus('Thông báo chưa bật trên thiết bị này. Chỉ trình duyệt này nhận được thông báo sau khi bạn chủ động bật.', 'secondary');
    }
  }

  function renderDevices() {
    if (!deviceList) return;
    deviceList.replaceChildren();
    if (!knownDevices.length) {
      const empty = document.createElement('div');
      empty.className = `list-group-item ${deviceLoadError ? 'text-danger' : 'text-secondary'}`;
      empty.textContent = deviceLoadError || 'Chưa có thiết bị nào bật thông báo.';
      deviceList.append(empty);
      return;
    }
    knownDevices.forEach(device => {
      const row = document.createElement('div');
      row.className = 'list-group-item d-flex justify-content-between align-items-center gap-3';
      row.dataset.deviceRow = '';
      row.dataset.endpointHash = device.endpointHash;
      const label = document.createElement('div');
      const name = document.createElement('strong');
      name.textContent = device.name || 'Thiết bị của tôi';
      const detail = document.createElement('div');
      detail.className = 'small text-secondary';
      detail.textContent = `${device.platform || 'Trình duyệt'} · hoạt động ${device.lastSeenAt || ''}`;
      label.append(name, detail);
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'btn btn-sm btn-outline-danger';
      button.textContent = 'Gỡ khỏi tài khoản này';
      button.addEventListener('click', () => removeDevice(device, row));
      row.append(label, button);
      deviceList.append(row);
    });
  }

  async function removeDevice(device, row) {
    const button = row?.querySelector('button');
    if (button) button.disabled = true;
    try {
      const response = await request(`/api/push/subscriptions/${encodeURIComponent(device.id)}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Không thể gỡ thiết bị. Hãy tải lại trang và thử lại.');
      // A physical browser subscription can be linked to several accounts. Removing
      // this account's link must not unsubscribe the browser endpoint used by others.
      if (device.endpointHash === localEndpointHash) {
        setStatus('Đã tắt thông báo cho tài khoản này. Liên kết của các tài khoản khác vẫn hoạt động.', 'success');
        ownedCurrentDevice = null;
      }
      knownDevices = knownDevices.filter(item => item.id !== device.id);
      renderDevices();
      updateControls();
    } catch (error) {
      setStatus(error.message || 'Không thể gỡ thiết bị.', 'danger');
      if (button) button.disabled = false;
    }
  }

  if (enableButton) {
    enableButton.disabled = true;
    enableButton.addEventListener('click', async () => {
      enableButton.disabled = true;
      try {
        if (pushSetupError) throw new Error(pushSetupError);
        if (!pushConfig) throw new Error('Máy chủ chưa sẵn sàng nhận thông báo.');
        let permission = Notification.permission;
        if (permission === 'default') permission = await Notification.requestPermission();
        if (permission !== 'granted') throw new Error('Bạn chưa cho phép trình duyệt gửi thông báo.');

        const registration = await registrationPromise;
        if (!registration) throw new Error('Không thể khởi tạo service worker.');
        let subscription = await registration.pushManager.getSubscription();
        if (!subscription) {
          subscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: fromBase64Url(pushConfig.publicKey)
          });
        }
        const json = subscription.toJSON();
        const response = await request('/api/push/subscriptions', {
          method: 'POST',
          body: JSON.stringify({
            endpoint: json.endpoint,
            keys: json.keys,
            deviceName: navigator.userAgentData?.platform || navigator.platform || 'Thiết bị của tôi',
            platform: navigator.userAgentData?.platform || navigator.platform || 'Web'
          })
        });
        if (response.status === 409) {
          localSubscription = subscription;
          localEndpointHash = await sha256(subscription.endpoint);
          throw new Error('Dữ liệu đăng ký thiết bị không khớp. Hãy tải lại trang và thử lại.');
        }
        if (!response.ok) throw new Error('Máy chủ chưa lưu được đăng ký nhận thông báo. Hãy thử lại.');
        localSubscription = subscription;
        localEndpointHash = await sha256(subscription.endpoint);
        localUnsubscribeWarning = '';
        const devices = await request('/api/push/subscriptions');
        if (!devices.ok) {
          setStatus('Máy chủ đã lưu đăng ký nhận thông báo nhưng không tải được danh sách thiết bị. Hãy tải lại trang để xác minh hoặc tắt.', 'warning');
          return;
        }
        knownDevices = await devices.json();
        deviceLoadError = '';
        ownedCurrentDevice = knownDevices.find(device => device.endpointHash === localEndpointHash) || null;
        subscriptionChanged = false;
        renderDevices();
        updateControls();
      } catch (error) {
        setStatus(error.message || 'Không thể bật thông báo trên thiết bị này.', 'danger');
        const permissionBlocked = 'Notification' in window && Notification.permission === 'denied';
        enableButton.disabled = Boolean(pushSetupError || localSubscription || permissionBlocked);
      }
    });
  }

  disableButton?.addEventListener('click', () => {
    if (ownedCurrentDevice) removeDevice(ownedCurrentDevice,
      deviceList?.querySelector(`[data-endpoint-hash="${ownedCurrentDevice.endpointHash}"]`));
  });

  loadPushState().catch(() => {
    pushSetupError ||= 'Không thể kiểm tra trạng thái thông báo trên thiết bị này.';
    updateControls();
  });
  navigator.serviceWorker?.addEventListener('message', event => {
    if (event.data?.type !== 'EDUCENTER_PUSH_SUBSCRIPTION_CHANGED') return;
    subscriptionChanged = true;
    loadPushState().catch(() => setStatus('Đăng ký thông báo đã thay đổi. Hãy tải lại trang và bật lại trên thiết bị.', 'warning'));
  });

  const installButton = document.getElementById('installAppButton');
  let installPrompt = null;
  window.addEventListener('beforeinstallprompt', event => {
    if (!installButton) return;
    event.preventDefault();
    installPrompt = event;
    installButton?.classList.remove('d-none');
  });
  window.addEventListener('appinstalled', () => {
    installPrompt = null;
    installButton?.classList.add('d-none');
  });
  installButton?.addEventListener('click', async () => {
    if (!installPrompt) return;
    installButton.disabled = true;
    try {
      await installPrompt.prompt();
      await installPrompt.userChoice;
    } finally {
      installPrompt = null;
      installButton.classList.add('d-none');
      installButton.disabled = false;
    }
  });
})();

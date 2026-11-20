const CACHE_NAME = 'educenter-shell-v1';
const SHELL = ['/offline.html', '/icons/school.svg'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys
    .filter(key => key.startsWith('educenter-') && key !== CACHE_NAME)
    .map(key => caches.delete(key)))));
  self.clients.claim();
});

// Authenticated HTML, API responses, and uploaded files are deliberately never cached.
self.addEventListener('fetch', event => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')
      || url.pathname.startsWith('/chat/') || url.pathname.startsWith('/uploads/')) return;
  if (request.mode === 'navigate') {
    event.respondWith(fetch(request).catch(() => caches.match('/offline.html')));
  }
});

self.addEventListener('push', event => {
  let payload = {};
  try { payload = event.data ? event.data.json() : {}; } catch (_) { payload = { body: 'Bạn có thông báo mới.' }; }
  const title = payload.title || 'EduCenter';
  const options = {
    body: payload.body || 'Bạn có thông báo mới.',
    icon: '/icons/school.svg',
    badge: '/icons/school.svg',
    tag: payload.tag || 'educenter-notification',
    renotify: false,
    silent: Boolean(payload.silent),
    data: { url: payload.url || '/notifications' }
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  let targetUrl;
  try {
    targetUrl = new URL(event.notification.data?.url || '/notifications', self.location.origin);
    if (targetUrl.origin !== self.location.origin) targetUrl = new URL('/notifications', self.location.origin);
  } catch (_) {
    targetUrl = new URL('/notifications', self.location.origin);
  }
  const target = targetUrl.href;
  event.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windows => {
    const sameOrigin = windows.find(client => new URL(client.url).origin === self.location.origin);
    if (sameOrigin) return sameOrigin.navigate(target).then(client => client.focus());
    return clients.openWindow(target);
  }));
});

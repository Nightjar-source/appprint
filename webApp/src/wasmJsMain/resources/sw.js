const CACHE = 'tabati-v2.1.54';
// Legacy unversioned workers cached an obsolete webApp.js (and a missing .wasm hash).
// When this file is fetched at the old URL, retire it; the new index registers sw.js?v=… .
if (!self.location.search.includes('v=')) {
  self.registration.unregister().catch(() => {});
}
// Emit only relative, existing root assets. Compose/Wasm hashed chunks are fetched at runtime.
const ASSETS = ['./', '/appprint', '/appprint/', './index.html', './webApp.js', './manifest.webmanifest', './icon-192.png', './icon-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE)
    .then(c => Promise.allSettled(ASSETS.map(a => c.add(a).catch(() => {}))))
    .then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim())
    .then(() => self.clients.matchAll().then(clients => clients.forEach(client => {
      try { client.postMessage('tabati-sw-updated'); } catch (_) {}
    }))));
});

self.addEventListener('message', event => {
  if (event.data === 'tabati-sw-updated') {
    const client = event.source;
    if (client) client.postMessage('tabati-sw-updated'); // ack for diagnostics
  }
});

const fetchAndCache = req => fetch(req).then(res => {
  if (res.ok && res.type !== 'opaqueredirect') {
    const copy = res.clone();
    caches.open(CACHE).then(c => c.put(req, copy)).catch(() => {});
  }
  return res;
}).catch(() => null);

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  const navigate = req.mode === 'navigate';
  if (navigate) {
    e.respondWith(
      fetchAndCache(req)
        .then(res => res || caches.match('./index.html'))
    );
    return;
  }

  e.respondWith(
    caches.match(req)
      .then(hit => hit || fetchAndCache(req) || caches.match('./index.html'))
  );
});

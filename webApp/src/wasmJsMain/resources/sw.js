const CACHE = 'tabati-v2.1.68';

// Retire legacy unversioned workers that cached a stale webApp.js with a missing .wasm hash.
if (!self.location.search.includes('v=')) {
  self.registration.unregister().catch(() => {});
}

// Only stable, version-independent shell assets are precached. webApp.js and the hashed
// .wasm/.data files are ALWAYS taken from the network first — they change every release.
const ASSETS = ['./', '/appprint', '/appprint/', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'];

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
    if (client) client.postMessage('tabati-sw-updated');
  }
});

const put = (req, res) => {
  if (res && res.ok && res.type !== 'opaqueredirect') {
    const copy = res.clone();
    caches.open(CACHE).then(c => c.put(req, copy)).catch(() => {});
  }
  return res;
};

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Navigation: network first so a new release is picked up immediately.
  if (req.mode === 'navigate') {
    e.respondWith(fetch(req).then(res => put(req, res)).catch(() => caches.match('./index.html')));
    return;
  }

  // Runtime code assets: NEVER serve a stale cached copy — a mismatched webApp.js/.wasm
  // pair breaks WebAssembly.compile with "HTTP status code is not ok".
  if (/\.(js|wasm|data)(\?|$)/.test(url.pathname + url.search) || url.pathname.endsWith('webApp.js')) {
    e.respondWith(fetch(req).then(res => put(req, res)).catch(() => caches.match(req)));
    return;
  }

  // Everything else: cache first, then network.
  e.respondWith(caches.match(req).then(hit => hit || fetch(req).then(res => put(req, res))));
});

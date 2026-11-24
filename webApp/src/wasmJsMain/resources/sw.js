const CACHE = 'tabati-v2.1.28';
// Compose/Wasm emits hashed .wasm chunks (e.g. 16d025....wasm); there is no stable
// webApp.wasm, so precache only existing root assets and fetch hashes at runtime.
const ASSETS = ['./', './index.html', './webApp.js', './manifest.webmanifest', './icon-192.png', './icon-512.png'];
const BASE = '/appprint/';

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE)
    .then(c => Promise.allSettled(ASSETS.map(a => c.add(a))))
    .then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

const normalize = p => (p === '' || p === '/' ? BASE : p.replace(/\/+$/, '/'));

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  const path = normalize(url.pathname);
  // Only serve same-origin GETs; the file URL scheme is not used by this PWA.
  if (url.origin !== self.location.origin) return;

  const respondWithCachedOrNetwork = () => fetch(req).then(res => {
    if (res.ok && res.type !== 'opaqueredirect') {
      const copy = res.clone();
      caches.open(CACHE).then(c => c.put(req, copy)).catch(() => {});
    }
    return res;
  }).catch(() => caches.match('./index.html'));

  if (req.mode === 'navigate') {
    e.respondWith(
      respondWithCachedOrNetwork()
    );
    return;
  }

  e.respondWith(
    caches.match(req).then(hit => hit || respondWithCachedOrNetwork())
  );
});

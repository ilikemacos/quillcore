import { put, list } from '@vercel/blob';
import { timingSafeEqual, randomBytes } from 'node:crypto';

const MAX_BYTES = 2_000_000;
const ID_RE = /^[0-9]{8}-[0-9]{6}-[a-f0-9]{6}$/;

function authorized(req) {
  const expected = process.env.QUILL_UPLOAD_TOKEN || '';
  const got = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  if (expected.length < 32 || got.length !== expected.length) return false;
  return timingSafeEqual(Buffer.from(got), Buffer.from(expected));
}

async function readBody(req) {
  const chunks = []; let n = 0;
  for await (const c of req) { n += c.length; if (n > MAX_BYTES) throw new Error('too large'); chunks.push(c); }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function summarize(id, d) {
  const s = d.bench?.summary || {};
  return {
    id, generated: d.meta?.generated, commit: d.meta?.commit, branch: d.meta?.branch, gpu: d.meta?.['GPU renderer (GL_RENDERER)'] || d.meta?.GPU, benchValid: d.bench ? d.bench.valid !== false : null,
    resolution: d.meta?.['Bench resolution'], overall: d.overall,
    layers: (d.layers || []).map(l => ({ name: l.name, status: l.status, tests: l.tests ?? null, passed: l.passed ?? null, failed: l.failed ?? null })),
    avgFps: s.avgFps || null, pct1LowFps: s.pct1LowFps || null, msptAvg: s.msptAvg || null,
  };
}

export default async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');
  try {
    if (req.method === 'POST') {
      if (!authorized(req)) return res.status(401).json({ error: 'unauthorized' });
      const d = await readBody(req);
      if (!d || typeof d !== 'object' || !Array.isArray(d.layers) || !d.meta) return res.status(400).json({ error: 'not a quillTest run' });
      const ts = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 15).replace(/^(\d{8})(\d{6}).*/, '$1-$2');
      const id = `${ts.slice(0, 8)}-${ts.slice(9, 15)}-${randomBytes(3).toString('hex')}`;
      d.receivedAt = new Date().toISOString();
      const opts = { access: 'public', contentType: 'application/json', addRandomSuffix: false };
      await put(`runs/${id}.json`, JSON.stringify(d), opts);
      await put(`index/${id}.json`, JSON.stringify(summarize(id, d)), opts);
      const base = `https://${req.headers.host}`;
      return res.status(201).json({ id, url: `${base}/run?id=${id}` });
    }
    if (req.method === 'GET') {
      const id = req.query.id;
      if (id) {
        if (!ID_RE.test(id)) return res.status(400).json({ error: 'bad id' });
        const { blobs } = await list({ prefix: `runs/${id}.json`, limit: 1 });
        if (!blobs.length) return res.status(404).json({ error: 'not found' });
        const r = await fetch(blobs[0].url, { cache: 'no-store' });
        return res.status(200).json(await r.json());
      }
      const { blobs } = await list({ prefix: 'index/', limit: 1000 });
      blobs.sort((a, b) => b.pathname.localeCompare(a.pathname));
      const runs = await Promise.all(blobs.slice(0, 100).map(b => fetch(b.url, { cache: 'no-store' }).then(r => r.json()).catch(() => null)));
      return res.status(200).json({ runs: runs.filter(Boolean) });
    }
    res.setHeader('Allow', 'GET, POST');
    return res.status(405).json({ error: 'method not allowed' });
  } catch (e) {
    return res.status(e.message === 'too large' ? 413 : 500).json({ error: e.message === 'too large' ? 'payload too large' : 'server error' });
  }
}

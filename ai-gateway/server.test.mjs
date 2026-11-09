import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

test('gateway uses server-side API secret', () => {
  const source=readFileSync(new URL('./server.mjs',import.meta.url),'utf8');
  assert.match(source,/process\.env\.OPENAI_API_KEY/);
  assert.match(source,/https:\/\/api\.openai\.com\/v1\/responses/);
});
test('persistent device storage and revocation paths exist', () => {
  const source=readFileSync(new URL('./server.mjs',import.meta.url),'utf8');
  assert.match(source,/CREATE TABLE IF NOT EXISTS ai_tv_devices/);
  assert.match(source,/\/v1\/pair\/revoke/);
  assert.match(source,/revoked_at IS NULL/);
});
test('recommendation catalog is bounded', () => {
  const source=readFileSync(new URL('./server.mjs',import.meta.url),'utf8');
  assert.match(source,/body\.catalog\.slice\(0, 40\)/);
  assert.match(source,/ids\.has\(String\(x\.id\)\)/);
});

test('streaming resolver requires authenticated TMDB identity and server-side Watchmode key',()=>{
  const source=readFileSync(new URL('./server.mjs',import.meta.url),'utf8');
  assert.match(source,/WATCHMODE_API_KEY/);
  assert.match(source,/\/v1\/sources\?/);
  assert.match(source,/Number\.isSafeInteger\(id\)/);
  assert.match(source,/validSourceUrl\(x\.android_tv_url\)/);
});

test('HTTP health accepts tracking parameters and source lookup rejects unauthenticated requests', async () => {
  const {spawn}=await import('node:child_process');
  const {once}=await import('node:events');
  const {createServer}=await import('node:net');
  const probe=createServer();
  probe.listen(0,'127.0.0.1');
  await once(probe,'listening');
  const port=probe.address().port;
  await new Promise((resolve,reject)=>probe.close(e=>e?reject(e):resolve()));
  const child=spawn(process.execPath,['server.mjs'],{
    cwd:new URL('.',import.meta.url),
    env:{...process.env,PORT:String(port),DATABASE_URL:'',AI_TV_DEVICE_TOKEN:'',AI_TV_SIGNING_SECRET:''},
    stdio:'ignore'
  });
  try {
    let health;
    for(let attempt=0;attempt<35;attempt++){
      if(child.exitCode!==null)throw new Error('gateway exited');
      try {health=await fetch('http://127.0.0.1:'+port+'/health?utm_source=chatgpt.com');break;}
      catch {await new Promise(r=>setTimeout(r,100));}
    }
    assert.ok(health,'gateway did not start');
    assert.equal(health.status,200);
    const body=await health.json();
    assert.equal(body.ok,true);
    assert.equal(typeof body.watchmodeConfigured,'boolean');
    const sources=await fetch('http://127.0.0.1:'+port+'/v1/sources?tmdbId=278&kind=movie&region=IN');
    assert.equal(sources.status,401);
    assert.equal((await sources.json()).error,'unauthorized');
    const missing=await fetch('http://127.0.0.1:'+port+'/');
    assert.equal(missing.status,404);
  } finally {
    child.kill();
    if(child.exitCode===null)await Promise.race([once(child,'exit'),new Promise(r=>setTimeout(r,1000))]);
  }
});

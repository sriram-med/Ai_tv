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

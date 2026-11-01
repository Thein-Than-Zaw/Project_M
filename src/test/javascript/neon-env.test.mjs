import { test } from 'node:test';
import assert from 'node:assert/strict';
import { jdbcConnection, neonEnvironment } from '../../../scripts/neon-env.mjs';

const direct = 'postgresql://owner:p%40ss%3Aword@ep-example.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require';
const pooled = direct.replace('ep-example.', 'ep-example-pooler.');
test('JDBC conversion decodes credentials and keeps them out of the URL', () => {
  const c = jdbcConnection(direct);
  assert.equal(c.username, 'owner');
  assert.equal(c.password, 'p@ss:word');
  assert.equal(c.url, 'jdbc:postgresql://ep-example.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require');
});
test('runtime uses Hikari on direct endpoint for reliable schema and isolated media', () => {
  const env = neonEnvironment({ DATABASE_URL: pooled, DATABASE_URL_UNPOOLED: direct, DB_PASSWORD: 'old-local-secret' });
  assert.doesNotMatch(env.DB_URL, /-pooler\./);
  assert.doesNotMatch(env.SPRING_FLYWAY_URL, /-pooler\./);
  assert.equal(env.DB_PASSWORD, 'p@ss:word');
  assert.equal(env.SERVER_PORT, '8082');
  assert.equal(env.MEMORY_STORAGE_ROOT, '.local/neon-uploads');
  assert.match(env.DB_URL, /currentSchema=couple_private_space/);
  assert.equal(env.SPRING_FLYWAY_DEFAULT_SCHEMA, 'couple_private_space');
  assert.equal(env.SPRING_DATASOURCE_HIKARI_SCHEMA, 'couple_private_space');
});
test('invalid, insecure, missing direct or pooled migration URLs fail closed', () => {
  for (const url of ['bad', direct.replace('sslmode=require', 'sslmode=disable'), 'https://example.com/']) {
    assert.throws(() => jdbcConnection(url));
  }
  assert.throws(() => neonEnvironment({ DATABASE_URL: pooled }));
  assert.throws(() => neonEnvironment({ DATABASE_URL: pooled, DATABASE_URL_UNPOOLED: pooled }));
  assert.throws(() => neonEnvironment({ DATABASE_URL: pooled, DATABASE_URL_UNPOOLED: direct.replace('/neondb?', '/other?') }));
});

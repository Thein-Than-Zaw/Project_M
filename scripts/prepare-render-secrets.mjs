import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { parseEnv } from 'node:util';
import { jdbcConnection } from './neon-env.mjs';

const source=existsSync('.env')?'.env':'.env.local';
const values=parseEnv(readFileSync(source,'utf8'));
jdbcConnection(values.DATABASE_URL_UNPOOLED); // Fail before writing insecure config.
const keys=['DATABASE_URL_UNPOOLED','AWS_ENDPOINT_URL_S3','AWS_REGION','AWS_ACCESS_KEY_ID','AWS_SECRET_ACCESS_KEY'];
const escape=value=>value.replaceAll('\\','\\\\').replaceAll('\r','\\r').replaceAll('\n','\\n');
const lines=keys.map(key=>{
  if(!values[key])throw new Error(`Missing ${key}; no secret values logged.`);
  return `${key}=${escape(values[key])}`;
});
lines.push(`MEMORY_S3_BUCKET=${escape(values.MEMORY_S3_BUCKET||'couple-memories')}`);
mkdirSync('.local',{recursive:true});
writeFileSync('.local/render-private-space.properties',lines.join('\n')+'\n',{mode:0o600});
console.log('Prepared ignored Render runtime secret file (6 keys); values not printed.');

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
// Relay credentials stay in the runtime secret file, never in Docker/Git/argv.
const turn=existsSync('.local/express-turn.env')?parseEnv(readFileSync('.local/express-turn.env','utf8')):{};
for(const key of ['STUN_URLS','TURN_URLS','TURN_USERNAME','TURN_PASSWORD','TURN_SHARED_SECRET']){
  const value=turn[key]||values[key];if(value)lines.push(`${key}=${escape(value)}`);
}
mkdirSync('.local',{recursive:true});
writeFileSync('.local/render-private-space.properties',lines.join('\n')+'\n',{mode:0o600});
console.log(`Prepared ignored Render runtime secret file (${lines.length} keys); values not printed.`);

import { existsSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { delimiter } from 'node:path';

const file = existsSync('.env') ? '.env' : '.env.local';
if (existsSync(file)) process.loadEnvFile(file);
for (const key of ['AWS_ENDPOINT_URL_S3', 'AWS_REGION', 'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY']) {
  if (!process.env[key]) { console.error(`Missing ${key}; provision the private Neon bucket first.`); process.exit(1); }
}
const windowsMaven = existsSync('.tools/apache-maven-3.9.12/bin/mvn.cmd') ? '.tools\\apache-maven-3.9.12\\bin\\mvn.cmd' : 'mvnw.cmd';
const args = ['-B', '-ntp', '-DskipTests', 'compile', 'dependency:build-classpath', '-Dmdep.outputFile=.local/storage-classpath.txt'];
const build = process.platform === 'win32'
  ? spawnSync('cmd.exe', ['/d', '/s', '/c', `${windowsMaven} ${args.join(' ')}`], { stdio: 'inherit' })
  : spawnSync('./mvnw', args, { stdio: 'inherit' });
if (build.status !== 0) process.exit(build.status ?? 1);
const classpath = `target/classes${delimiter}${readFileSync('.local/storage-classpath.txt', 'utf8').trim()}`;
const probe = spawnSync('java', ['--class-path', classpath, 'scripts/StorageProbe.java'], { stdio: 'inherit' });
process.exit(probe.status ?? 1);

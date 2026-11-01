import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { neonEnvironment } from './neon-env.mjs';

const action = process.argv[2];
let child;
try {
  if (action === 'build') {
    // Packaging compiles application and test sources; database tests run separately.
    const windowsMaven = existsSync('.tools/apache-maven-3.9.12/bin/mvn.cmd')
      ? '.tools\\apache-maven-3.9.12\\bin\\mvn.cmd' : 'mvnw.cmd';
    child = process.platform === 'win32'
      ? spawn('cmd.exe', ['/d', '/s', '/c', `${windowsMaven} -B -ntp -DskipTests package`], { stdio: 'inherit' })
      : spawn('./mvnw', ['-B', '-ntp', '-DskipTests', 'package'], { stdio: 'inherit' });
  } else if (['start:neon', 'check:neon', 'setup:neon'].includes(action)) {
    const file = existsSync('.env') ? '.env' : '.env.local';
    if (existsSync(file)) process.loadEnvFile(file);
    if (action === 'check:neon' || action === 'setup:neon') {
      const source = action === 'check:neon' ? ['scripts/NeonProbe.java'] : ['scripts/IssueSetupToken.java', '.local/neon-setup-token.txt'];
      child = spawn('java', ['--class-path', '.tools/m2/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar', ...source], { stdio: 'inherit', env: neonEnvironment(process.env) });
    } else {
      const jar = 'target/private-space-0.1.0-SNAPSHOT.jar';
      if (!existsSync(jar)) throw new Error('Run npm run build first.');
      child = spawn('java', ['-jar', jar], { stdio: 'inherit', env: neonEnvironment(process.env) });
    }
  } else {
    throw new Error('Supported actions: build, start:neon, check:neon, setup:neon.');
  }
  child.on('error', () => { console.error('Could not start Maven/Java. Check the installed JDK and PATH.'); process.exitCode = 1; });
  child.on('exit', (code) => { process.exitCode = code ?? 1; });
  process.on('SIGINT', () => child.kill('SIGINT'));
  process.on('SIGTERM', () => child.kill('SIGTERM'));
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}

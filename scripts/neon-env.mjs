// Neon URLs stay in the process environment, never command arguments or logs.
export function jdbcConnection(value) {
  let url;
  try { url = new URL(value); } catch { throw new Error('Invalid Neon database URL.'); }
  if (!['postgres:', 'postgresql:'].includes(url.protocol) || !url.hostname || !url.username || !url.password) {
    throw new Error('Expected a PostgreSQL URL with credentials.');
  }
  if (!['require', 'verify-ca', 'verify-full'].includes(url.searchParams.get('sslmode'))) {
    throw new Error('Neon connection must require TLS.');
  }
  const username = decodeURIComponent(url.username);
  const password = decodeURIComponent(url.password);
  url.username = '';
  url.password = '';
  return { url: `jdbc:postgresql://${url.host}${url.pathname}${url.search}`, username, password };
}

export function neonEnvironment(env) {
  const runtime = jdbcConnection(env.DATABASE_URL);
  const migration = jdbcConnection(env.DATABASE_URL_UNPOOLED);
  if (new URL(env.DATABASE_URL_UNPOOLED).hostname.includes('-pooler.')) {
    throw new Error('Flyway requires DATABASE_URL_UNPOOLED to use a direct endpoint.');
  }
  const runtimeUrl = new URL(env.DATABASE_URL);
  const migrationUrl = new URL(env.DATABASE_URL_UNPOOLED);
  if (runtimeUrl.pathname !== migrationUrl.pathname ||
      runtimeUrl.hostname.replace('-pooler.', '.') !== migrationUrl.hostname) {
    throw new Error('Runtime and migration URLs must target the same Neon endpoint and database.');
  }
  const schemaUrl = (jdbc) => {
    const separator = jdbc.includes('?') ? '&' : '?';
    if (/[?&]currentSchema=/i.test(jdbc)) throw new Error('Remove currentSchema from Neon URLs; the launcher isolates the app schema.');
    return `${jdbc}${separator}currentSchema=couple_private_space`;
  };
  return {
    ...env,
    // This long-running JVM keeps schema session state on direct connections
    // in its existing bounded Hikari pool. Set the schema explicitly below;
    // URL currentSchema alone did not take effect in the live probe.
    DB_URL: schemaUrl(migration.url), DB_USERNAME: migration.username, DB_PASSWORD: migration.password,
    SPRING_FLYWAY_URL: schemaUrl(migration.url),
    SPRING_FLYWAY_USER: migration.username,
    SPRING_FLYWAY_PASSWORD: migration.password,
    SPRING_FLYWAY_DEFAULT_SCHEMA: 'couple_private_space',
    SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA: 'couple_private_space',
    SERVER_ADDRESS: '127.0.0.1',
    SERVER_PORT: env.SERVER_PORT || '8082',
    // A separate database must not share local-development asset cleanup/storage.
    MEMORY_STORAGE_ROOT: env.NEON_MEMORY_STORAGE_ROOT || '.local/neon-uploads',
    SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT: '30000',
    SPRING_DATASOURCE_HIKARI_SCHEMA: 'couple_private_space',
  };
}

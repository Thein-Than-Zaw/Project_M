# Neon agent setup audit — 2026-11-01

User explicitly requested fetching and following
https://neon.com/.well-known/agent-skills/neon/SKILL.md,
checking installed Neon agent skills/MCP, installing missing pieces, preserving
existing configuration and reporting changes.

## Actual checks

- Fetched that exact HTTPS URL through the owner's already-configured Windows
  proxy into ignored `.local/neon-requested-skill.md` and read it. The web fetch
  helper could not access the Markdown endpoint; the direct HTTPS fetch succeeded.
- Its normalized text matches the installed `C:/Users/USER/.agents/skills/neon/SKILL.md`.
- `scripts/neon-cli.ps1 skills status --global` reported all nine user-level skills
  **up to date**: neon, neon-ai-gateway, neon-auth, neon-functions,
  neon-object-storage, neon-postgres, neon-postgres-branches,
  neon-postgres-egress-optimizer and neon-realtime.
- Codex's existing `neon-postgres@plugins-cli` plugin is enabled. Installed package
  `neon-postgres/2.3.0/mcp.json` already declares the `neon` MCP server at
  `https://mcp.neon.tech/mcp`. It is a user-level plugin available across projects.
- No project-local `.codex/config.toml`, `.mcp.json` or duplicate skill installation
  was needed. Followed the fetched skill's instruction: if init installed the
  Neon plugin, do not also run neon mcp/neon skills for the same agent.
- Existing Codex configuration (including its node_repl MCP server) was inspected
  without exposing credentials and was not edited. No additional MCP key was
  minted and no unrelated tool/service was enabled by this audit.
- MCP registration/package presence is verified; an authenticated MCP query is not
  claimed. CLI is the skill's preferred path and its browser authentication succeeded.

## Changes during the ongoing HOST-01 work

- Added process-local Windows proxy support in `scripts/neon-cli.ps1`. It restores
  prior process env afterward; no global Windows/Node proxy/TLS settings changed.
- CLI DEFAULT browser-auth profile was authenticated after one 60-second expiry.
  Credentials remain in the CLI's private user configuration, not source.
- Preserved existing neon.ts branch policy; added only private `couple-memories`.
  Reviewed plan showed just `+ bucket couple-memories`, then deploy applied it.
- Used `deploy --no-env-pull` and pulled to a separate ignored file before adding
  only missing AWS_* and MEMORY_S3_BUCKET settings to `.env.local`. Verified all
  prior env values remained exactly equal. No database reset/relink/role replacement.
- No new Neon skill/MCP installation was performed in response to this audit:
  all requested tooling was already installed. The newly fetched Markdown is a
  local reference, not an overwrite of an installed skill.
- Live private storage upload/readback/anonymous-denial/delete evidence is recorded
  separately from the earlier database setup and from untested public Render flows.

The existing Java/Spring Security/Flyway/STOMP architecture was preserved as the
skill directs for established apps. No Next.js rewrite, Neon Auth migration, Data
API, Functions, AI Gateway or Neon Realtime provision was introduced.

HOST-01 implementation changes, tests and remaining Render work are described in
`HOST-01_Checkpoint.md` and `HOST-01_Render_Setup.md`.

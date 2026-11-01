import { defineConfig } from "@neon/config/v1";

export default defineConfig({
  // HOST-01: private Memory originals/thumbnails; no anonymous reads.
  buckets: { "couple-memories": {} },
  // Declare your Neon services here
  // PostgreSQL is built into every branch. Existing Spring Security owns auth;
  // leave other Neon service settings unspecified rather than disabling them.
  // Branch policy: per-branch tuning
  branch: (branch) => {
    if (branch.isDefault) {
      // Default branch: no overrides, uses project defaults
      return {};
    }
    if (!branch.exists) {
      // New non-default branches: auto-expire
      // Run `neon checkout <name>` to create a new branch with these settings
      return { ttl: "7d" };
    }
    // Existing branch: no changes
    return {};
  },
});

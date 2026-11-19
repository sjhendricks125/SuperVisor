import "dotenv/config";
import { createClient } from "@supabase/supabase-js";
import { createApp } from "./app.js";
import type { Database } from "./database.js";
import { SupabaseSyncStore } from "./sync-store.js";

function requiredEnvironmentVariable(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function portFromEnvironment(): number {
  const rawPort = process.env.PORT ?? "3000";
  const port = Number(rawPort);
  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error("PORT must be an integer between 1 and 65535");
  }
  return port;
}

const supabase = createClient<Database>(
  requiredEnvironmentVariable("SUPABASE_URL"),
  requiredEnvironmentVariable("SUPABASE_SERVICE_ROLE_KEY"),
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  },
);

const app = createApp(new SupabaseSyncStore(supabase));

try {
  await app.listen({
    host: process.env.HOST ?? "127.0.0.1",
    port: portFromEnvironment(),
  });
} catch (error) {
  app.log.error({ err: error }, "API startup failed");
  process.exitCode = 1;
}

import type { SupabaseClient } from "@supabase/supabase-js";
import type {
  SyncDataset,
  SyncRequest,
  SyncSummary,
} from "./contracts.js";
import type { Database } from "./database.js";
import { mapSyncRows, summarizeSyncRows } from "./sync-mapping.js";

export interface SyncStore {
  authenticate(accessToken: string): Promise<string | null>;
  upload(userId: string, request: SyncRequest): Promise<SyncSummary>;
}

export class AuthServiceError extends Error {
  constructor(options?: ErrorOptions) {
    super("Supabase authentication is unavailable", options);
    this.name = "AuthServiceError";
  }
}

export class SyncWriteError extends Error {
  constructor(
    readonly dataset: SyncDataset,
    options?: ErrorOptions,
  ) {
    super(`Supabase write failed for ${dataset}`, options);
    this.name = "SyncWriteError";
  }
}

export class SupabaseSyncStore implements SyncStore {
  constructor(private readonly client: SupabaseClient<Database>) {}

  async authenticate(accessToken: string): Promise<string | null> {
    try {
      const { data, error } = await this.client.auth.getUser(accessToken);
      if (error) {
        if (error.status === 400 || error.status === 401 || error.status === 403) {
          return null;
        }

        throw new AuthServiceError({ cause: error });
      }

      return data.user?.id ?? null;
    } catch (error) {
      if (error instanceof AuthServiceError) {
        throw error;
      }

      throw new AuthServiceError({ cause: error });
    }
  }

  async upload(userId: string, request: SyncRequest): Promise<SyncSummary> {
    const rows = mapSyncRows(userId, request);

    if (rows.locationPings.length > 0) {
      await this.write(
        "locationPings",
        this.client.from("location_pings").upsert(rows.locationPings, {
          onConflict: "user_id,device_id,client_record_id",
        }),
      );
    }
    if (rows.batteryPings.length > 0) {
      await this.write(
        "batteryPings",
        this.client.from("battery_pings").upsert(rows.batteryPings, {
          onConflict: "user_id,device_id,client_record_id",
        }),
      );
    }
    if (rows.installedApps.length > 0) {
      await this.write(
        "installedApps",
        this.client.from("installed_apps").upsert(rows.installedApps, {
          onConflict: "user_id,device_id,client_record_id",
        }),
      );
    }
    if (rows.appChangeEvents.length > 0) {
      await this.write(
        "appChangeEvents",
        this.client.from("app_change_events").upsert(rows.appChangeEvents, {
          onConflict: "user_id,device_id,client_record_id",
        }),
      );
    }
    if (rows.messageNotificationLogs.length > 0) {
      await this.write(
        "messageNotificationLogs",
        this.client.from("message_notification_logs").upsert(
          rows.messageNotificationLogs,
          { onConflict: "user_id,device_id,client_record_id" },
        ),
      );
    }

    return summarizeSyncRows(rows);
  }

  private async write(
    dataset: SyncDataset,
    query: PromiseLike<{ error: { message: string } | null }>,
  ): Promise<void> {
    try {
      const { error } = await query;
      if (error) {
        throw error;
      }
    } catch (error) {
      throw new SyncWriteError(dataset, { cause: error });
    }
  }
}

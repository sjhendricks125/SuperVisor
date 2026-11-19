import type { SyncRequest } from "./contracts.js";

type Table<Row, Insert> = {
  Row: Row;
  Insert: Insert;
  Update: Partial<Insert>;
  Relationships: [];
};

type LocationPing = SyncRequest["locationPings"][number];
type BatteryPing = SyncRequest["batteryPings"][number];
type InstalledApp = SyncRequest["installedApps"][number];
type AppChangeEvent = SyncRequest["appChangeEvents"][number];
type MessageNotificationLog =
  SyncRequest["messageNotificationLogs"][number];

type LocationPingRow = Omit<LocationPing, "id" | "isSynced"> & {
  id: number;
  user_id: string;
  device_id: string;
  client_record_id: number;
  isSynced: boolean;
};

type BatteryPingRow = Omit<BatteryPing, "id" | "isSynced"> & {
  id: number;
  user_id: string;
  device_id: string;
  client_record_id: number;
  isSynced: boolean;
};

type InstalledAppRow = InstalledApp & {
  user_id: string;
  device_id: string;
  client_record_id: string;
};

type AppChangeEventRow = AppChangeEvent & {
  user_id: string;
  device_id: string;
  client_record_id: number;
};

type MessageNotificationLogRow = Omit<MessageNotificationLog, "id"> & {
  id: number;
  user_id: string;
  device_id: string;
  client_record_id: number;
};

export interface Database {
  public: {
    Tables: {
      /**
       * Requires user_id, device_id, client_record_id and a matching unique
       * constraint. latitude must be nullable and isSynced must be boolean.
       */
      location_pings: Table<
        LocationPingRow,
        Omit<LocationPingRow, "id"> & { id?: number }
      >;
      /**
       * Requires the owner/source columns and unique constraint. Update the
       * existing plugType, Boolean, and temperature column types/names.
       */
      battery_pings: Table<
        BatteryPingRow,
        Omit<BatteryPingRow, "id"> & { id?: number }
      >;
      /**
       * Requires the owner/source columns and unique constraint. Replace the
       * existing packageName-only primary key for per-user/device snapshots.
       */
      installed_apps: Table<InstalledAppRow, InstalledAppRow>;
      /** Requires the owner/source columns and unique constraint. */
      app_change_events: Table<
        AppChangeEventRow,
        Omit<AppChangeEventRow, "id"> & { id?: number }
      >;
      /**
       * Requires the owner/source columns and unique constraint. Rename the
       * existing misspelled relation to message_notification_logs.
       */
      message_notification_logs: Table<
        MessageNotificationLogRow,
        Omit<MessageNotificationLogRow, "id"> & { id?: number }
      >;
    };
    Views: Record<string, never>;
    Functions: Record<string, never>;
    Enums: Record<string, never>;
    CompositeTypes: Record<string, never>;
  };
}

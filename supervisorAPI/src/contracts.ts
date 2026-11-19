import { Static, Type } from "@sinclair/typebox";

const MAX_SAFE_INTEGER = Number.MAX_SAFE_INTEGER;
const MAX_SYNC_ROWS = 1_000;

const RecordIdSchema = Type.Integer({ minimum: 1, maximum: MAX_SAFE_INTEGER });
const TimestampSchema = Type.Integer({ minimum: 0, maximum: MAX_SAFE_INTEGER });
const NullableNumberSchema = Type.Union([Type.Number(), Type.Null()]);
const NullableStringSchema = Type.Union([Type.String(), Type.Null()]);
const NullableBooleanSchema = Type.Union([Type.Boolean(), Type.Null()]);

const LocationPingSchema = Type.Object(
  {
    id: RecordIdSchema,
    timestamp: TimestampSchema,
    latitude: Type.Union([
      Type.Number({ minimum: -90, maximum: 90 }),
      Type.Null(),
    ]),
    longitude: Type.Union([
      Type.Number({ minimum: -180, maximum: 180 }),
      Type.Null(),
    ]),
    altitude: NullableNumberSchema,
    accuracyRadius: Type.Union([Type.Number({ minimum: 0 }), Type.Null()]),
    signalQuality: Type.String(),
    status: Type.String(),
    // Kept for local-entity compatibility; the API stores true after upload.
    isSynced: Type.Optional(Type.Boolean()),
  },
  { additionalProperties: false },
);

const BatteryPingSchema = Type.Object(
  {
    id: RecordIdSchema,
    timestamp: TimestampSchema,
    levelPercent: Type.Union([
      Type.Integer({ minimum: 0, maximum: 100 }),
      Type.Null(),
    ]),
    isCharging: NullableBooleanSchema,
    plugType: NullableStringSchema,
    health: NullableStringSchema,
    temperatureCelsius: NullableNumberSchema,
    voltageMillivolts: Type.Union([
      Type.Integer({ minimum: 0 }),
      Type.Null(),
    ]),
    isPowerSaveMode: NullableBooleanSchema,
    levelCategory: Type.String(),
    status: Type.String(),
    // Kept for local-entity compatibility; the API stores true after upload.
    isSynced: Type.Optional(Type.Boolean()),
  },
  { additionalProperties: false },
);

const InstalledAppSchema = Type.Object(
  {
    packageName: Type.String({ minLength: 1 }),
    appName: Type.String(),
    versionName: Type.String(),
    isSystemApp: Type.Boolean(),
    firstSeen: TimestampSchema,
    lastSeen: TimestampSchema,
  },
  { additionalProperties: false },
);

const AppChangeEventSchema = Type.Object(
  {
    id: RecordIdSchema,
    timestamp: TimestampSchema,
    packageName: Type.String({ minLength: 1 }),
    appName: NullableStringSchema,
    versionName: NullableStringSchema,
    isSystemApp: NullableBooleanSchema,
    eventType: Type.String(),
  },
  { additionalProperties: false },
);

const MessageNotificationLogSchema = Type.Object(
  {
    id: RecordIdSchema,
    sender: Type.String(),
    timestamp: TimestampSchema,
  },
  { additionalProperties: false },
);

export const SyncRequestSchema = Type.Object(
  {
    deviceId: Type.String({ format: "uuid" }),
    locationPings: Type.Array(LocationPingSchema, { maxItems: MAX_SYNC_ROWS }),
    batteryPings: Type.Array(BatteryPingSchema, { maxItems: MAX_SYNC_ROWS }),
    installedApps: Type.Array(InstalledAppSchema, { maxItems: MAX_SYNC_ROWS }),
    appChangeEvents: Type.Array(AppChangeEventSchema, {
      maxItems: MAX_SYNC_ROWS,
    }),
    messageNotificationLogs: Type.Array(MessageNotificationLogSchema, {
      maxItems: MAX_SYNC_ROWS,
    }),
  },
  { additionalProperties: false },
);

export type LocationPing = Static<typeof LocationPingSchema>;
export type BatteryPing = Static<typeof BatteryPingSchema>;
export type InstalledApp = Static<typeof InstalledAppSchema>;
export type AppChangeEvent = Static<typeof AppChangeEventSchema>;
export type MessageNotificationLog = Static<
  typeof MessageNotificationLogSchema
>;
export type SyncRequest = Static<typeof SyncRequestSchema>;
export type SyncDataset = keyof SyncSummary;

export interface SyncSummary {
  locationPings: number;
  batteryPings: number;
  installedApps: number;
  appChangeEvents: number;
  messageNotificationLogs: number;
}

export const MAX_SYNC_ROWS_PER_REQUEST = MAX_SYNC_ROWS;

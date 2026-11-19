import type { SyncRequest, SyncSummary } from "./contracts.js";
import type { Database } from "./database.js";

type Tables = Database["public"]["Tables"];

export interface SyncRows {
  locationPings: Tables["location_pings"]["Insert"][];
  batteryPings: Tables["battery_pings"]["Insert"][];
  installedApps: Tables["installed_apps"]["Insert"][];
  appChangeEvents: Tables["app_change_events"]["Insert"][];
  messageNotificationLogs: Tables["message_notification_logs"]["Insert"][];
}

export function mapSyncRows(
  userId: string,
  request: SyncRequest,
): SyncRows {
  return {
    locationPings: request.locationPings.map((row) => ({
      user_id: userId,
      device_id: request.deviceId,
      client_record_id: row.id,
      timestamp: row.timestamp,
      latitude: row.latitude,
      longitude: row.longitude,
      altitude: row.altitude,
      accuracyRadius: row.accuracyRadius,
      signalQuality: row.signalQuality,
      status: row.status,
      isSynced: true,
    })),
    batteryPings: request.batteryPings.map((row) => ({
      user_id: userId,
      device_id: request.deviceId,
      client_record_id: row.id,
      timestamp: row.timestamp,
      levelPercent: row.levelPercent,
      isCharging: row.isCharging,
      plugType: row.plugType,
      health: row.health,
      temperatureCelsius: row.temperatureCelsius,
      voltageMillivolts: row.voltageMillivolts,
      isPowerSaveMode: row.isPowerSaveMode,
      levelCategory: row.levelCategory,
      status: row.status,
      isSynced: true,
    })),
    installedApps: request.installedApps.map((row) => ({
      user_id: userId,
      device_id: request.deviceId,
      client_record_id: row.packageName,
      packageName: row.packageName,
      appName: row.appName,
      versionName: row.versionName,
      isSystemApp: row.isSystemApp,
      firstSeen: row.firstSeen,
      lastSeen: row.lastSeen,
    })),
    appChangeEvents: request.appChangeEvents.map((row) => ({
      user_id: userId,
      device_id: request.deviceId,
      client_record_id: row.id,
      timestamp: row.timestamp,
      packageName: row.packageName,
      appName: row.appName,
      versionName: row.versionName,
      isSystemApp: row.isSystemApp,
      eventType: row.eventType,
    })),
    messageNotificationLogs: request.messageNotificationLogs.map((row) => ({
      user_id: userId,
      device_id: request.deviceId,
      client_record_id: row.id,
      sender: row.sender,
      timestamp: row.timestamp,
    })),
  };
}

export function summarizeSyncRows(rows: SyncRows): SyncSummary {
  return {
    locationPings: rows.locationPings.length,
    batteryPings: rows.batteryPings.length,
    installedApps: rows.installedApps.length,
    appChangeEvents: rows.appChangeEvents.length,
    messageNotificationLogs: rows.messageNotificationLogs.length,
  };
}

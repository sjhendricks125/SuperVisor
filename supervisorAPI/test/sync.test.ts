import assert from "node:assert/strict";
import test from "node:test";
import { createApp } from "../src/app.js";
import type { SyncRequest, SyncSummary } from "../src/contracts.js";
import { mapSyncRows } from "../src/sync-mapping.js";
import {
  AuthServiceError,
  SyncWriteError,
  type SyncStore,
} from "../src/sync-store.js";

const deviceId = "4e1b5b5e-6888-4ed1-bf6d-93b1f74016f8";

const emptyRequest: SyncRequest = {
  deviceId,
  locationPings: [],
  batteryPings: [],
  installedApps: [],
  appChangeEvents: [],
  messageNotificationLogs: [],
};

const emptySummary: SyncSummary = {
  locationPings: 0,
  batteryPings: 0,
  installedApps: 0,
  appChangeEvents: 0,
  messageNotificationLogs: 0,
};

function createStore(overrides: Partial<SyncStore> = {}): SyncStore {
  return {
    authenticate: async (token) => (token === "valid-token" ? "user-123" : null),
    upload: async () => emptySummary,
    ...overrides,
  };
}

test("health endpoint reports readiness", async () => {
  const app = createApp(createStore(), { logger: false });
  try {
    const response = await app.inject({ method: "GET", url: "/health" });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { status: "ok" });
  } finally {
    await app.close();
  }
});

test("sync requires a valid bearer token", async () => {
  const app = createApp(createStore(), { logger: false });
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      payload: emptyRequest,
    });
    assert.equal(response.statusCode, 401);
    assert.deepEqual(response.json(), { error: "unauthorized" });
  } finally {
    await app.close();
  }
});

test("sync authenticates first and returns accepted row counts", async () => {
  let uploadedUserId: string | undefined;
  let uploadedRequest: SyncRequest | undefined;
  const summary: SyncSummary = {
    ...emptySummary,
    locationPings: 1,
  };
  const request: SyncRequest = {
    ...emptyRequest,
    locationPings: [
      {
        id: 8,
        timestamp: 1_700_000_000_000,
        latitude: null,
        longitude: -71.1,
        altitude: null,
        accuracyRadius: null,
        signalQuality: "good",
        status: "ok",
        isSynced: false,
      },
    ],
  };
  const store = createStore({
    upload: async (userId, body) => {
      uploadedUserId = userId;
      uploadedRequest = body;
      return summary;
    },
  });
  const app = createApp(store, { logger: false });
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: request,
    });
    assert.equal(response.statusCode, 200);
    assert.deepEqual(response.json(), { deviceId, accepted: summary });
    assert.equal(uploadedUserId, "user-123");
    assert.deepEqual(uploadedRequest, request);
  } finally {
    await app.close();
  }
});

test("sync rejects requests over the total row limit", async () => {
  let uploadCalled = false;
  const request: SyncRequest = {
    ...emptyRequest,
    locationPings: Array.from({ length: 500 }, (_, index) => ({
      id: index + 1,
      timestamp: 1_700_000_000_000,
      latitude: null,
      longitude: null,
      altitude: null,
      accuracyRadius: null,
      signalQuality: "good",
      status: "ok",
    })),
    batteryPings: Array.from({ length: 501 }, (_, index) => ({
      id: index + 1,
      timestamp: 1_700_000_000_000,
      levelPercent: null,
      isCharging: null,
      plugType: null,
      health: null,
      temperatureCelsius: null,
      voltageMillivolts: null,
      isPowerSaveMode: null,
      levelCategory: "unknown",
      status: "ok",
    })),
  };
  const store = createStore({
    upload: async () => {
      uploadCalled = true;
      return emptySummary;
    },
  });
  const app = createApp(store, { logger: false });
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: request,
    });
    assert.equal(response.statusCode, 413);
    assert.deepEqual(response.json(), {
      error: "too_many_records",
      maxRows: 1_000,
    });
    assert.equal(uploadCalled, false);
  } finally {
    await app.close();
  }
});

test("sync rejects malformed client records", async () => {
  const app = createApp(createStore(), { logger: false });
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: { ...emptyRequest, extraField: true },
    });
    assert.equal(response.statusCode, 400);
  } finally {
    await app.close();
  }
});

test("sync rejects a non-UUID device ID", async () => {
  const app = createApp(createStore(), { logger: false });
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: { ...emptyRequest, deviceId: "not-a-uuid" },
    });
    assert.equal(response.statusCode, 400);
  } finally {
    await app.close();
  }
});

test("sync rejects duplicate source IDs within a dataset", async () => {
  let uploadCalled = false;
  const request: SyncRequest = {
    ...emptyRequest,
    locationPings: [
      {
        id: 8,
        timestamp: 1_700_000_000_000,
        latitude: null,
        longitude: null,
        altitude: null,
        accuracyRadius: null,
        signalQuality: "good",
        status: "ok",
      },
      {
        id: 8,
        timestamp: 1_700_000_000_001,
        latitude: null,
        longitude: null,
        altitude: null,
        accuracyRadius: null,
        signalQuality: "good",
        status: "ok",
      },
    ],
  };
  const app = createApp(
    createStore({
      upload: async () => {
        uploadCalled = true;
        return emptySummary;
      },
    }),
    { logger: false },
  );
  try {
    const response = await app.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: request,
    });
    assert.equal(response.statusCode, 400);
    assert.deepEqual(response.json(), {
      error: "duplicate_client_record_id",
      dataset: "locationPings",
    });
    assert.equal(uploadCalled, false);
  } finally {
    await app.close();
  }
});

test("upstream failures return explicit service errors", async () => {
  const authApp = createApp(
    createStore({
      authenticate: async () => {
        throw new AuthServiceError();
      },
    }),
    { logger: false },
  );
  const syncApp = createApp(
    createStore({
      upload: async () => {
        throw new SyncWriteError("batteryPings");
      },
    }),
    { logger: false },
  );
  try {
    const authResponse = await authApp.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: emptyRequest,
    });
    assert.equal(authResponse.statusCode, 503);
    assert.deepEqual(authResponse.json(), {
      error: "authentication_unavailable",
    });

    const syncResponse = await syncApp.inject({
      method: "POST",
      url: "/api/v1/sync",
      headers: { authorization: "Bearer valid-token" },
      payload: emptyRequest,
    });
    assert.equal(syncResponse.statusCode, 502);
    assert.deepEqual(syncResponse.json(), {
      error: "sync_write_failed",
      dataset: "batteryPings",
      retryWholeBatchSafely: true,
    });
  } finally {
    await Promise.all([authApp.close(), syncApp.close()]);
  }
});

test("mapping uses user/device/client ids and marks pings synced", () => {
  const rows = mapSyncRows("user-123", {
    ...emptyRequest,
    batteryPings: [
      {
        id: 5,
        timestamp: 1_700_000_000_000,
        levelPercent: 80,
        isCharging: true,
        plugType: "USB",
        health: "GOOD",
        temperatureCelsius: 21.5,
        voltageMillivolts: 4_000,
        isPowerSaveMode: false,
        levelCategory: "high",
        status: "ok",
        isSynced: false,
      },
    ],
    installedApps: [
      {
        packageName: "com.example.app",
        appName: "Example",
        versionName: "1.0",
        isSystemApp: false,
        firstSeen: 1_700_000_000_000,
        lastSeen: 1_700_000_000_000,
      },
    ],
  });

  assert.deepEqual(rows.batteryPings[0], {
    user_id: "user-123",
    device_id: deviceId,
    client_record_id: 5,
    timestamp: 1_700_000_000_000,
    levelPercent: 80,
    isCharging: true,
    plugType: "USB",
    health: "GOOD",
    temperatureCelsius: 21.5,
    voltageMillivolts: 4_000,
    isPowerSaveMode: false,
    levelCategory: "high",
    status: "ok",
    isSynced: true,
  });
  assert.equal(rows.installedApps[0]?.client_record_id, "com.example.app");
});

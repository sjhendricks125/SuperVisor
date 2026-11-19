import Fastify from "fastify";
import type { TypeBoxTypeProvider } from "@fastify/type-provider-typebox";
import {
  MAX_SYNC_ROWS_PER_REQUEST,
  SyncRequestSchema,
} from "./contracts.js";
import type { SyncDataset, SyncRequest } from "./contracts.js";
import type { SyncStore } from "./sync-store.js";
import { AuthServiceError, SyncWriteError } from "./sync-store.js";

const BODY_LIMIT_BYTES = 1_048_576;

function findDuplicateDataset(request: SyncRequest): SyncDataset | null {
  const recordIds: Array<[SyncDataset, Array<number | string>]> = [
    ["locationPings", request.locationPings.map(({ id }) => id)],
    ["batteryPings", request.batteryPings.map(({ id }) => id)],
    ["installedApps", request.installedApps.map(({ packageName }) => packageName)],
    ["appChangeEvents", request.appChangeEvents.map(({ id }) => id)],
    [
      "messageNotificationLogs",
      request.messageNotificationLogs.map(({ id }) => id),
    ],
  ];

  return (
    recordIds.find(([, ids]) => new Set(ids).size !== ids.length)?.[0] ?? null
  );
}

export function createApp(
  syncStore: SyncStore,
  options: { logger?: boolean } = {},
) {
  const app = Fastify({
    logger: options.logger ?? true,
    bodyLimit: BODY_LIMIT_BYTES,
    ajv: {
      customOptions: {
        coerceTypes: false,
        removeAdditional: false,
        useDefaults: false,
      },
    },
  }).withTypeProvider<TypeBoxTypeProvider>();

  app.get("/health", async () => ({ status: "ok" }));

  app.post(
    "/api/v1/sync",
    { schema: { body: SyncRequestSchema } },
    async (request, reply) => {
      const accessToken = /^Bearer ([^\s]+)$/i.exec(
        request.headers.authorization ?? "",
      )?.[1];
      if (!accessToken) {
        return reply.code(401).send({ error: "unauthorized" });
      }

      let userId: string | null;
      try {
        userId = await syncStore.authenticate(accessToken);
      } catch (error) {
        if (!(error instanceof AuthServiceError)) {
          throw error;
        }
        request.log.error({ err: error }, "Supabase authentication failed");
        return reply.code(503).send({ error: "authentication_unavailable" });
      }

      if (!userId) {
        return reply.code(401).send({ error: "unauthorized" });
      }

      const totalRows =
        request.body.locationPings.length +
        request.body.batteryPings.length +
        request.body.installedApps.length +
        request.body.appChangeEvents.length +
        request.body.messageNotificationLogs.length;
      if (totalRows > MAX_SYNC_ROWS_PER_REQUEST) {
        return reply.code(413).send({
          error: "too_many_records",
          maxRows: MAX_SYNC_ROWS_PER_REQUEST,
        });
      }

      const duplicateDataset = findDuplicateDataset(request.body);
      if (duplicateDataset) {
        return reply.code(400).send({
          error: "duplicate_client_record_id",
          dataset: duplicateDataset,
        });
      }

      try {
        const accepted = await syncStore.upload(userId, request.body);
        return reply.send({ deviceId: request.body.deviceId, accepted });
      } catch (error) {
        if (!(error instanceof SyncWriteError)) {
          throw error;
        }
        request.log.error(
          { err: error, dataset: error.dataset },
          "Supabase sync write failed",
        );
        return reply.code(502).send({
          error: "sync_write_failed",
          dataset: error.dataset,
          retryWholeBatchSafely: true,
        });
      }
    },
  );

  return app;
}

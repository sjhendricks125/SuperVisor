# SuperVisor

## API Usage 

| Google Play services Location API (Fused Location Provider) | Obtains the device's current location using high-accuracy priority. |
| Android framework location and permission APIs | Checks fine/coarse location permission and whether system location services are enabled. |
| Android foreground-service and notification APIs | Runs periodic location monitoring as a location foreground service with an ongoing notification. |
| AndroidX Room | Stores location pings locally in the app's SQLite database. |
| AndroidX WorkManager | Schedules daily maintenance that removes location pings older than 30 days. |
| Contacts | Android ContactsContract API | Retrieves contact names and phone numbers stored on the device. |
| Contacts | Android ContentResolver API | Queries the device's contacts provider. |
| Battery | Android BatteryManager and sticky ACTION_BATTERY_CHANGED broadcast. | Reads Battery Details |
There is **no SuperVisor backend or HTTP API integration implemented** 
`ApiSyncClient` is only a placeholder interface 
`SyncWorker` contains a TODO for future synchronization

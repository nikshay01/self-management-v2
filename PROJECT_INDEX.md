# Project Index

## What this folder contains

This workspace currently centers on an Android companion app that reads Samsung Health data and uploads normalized records to the Self Management backend. It also contains an older browser based input prototype, the Samsung Health Data SDK v1.1.0 package, and a small JSON data sample.

## Main areas

| Path | Purpose | Start here |
| --- | --- | --- |
| `bridge_proto/samsung-health-bridge/` | Android/Kotlin Samsung Health bridge application | `README.md`, `app/src/main/` |
| `bridge_proto/samsung-health-bridge/app/src/main/java/com/selfmanagement/samsungbridge/` | Application UI and implementation | `MainActivity.kt` |
| `.../models/` | Canonical health record and payload models | `HealthSyncPayload.kt`, `HealthRecord.kt` |
| `.../samsung/` | Samsung SDK connection, permissions, data readers, and mapping | `SamsungHealthManager.kt`, `SamsungHealthReader.kt`, `SamsungDataMapper.kt` |
| `.../sync/` | Sync orchestration and persisted sync state | `SyncManager.kt`, `SyncStateStore.kt` |
| `.../network/` | Backend client and ingestion API | `ApiClient.kt`, `HealthSyncApi.kt` |
| `.../app/src/test/` | JVM unit tests for models, mapping, and sync time ranges | Test classes under package paths |
| `inputPrototype/` | Standalone browser based input/form prototype | `index.html`, `js/`, `css/` |
| `samsung-health-data-sdk-1.1.0/` | Samsung SDK distribution, API/programming docs, sample, and viewer tool | `1.1.0/docs/` |
| `new data/` | JSON sample data | Review data contents before sharing or committing |
| `session_summary_and_guide.md` | Prior implementation notes and device usage guide | This file |
| `run.txt` | Run/log notes | This file |

## Android bridge flow

`MainActivity` → `SamsungHealthManager` and data readers → `SamsungDataMapper` → canonical models → `SyncManager` → `HealthSyncApi` / `ApiClient` → `POST /api/integrations/samsung/health-data`.

Data categories currently represented include steps, heart rate, sleep, and exercise. SDK-specific code is kept in the `samsung` package; model, sync, and network code use the normalized application records.

## Build and operation

- Android project root: `bridge_proto/samsung-health-bridge/`
- Build variant documented by the project: `developmentDebug`
- Build command (Windows): `.\gradlew.bat assembleDevelopmentDebug`
- Device use requires a physical Android device with Samsung Health, permissions, and ADB; see the bridge `README.md` and `session_summary_and_guide.md`.
- `local.properties` is machine-specific configuration. Do not copy its contents into shared docs; use `local.properties.template` as the safe reference.

## Notes

- The root `AGENTS.md` references `RTK.md`, but no `RTK.md` is present in this folder at indexing time.
- A number of Android bridge source files have existing local modifications. This index does not attempt to describe those changes as committed project state.

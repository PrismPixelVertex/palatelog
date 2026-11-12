# Changelog

All notable changes to PalateLog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.32] - 2026-11-12

### Fixed & Enhanced (Release Crash Hardening & Resource Preservation)
- **Resolved Startup Crash from Resource Shrinking (`R.string.mappls_olf_data`)**:
  - Added `app/src/main/res/raw/keep.xml` to instruct Android's resource shrinker (`isShrinkResources = true`) to preserve dynamic Mappls license and configuration strings (`mappls_olf_data`, `mappls_conf_data`), drawables, and layout templates that are resolved at runtime.
  - Added explicit ProGuard keep rules in `app/proguard-rules.pro` for `com.mappls.sdk.**` classes and interfaces.
- **Resilient Map SDK Background Failure Isolation (`PalateLogApp`)**:
  - Registered a dedicated uncaught exception safety handler in `PalateLogApp.onCreate()` to intercept asynchronous initialization failures originating from Mappls background worker threads (such as Play App Signing certificate fingerprint mismatches or network timeouts).
  - Automatically engages the graceful OpenStreetMap (OSM) fallback (`MapProviderConfig.notifyMapplsInitialized(false)` and `MapProviderConfig.notifyFallbackEngaged()`), guaranteeing that map configuration issues never crash the app process on launch.
- **Added Public GitHub Pages Privacy Policy (`docs/index.html` & `docs/privacy-policy.html`)**:
  - Authored a comprehensive, mobile-responsive Privacy Policy hosted via GitHub Pages at `https://prismpixelvertex.github.io/palatelog/`.
  - Detailed offline-first data sovereignty, ephemeral memory-only processing of location coordinates for Palate Radar geofencing, app-scoped camera and photo storage, and direct zero-cost Google Drive sync with zero developer servers.
  - Linked policy documentation directly in `README.md` and `doc/privacy_policy.md` for Google Play Closed Testing readiness.

## [1.2.31] - 2026-11-11

### Fixed & Enhanced (AGP 9 & Built-in Kotlin Modernization)
- **Migrated to AGP 9 Built-in Kotlin**:
  - Removed obsolete `org.jetbrains.kotlin.android` plugin from root `build.gradle.kts` and `app/build.gradle.kts` in favor of AGP 9's native built-in Kotlin compilation.
  - Cleaned up plugin alias definition from `gradle/libs.versions.toml`.
- **Eliminated Obsolete Variant API Warnings**:
  - Resolved `applicationVariants`, `testVariants`, and `unitTestVariants` deprecation warnings previously triggered by `kotlin-android`.
- **Cleaned Up Deprecated Compatibility Flags in `gradle.properties`**:
  - Removed deprecated opt-out flags: `android.builtInKotlin=false`, `android.newDsl=false`, `android.enableAppCompileTimeRClass=false`, `android.usesSdkInManifest.disallowed=false`, `android.sdk.defaultTargetSdkToCompileSdkIfUnset=false`, `android.defaults.buildfeatures.resvalues=true`, and `android.uniquePackageNames=false`.
  - Configured `android.dependency.useConstraints=false`, removing library constraint warnings.
  - Successfully validated release AAB generation (`bundleRelease`) and complete test suite (`test`) with zero configuration warnings.

## [1.2.30] - 2026-11-11

### Fixed & Enhanced (Family Sync Notifications & Delta Filtering)
- **Accurate Sync Delta Filtering (`SyncRepository`)**:
  - Captured `previousSyncTimestamp` prior to remote downloads and imports to accurately identify newly modified restaurants, visits, and dishes (`it.updatedAt > previousSyncTimestamp`).
  - Eliminated repeating duplicate notifications on subsequent sync passes when no new remote updates were made by family members.
  - Linked new visits and dish reviews to their respective restaurants so relevant places are accurately surfaced in notifications even when restaurant metadata itself was unchanged.
- **Semantic Verdict Indicator Formatting (`PalateNotificationManager` & `SyncRepository`)**:
  - Replaced hardcoded red pushpin (`📍`) prefix on restaurants with clean semantic verdict tagging: `📍 Restaurant Name [VERDICT]` (e.g. `📍 Jems Pizza [GO]`, `📍 Thata Coffee [NO-GO]`, `📍 California Burrito [CAUTION]`).
  - Resolved verdict text directly from centralized string resources (`R.string.verdict_go`, `R.string.verdict_no_go`, `R.string.verdict_caution`) for localization readiness and consistency.
  - Preserved color circle indicators for dish verdicts: `🟢` for GO (Must-Try), `🔴` for NO-GO (Avoid), and `🟡` for MEH/Caution.
  - Added `SyncRepository.formatSyncDigestDetails()` and unit test coverage in `PalateNotificationManagerTest` verifying verdict formatting, line capping, and deduplication.
- **Offline & Network Error Isolation (`GoogleDriveSyncClient`, `SyncRepository`, & `SettingsViewModel`)**:
  - Differentiated transient `IOException` (offline/network drops) from genuine authentication failures (`GoogleAuthException`) during OAuth token retrieval.
  - Eliminated false "Google Drive authorization expired" notification alerts when syncing while offline or in Airplane mode, displaying an in-app offline toast message instead while keeping notification shade silent.
  - Extracted offline toast messages (`R.string.settings_sync_offline` and `R.string.settings_sync_offline_retry`) into `strings.xml` for full localization.

## [1.2.29] - 2026-11-11

### Fixed & Enhanced (Android Lint & Compose Architecture Hardening)
- **Configuration-Aware String Retrieval in Compose (`AndroidLintLocalContextGetResourceValueCall`)**:
  - Replaced non-configuration-aware `LocalContext.current.getString(...)` calls across `CameraXCaptureDialog`, `LocationPickerDialog`, `EntryEditorScreen`, `MapScreen`, `QuickCaptureScreen`, and `RestaurantDetailScreen` with configuration-aware `stringResource(...)`.
  - Ensures runtime configuration changes (language/locale switches, font scaling, display updates) reactively re-render localized strings without displaying stale text.
- **Observable Compose Locales (`AndroidLintNonObservableLocale`)**:
  - Migrated `Locale.getDefault()` calls in `RestaurantDetailScreen` and `SettingsScreen` to `LocalConfiguration.current.locales[0]`, ensuring date and currency formatters observe locale changes dynamically.
- **State Autoboxing Optimization (`AndroidLintAutoboxingStateCreation`)**:
  - Refactored camera selector state in `CameraXCaptureDialog` to use `mutableIntStateOf` rather than `mutableStateOf<Int>`, eliminating primitive autoboxing and heap allocation overhead during camera preview lifecycle events.
- **Compose API Guidelines Compliance (`AndroidLintModifierParameter`)**:
  - Reordered function signatures across `DashboardScreen`, `EntryEditorScreen`, `HelpGuideScreen`, `QuickCaptureScreen`, `RestaurantDetailScreen`, and `SettingsScreen` so `modifier: Modifier = Modifier` is the first optional parameter, complying with Jetpack Compose API design conventions.
- **Android 13+ Themed Icons (`AndroidLintMonochromeLauncherIcon`)**:
  - Added `<monochrome android:drawable="@drawable/ic_launcher_foreground" />` to `ic_launcher.xml` and `ic_launcher_round.xml`, enabling Material You dynamic icon theming on Android 13+ launchers.
- **Deprecation & Code Hygiene Modernization**:
  - Replaced deprecated `Project.android(Action<BaseAppModuleExtension>)` DSL block in `app/build.gradle.kts` with `configure<com.android.build.api.dsl.ApplicationExtension> { ... }` for forward compatibility with AGP 9.0 (`android.newDsl=true`) and AGP 10.0.
  - Replaced direct context display metric calculation in `MapplsMapRenderer` with `with(LocalDensity.current) { 16.dp.roundToPx() }` (`AndroidLintLocalContextResourcesRead`), and modernized compass access to Kotlin property syntax.
  - Migrated `delay(Long)` millisecond literals in `GoogleDriveSyncClient` and `HelpGuideScreen` to type-safe `kotlin.time.Duration.Companion.milliseconds`.
  - Modernized control flow with idiomatic `when` blocks in `PalateMapModels` and `PalateNotificationManager`, and applied `.ifEmpty` / `.ifBlank` idioms across `SyncRepository`, `DashboardViewModel`, and `EntryEditorViewModel`.
  - Migrated `androidx.hilt.navigation.compose.hiltViewModel` import to `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` in `PalateLogNavGraph`.
  - Updated Room `fallbackToDestructiveMigration()` in `DatabaseModule` to `fallbackToDestructiveMigration(dropAllTables = true)`.
  - Removed redundant non-null assertions (`!!`) and always-true null checks on coordinates in `PalateRadarManager`.
  - Cleaned up unused import directives across `Converters`, `PalateRadarService`, `SyncRepository`, and `EntryEditorScreen`.

## [1.2.28] - 2026-11-10

### Fixed & Hardened (GitHub Code Scanning Security Hardening)
- **Path Traversal Protection (CWE-22 / Alert #1)**:
  - Added `SyncSecurityUtils` to sanitize and validate incoming photo file names from cross-device JSON payloads.
  - Strictly enforce image extension whitelist (`.jpg`, `.jpeg`, `.png`, `.webp`) and reject path traversal sequences (`..`, `/`, `\`, null bytes).
  - Enforced canonical path boundaries so photo file destinations can never resolve outside the app's internal `photos/` directory.
- **Resource Exhaustion Defense (CWE-400 / Alert #4)**:
  - Stream remote photo downloads via Ktor `prepareGet` and `ByteReadChannel.copyTo` directly to a temporary file, eliminating memory spikes and `OutOfMemoryError` risks.
  - Implemented strict size caps: 15 MB for photo downloads (`MAX_PHOTO_SIZE_BYTES`) and 10 MB for sync JSON payloads (`MAX_PAYLOAD_SIZE_BYTES`).
  - Added pre-flight `Content-Length` inspection, stream byte limit counting, and `Content-Type` validation to reject untrusted or oversized media.
- **Data Backup & Privacy Hardening (CWE-312 / Alert #2)**:
  - Added `disableIfNoEncryptionCapabilities="true"` to `<cloud-backup>` in `app/src/main/res/xml/data_extraction_rules.xml` to require device screen-lock / end-to-end encryption for cloud backups.
  - Excluded `palatelog_sync_prefs.xml` from both `<cloud-backup>` and `<device-transfer>` across `data_extraction_rules.xml` and `backup_rules.xml` to safeguard Google account email, target Drive folder ID, and sync state.
- **OAuth Scope Documentation & Unit Tests (CWE-250 / Alert #3)**:
  - Clarified architectural justification for `drive.readonly` in `GoogleDriveSyncClient` required for Mode B cross-account shared family sync folder discovery.
  - Added comprehensive unit tests in `SyncSecurityHardeningTest` verifying path traversal sanitization, canonical directory enclosure, and resource cap configurations.

## [1.2.27] - 2026-11-10

### Changed & Enhanced (AGP 9.5.1, Gradle 9.6.0 & Dependency Compatibility Upgrade)
- **Toolchain & Dependency Compatibility**:
  - Upgraded Android Gradle Plugin to 9.5.1 and Gradle Wrapper to 9.6.0.
  - Upgraded KSP to 2.3.6 and Dagger/Hilt to 2.60.1 (resolving KSP2 XProcessing JVM signature analyzer failure).
  - Upgraded Room to 2.8.5 with full KSP2 compatibility.
  - Upgraded AndroidX dependencies (`core-ktx` to 1.19.1, `lifecycle` to 2.11.0, `navigation-compose` to 2.10.2, `compose-bom` to 2026.09.00).
  - Upgraded third-party dependencies (`play-services-location` to 21.4.0, `kotlinx.coroutines` to 1.11.0, `kotlinx.serialization` to 1.9.0, `coil3` to 3.3.0, `ktor` to 3.3.0, `mappls-bom` to 2.0.4).
- **AGP 9 & Gradle 9.6 DSL Modernization**:
  - Migrated `tasks.registering` to `tasks.register("ensureMapplsConfigFiles")` with task description.
  - Replaced deprecated `android { kotlinOptions { jvmTarget = "17" } }` with `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`.
  - Added `testOptions.unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` and `android.onlyEnableUnitTestForTheTestedBuildType=false` in `gradle.properties` to ensure AGP 9.5+ Compose Preview runfile tasks configure successfully across all variants.
  - Enabled R8 optimized resource shrinking (`android.r8.optimizedResourceShrinking=true`) in `gradle.properties` to optimize release APK binary size and eliminate the AGP 10 deprecation warning.
- **Sync Reliability & Timeout Resilience**:
  - Increased Ktor HTTP timeouts in `NetworkModule` (`requestTimeoutMillis = 60s`, `socketTimeoutMillis = 45s`, `connectTimeoutMillis = 20s`) to provide sufficient headroom for Google Drive media uploads over cellular connections.
  - Added 1-time automatic retry with backoff for transient timeouts and socket disconnections during sync payload uploads in `GoogleDriveSyncClient`.

## [1.2.26] - 2026-11-10

### Fixed & Enhanced (Location & Offline Error Recovery UX)
- **Map View Location Off Alert & Direct Action**:
  - Added real-time detection of system location service state in `MapViewModel` via `LocationService.isLocationEnabled()`.
  - Added a top floating warning pill in `MapScreen` when location service is turned off, providing a 1-tap "Turn On" action directly launching Android's system location settings.
  - Added actionable Snackbar feedback on the "My Location" Floating Action Button in `MapScreen` prompting the user when location is disabled.
  - Added lifecycle resumption observation in `MapScreen` to automatically clear the warning and acquire user coordinates immediately when returning from system settings.
- **Quick Regret Logger Location Status Accuracy**:
  - Replaced the erroneous fallback that displayed "GPS Location Acquired" when location was off with explicit state tracking (`QuickCaptureLocationStatus`).
  - Added a dedicated warning and "Turn On" action in `QuickCaptureScreen` when location is turned off.
  - Retained coordinates in state and rendered precise coordinate readout when GPS is fixed but geocoding is unavailable.
- **Resilient Reverse Geocoding & Offline Error Recovery**:
  - Added automatic 2-attempt retry with backoff in `LocationClient.reverseGeocodeWithStatus` to gracefully handle transient network glitches and cell tower handovers during highway drives.
  - Added a non-intrusive offline indicator badge in `LocationPickerDialog` to set expectations when network connectivity is unavailable.
  - Added an actionable dual-action Snackbar (`Retry` and `Ignore`) when reverse geocoding encounters network disconnection.
  - Added an inline "Tap to fetch address" helper chip in `EntryEditorScreen` and `QuickCaptureScreen` so users can easily re-run address resolution once signal returns without needing to re-open the map dialog.

## [1.2.25] - 2026-11-10

### Fixed & Enhanced (Location Picker, Geocoding & Relational Dish Cleanup)
- **Quick Regret Logger Place Name Refresh**:
  - Fixed `QuickCaptureViewModel.onLocationPicked` to refresh `placeName` with reverse-geocoded place details upon picking a location on the map, eliminating stale stall names.
- **Location Picker Re-centering & Animation**:
  - Fixed the "My Location" Floating Action Button in `LocationPickerDialog` to fetch and animate to the user's actual GPS location rather than remaining stationary on the dropped pin.
  - Added `animationTrigger` support to `PalateInteractiveMap` to ensure camera transitions trigger reliably when coordinates update without a `PalateMapState` instance.
- **Suburban & Rural Geocoding Fallback**:
  - Enhanced reverse geocoding in `LocationClient` with fallback across `thoroughfare`, `subLocality`, `subAdminArea`, and formatted address line, ensuring address and street fields resolve accurately in suburban and highway areas.
  - Updated `EntryEditorViewModel.onLocationPicked` to update address with resolved geocode results.
- **Family Reaction Tag Display Hygiene**:
  - Updated `FamilyReactionTag` in `Components.kt` and `EntryEditorViewModel` to omit the automatic primary device name prefix when freeform member reactions are recorded, cleanly rendering user-entered text.
- **Relational Dish Deletion & Orphan Prevention**:
  - Added `softDeleteForVisit` in `DishFeedbackDao` and wired it into `RestaurantDetailViewModel.deleteVisit` so deleting a visit cascades soft-deletion to its associated dish feedback records.
  - Enhanced the Visit Deletion dialog in `RestaurantDetailScreen` to dynamically warn the user if linked dishes and/or attached photos will be removed from view, displaying exact counts of affected dishes and photos.
  - Added an individual dish deletion action and confirmation dialog in `RestaurantDetailScreen`, allowing users to delete mistakenly added dishes directly from the Dish Matrix.

## [1.2.24] - 2026-11-09

### Changed & Modernized (Platform SDK Upgrade to Android 17 / API 37)
- **Compile & Target SDK Upgrade to API 37**:
  - Upgraded `compileSdk` and `targetSdk` to level **37** (`Android 17`) in `app/build.gradle.kts`.
  - Bumped `versionCode` to `32` and `versionName` to `1.2.24` in lockstep.
  - Added `android.suppressUnsupportedCompileSdk=37.0` in `gradle.properties` to ensure smooth build reproducibility and prevent warning noise across AGP 8.13+ tooling.
- **Android Lint & Compose Configuration**:
  - Configured `LocalContextGetResourceValueCall` severity to `warning` in `app/lint.xml` to accommodate string lookups inside non-composable callback lambdas (e.g. CameraX capture listeners, Toast helpers) while keeping core lint passes clean.
- **AndroidX Dependency Maintenance**:
  - Upgraded `activityCompose` from `1.12.0` to `1.13.0` in `gradle/libs.versions.toml`.
  - Upgraded `camerax` from `1.4.1` to `1.6.2` in `gradle/libs.versions.toml`.
- **Verification & Compatibility**:
  - Verified full test suite execution (`:app:testDebugUnitTest`) with 9/9 passing tests.
  - Verified debug and release assembly (`:app:assembleDebug`, `:app:assembleRelease`) with R8 minification and resource shrinking succeeding on API 37.

## [1.2.23] - 2026-11-08

### Added & Enhanced (Phase 3B: Palate Radar & On-Demand Dynamic Geofencing)
- **On-Demand Highway Proximity Radar (`PalateRadarService`)**:
  - Implemented `PalateRadarService.kt` (`app/src/main/java/com/prismpixelvertex/palatelog/core/service/PalateRadarService.kt`) as an Android location foreground service (`FOREGROUND_SERVICE_LOCATION`) operating strictly on-demand during road trips.
  - Zero perpetual background location tracking or battery drain when inactive; automatically stops and de-registers all geofences upon session termination.
  - Displays a persistent, low-importance status notification (`channel_radar_service`) with live monitored stop counts and a 1-tap "Turn Off" action.
- **Dynamic 500 km Corridor Geofencing (`PalateRadarManager`)**:
  - Implemented `PalateRadarManager.kt` (`app/src/main/java/com/prismpixelvertex/palatelog/core/radar/PalateRadarManager.kt`) wrapping Google Play Services `GeofencingClient`.
  - Queries Room SQLite for saved highway dining spots within a 500 km corridor from current GPS coordinates, respecting the Play Services 100-geofence limit.
  - Prioritizes roadside regrets (`NO_GO` spots) first, followed by mixed/caution stops (`CAUTION`), and optional recommendations (`GO`).
  - Registers 500-meter entry geofences (`GEOFENCE_TRANSITION_ENTER`) for ample stopping notice at highway speeds.
- **Proximity Heads-Up Warnings (`PalateRadarReceiver` & `PalateNotificationManager`)**:
  - Implemented `PalateRadarReceiver.kt` (`app/src/main/java/com/prismpixelvertex/palatelog/core/radar/PalateRadarReceiver.kt`) to catch geofence transitions.
  - Formats concise, actionable warning copy (e.g., *"🔴 Roadside Regret Ahead (500m): Sri Krishna Dhaba — Avoid: Watery tea, dirty restrooms"*).
  - Consolidated proximity alerts to a single dedicated notification slot (`NOTIFICATION_ID_RADAR_ALERT`), preventing notification drawer flooding while maintaining `INITIAL_TRIGGER_ENTER` so drivers restarted at fuel bunks are immediately alerted to adjacent regrets.
  - Batch prioritization: surfaces highest-priority regret first with multi-spot indicators (`[+N other spots nearby]`).
  - Added high-importance notification channel `channel_radar_alerts` with sound/vibration; tapping the notification deep-links directly to `RestaurantDetailScreen`.
  - Clean dismissal: stopping Palate Radar immediately clears all active proximity notifications along with the ongoing service notification (`clearAllRadarNotifications()`).
- **System-Wide Quick Settings Tile (`PalateRadarTileService`)**:
  - Added `PalateRadarTileService.kt` (`app/src/main/java/com/prismpixelvertex/palatelog/core/service/PalateRadarTileService.kt`) allowing drivers to toggle Palate Radar on/off with 1 tap from the Android Quick Settings shade (`STATE_ACTIVE` / `STATE_INACTIVE`).
- **Collapsible Explore Banner & Interactive Help Guide**:
  - Implemented Option A slim collapsible banner (`PalateRadarCard`) in `DashboardScreen.kt`, positioned directly below the search bar and above the filter chips bar, ensuring filter chips remain closely paired with the restaurant card feed. Vertical space consumption reduced by ~80% with auto-collapse on scroll and expandable details.
  - Added prominent disclosure dialog with clear guidance on opting into favorite (`GO`) stops in Settings.
  - Added dedicated Palate Radar guide card in `HelpGuideScreen.kt` with deep-linking shortcut (`HelpTopicKey.RADAR`).
  - Added Palate Radar settings in `SettingsScreen.kt` including an opt-in toggle to alert for recommended (`GO`) stops and a privacy disclosure review/reset action.
- **Google Play Compliance Documentation**:
  - Created `doc/play_store_location_policy_justification.md` containing complete ready-to-use declaration answers, core feature justifications, prominent disclosure text, and reviewer video walkthrough script.

## [1.2.22] - 2026-11-07

### Added & Enhanced (Phase 3A: Smart Notifications & Sync Health Alerts)
- **Centralized Notification Architecture (`PalateNotificationManager`)**:
  - Implemented [`PalateNotificationManager.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/notification/PalateNotificationManager.kt) to manage Android notification channels and dispatch typed, non-intrusive notifications.
  - Initialized notification channels (`channel_sync_health`, `channel_family_sync`) in [`PalateLogApp.kt`](app/src/main/java/com/prismpixelvertex/palatelog/PalateLogApp.kt) `onCreate()`.
  - Added dedicated monochrome notification icon vector drawable `ic_notification_restaurant.xml`.
- **Actionable Sync Health Alerts**:
  - Wired into [`SyncRepository.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/sync/SyncRepository.kt) to alert users strictly when actionable human intervention is required (e.g. Google Drive OAuth expiration or revoked family sync folder permissions `HTTP 401/403`).
  - Standard offline states and transient network dropouts remain completely silent, maintaining strict zero-noise discipline.
  - Automatically clears the health notification via `clearSyncAuthAlert()` as soon as sync succeeds.
- **Family Sync Delta Digest**:
  - Aggregates incoming remote JSON deltas from other family devices in [`SyncRepository.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/sync/SyncRepository.kt).
  - Emits a low-priority, batched summary notification (e.g., *"Priya added 2 new dining reviews"*) with expandable restaurant & dish highlights.
- **Deep-Link Intent Routing & Android 13+ Runtime Permissions**:
  - Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` in `AndroidManifest.xml`.
  - Added runtime notification permission check alongside location permissions in [`MainActivity.kt`](app/src/main/java/com/prismpixelvertex/palatelog/MainActivity.kt) on Android 13+ (API 33+).
  - Tapping the sync health notification deep-links directly to Settings, while tapping the family digest navigates to Explore (`PalateLogNavGraph.kt`).

## [1.2.21] - 2026-11-02

### Improved & Optimized (Explore Screen Location Caching & Distance Sorting)
- **Time-Based Location Cache Expiry (5-Minute Window)**:
  - Added timestamp-backed location caching (`LOCATION_CACHE_EXPIRY_MS = 5 mins`) in [`DashboardViewModel.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/dashboard/DashboardViewModel.kt). Switching tabs back to the Explore screen reuses cached location coordinates when under 5 minutes old, eliminating redundant location polling and list jumps.
- **Fast Raw Coordinate Fetch (Reverse Geocode Bypass)**:
  - Added `skipGeocoding: Boolean = false` parameter to [`LocationService.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/location/LocationService.kt) and [`LocationClient.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/location/LocationClient.kt).
  - Explore screen distance sorting now requests raw `(latitude, longitude)` directly (`skipGeocoding = true`), bypassing the 500ms `Geocoder.getFromLocation()` network lookup.
- **50-Meter Distance Shift Threshold**:
  - Distance sorting and UI state recalculations are only re-triggered if the user moves by 50 meters or more (`SIGNIFICANT_DISTANCE_SHIFT_KM = 0.05`), preventing CPU re-evaluations during minor GPS drift.
- **Silent Background Refresh & Flashing Elimination**:
  - Re-fetching location on tab resume now performs a silent background refresh without setting `_isLocationLoading.value = true` when valid location state already exists, eliminating UI spinner flickering.

## [1.2.20] - 2026-11-02

### Improved & Fixed (Camera Selection & In-App Capture)
- **In-App Camera Capture via CameraX (`CameraXCaptureDialog`)**:
  - Integrated CameraX dependencies (`androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` version `1.4.1`) in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
  - Added [`CameraXCaptureDialog.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/CameraXCaptureDialog.kt) providing a fast, 1-tap in-app camera viewfinder dialog configured with `CameraSelector.LENS_FACING_BACK`.
  - Guaranteed 100% reliable rear (back) camera selection across all device OEMs when taking food, menu, or bill photos in [`EntryEditorScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/entry_editor/EntryEditorScreen.kt).
  - Added runtime camera permission handling (`android.permission.CAMERA`) with smooth fallback.

## [1.2.19] - 2026-11-02

### Changed & Upgraded (Dependency Catalog & Toolchain Optimization)
- **Dependency Catalog Modernization (`gradle/libs.versions.toml`)**:
  - Upgraded Jetpack Compose BoM from `2024.10.01` to `2025.12.00` incorporating over 14 months of Material 3 and Compose runtime optimizations.
  - Upgraded `navigation-compose` from `2.8.3` to `2.9.7` for modern predictive back gestures and type-safe routing.
  - Upgraded `lifecycle-runtime-ktx` and `lifecycle-viewmodel-compose` from `2.8.7` to `2.10.0`.
  - Upgraded `hilt` from `2.52` to `2.57` (highest supported release under AGP 8).
  - Upgraded `hilt-navigation-compose` and `hilt-work` from `1.2.0` to `1.3.0`.
  - Upgraded `core-ktx` from `1.15.0` to `1.18.0` and `activity-compose` from `1.9.3` to `1.12.0` (highest releases targeting SDK 36).
  - Upgraded `work-runtime-ktx` from `2.10.0` to `2.12.0` for background sync workers.
  - Upgraded `coil` image loader from `3.1.0` to `3.2.0`.
  - Upgraded `play-services-auth` from `21.3.0` to `22.0.0` and `credentials` from `1.3.0` to `1.6.0`.
  - Upgraded `osmdroid-android` from `6.1.18` to `6.1.20`.
  - Upgraded `mappls-bom` from `2.0.0` to `2.0.3` (resolving `mappls-android-sdk:9.0.3` and `mappls-services:1.0.1`).
  - Upgraded `kotlinx-serialization-json` from `1.7.3` to `1.8.0` and `kotlinx-coroutines-android` from `1.9.0` to `1.10.1`.
  - Upgraded testing dependencies (`androidx.test.ext:junit` `1.2.1` -> `1.3.0`, `espresso-core` `3.6.1` -> `3.7.0`).
- **Dependency Strategy & Compatibility Audit Documentation**:
  - Added comprehensive technical audit and risk analysis record in [`doc/dependency_upgrade_strategy_and_audit.md`](doc/dependency_upgrade_strategy_and_audit.md) detailing upstream SDK 37, Kotlin metadata, and AGP 9 architecture boundaries.

## [1.2.18] - 2026-11-02

### Improved & Fixed (Explore Screen Scroll Stability & Location Fallbacks)
- **Explore Screen Scroll Reset & Top Position Guarantee**:
  - Fixed an issue where Compose `LazyColumn` key preservation (`key = { it.restaurant.id }`) caused the list to scroll to the last card when location resolved and re-sorted the list by distance.
  - Ensured `listState.scrollToItem(0)` unconditionally resets the scroll position to index `0` (top closest card) upon location resolution, filter change, or search query change in [`DashboardScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/dashboard/DashboardScreen.kt).
- **Graceful "Near Me" Location Fallback**:
  - Updated [`DashboardViewModel.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/dashboard/DashboardViewModel.kt) so that when location is unavailable (or no spots are within 15km), selecting the "Near Me" filter chip gracefully falls back to showing all cards by recent visits rather than an empty list.
  - Added explicit status notes ("📍 Location unavailable, showing all cards by recent visits" or "📍 No spots within 15km, showing all cards by recent visits") in [`DashboardScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/dashboard/DashboardScreen.kt) to keep the user clearly informed.
- **Stabilized Location Fetching & Instant Cached Fixes**:
  - Enhanced [`LocationClient.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/location/LocationClient.kt) to query `lastLocation` first for instant (~10ms) location resolution, with non-blocking reverse geocoding.
  - Added location loading state (`_isLocationLoading`) in [`DashboardViewModel.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/dashboard/DashboardViewModel.kt) to prevent sudden re-sorting layout jumps.

## [1.2.17] - 2026-11-02

### Fixed & Tested
- **In-App Update Dialog Scoping & Mutual Exclusion**:
  - Restricted the modal update restart `AlertDialog` to `BuildConfig.DEBUG` in [`MainActivity.kt`](app/src/main/java/com/prismpixelvertex/palatelog/MainActivity.kt) so it is used exclusively for debug testing and ADB broadcast simulation (`TEST_UPDATE`).
  - Enforced mutual exclusion between the modal `AlertDialog` and the persistent bottom banner card (`AnimatedVisibility` condition: `visible = isUpdateDownloaded && (!BuildConfig.DEBUG || !showRestartDialog)`), preventing both update restart prompts from ever displaying simultaneously on screen.
  - Documented architectural decisions in [ADR 010 in `decisions_and_tradeoffs.md`](doc/decisions_and_tradeoffs.md#L188-L200).
- **ADB Update Simulation Testing & Verification**:
  - Tested simulated update flows on a physical ADB connected Android test device.
  - Verified `adb shell am broadcast -a com.prismpixelvertex.palatelog.TEST_UPDATE --es state DOWNLOADED`: modal `AlertDialog` pops up cleanly with the bottom banner card hidden.
  - Verified postponed state: dismissing the dialog via **"Later"** (`showRestartDialog = false`) seamlessly reveals the persistent bottom banner card above navigation bars.
  - Verified `adb shell am broadcast -a com.prismpixelvertex.palatelog.TEST_UPDATE --es state RESET`: resets `isUpdateDownloaded` state and dismisses all update UI elements cleanly.

## [1.2.16] - 2026-11-01

### Added & Refactored (Plug-and-Play Indian-Compliant Map Architecture)
- **Plug-and-Play Map Architecture (Strategy & Facade Pattern)**:
  - Defined `MapProviderType` (`MAPPLS`, `OSM_DROID`, `GOOGLE_MAPS`, `OLA_MAPS`) and `MapProviderConfig` in [`PalateMapModels.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt) for runtime map vendor switching and automated fallback.
  - Provided metadata contracts (`nameResId`, `attributionResId`, `isGovernmentCompliant`) to support dynamic attribution and regulatory compliance badges across all map vendors.
  - Implemented [`MapplsMapRenderer.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/MapplsMapRenderer.kt) with Mappls Android SDK v2.0+ (BoM 2.0.0) integrating cryptographically signed configuration files (`.a.conf` / `.a.olf`).
  - Extracted [`OsmDroidMapRenderer.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/OsmDroidMapRenderer.kt) as a dedicated offline-capable fallback renderer.
  - Updated universal composable facade [`PalateInteractiveMap.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateInteractiveMap.kt) to dispatch between Mappls and OSMDroid with seamless runtime error recovery.
  - Initialized Mappls SDK safely in [`PalateLogApp.kt`](app/src/main/java/com/prismpixelvertex/palatelog/PalateLogApp.kt) with runtime initialization error guards.
- **Indian Statutory Geospatial Compliance (Survey of India) & Transparent Notices**:
  - Adhered strictly to the Criminal Law Amendment Act, 1961, National Geospatial Policy (2022), and Survey of India external boundary guidelines for Jammu & Kashmir, Ladakh, and Arunachal Pradesh.
  - Added dynamic map provider disclosure and attribution to the About section in [`SettingsScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/settings/SettingsScreen.kt): cleanly displays the primary compliant provider when active, or displays primary provider alongside an explicit fallback notice and disclaimer when connectivity/service limits require falling back to OSM.
- **Presentation Layer Clean Architecture Decoupling**:
  - Migrated [`MapScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/map/MapScreen.kt), [`LocationPickerDialog.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/LocationPickerDialog.kt), and [`RestaurantDetailScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/restaurant_detail/RestaurantDetailScreen.kt) to consume `PalateInteractiveMap`, completely eliminating vendor-specific `org.osmdroid.views.MapView` instantiations from feature screens.
- **Security, CI & Build Configuration**:
  - Added Mappls Gradle Services plugin and Maven repository configuration in `settings.gradle.kts` and root `build.gradle.kts`.
  - Added `*.a.conf`, `*.a.olf`, and `mappls-conf.txt` to `.gitignore` to prevent credential exposure in version control.
  - Implemented automated placeholder generation task `ensureMapplsConfigFiles` in [`app/build.gradle.kts`](app/build.gradle.kts) and updated [`.github/workflows/codeql.yml`](.github/workflows/codeql.yml) to ensure CI analysis and open-source checkouts compile without requiring private developer portal credentials.
  - Enforced strict serial execution order in [`app/build.gradle.kts`](app/build.gradle.kts) (`merge<Variant>Assets` depends on `process<Variant>MapplsServices`) and disabled UP-TO-DATE caching on `MapplsServicesTask` to guarantee the correct credentials match the active keystore variant without parallel execution race conditions or duplicate asset collisions.
  - Added structured diagnostic logging with tags (`PalateLog_App`, `PalateLog_Mappls`, `PalateLog_Map`) in [`PalateLogApp.kt`](app/src/main/java/com/prismpixelvertex/palatelog/PalateLogApp.kt), [`MapplsMapRenderer.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/MapplsMapRenderer.kt), and [`PalateMapModels.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt).
- **UI & UX Refinements**:
  - Applied `statusBarsPadding()` in [`MapScreen.kt`](app/src/main/java/com/prismpixelvertex/palatelog/feature/map/MapScreen.kt) to keep the map view and controls neatly below the top system bar.
  - Repositioned Mappls map compass to Top-Start with 16dp margins in [`MapplsMapRenderer.kt`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/MapplsMapRenderer.kt) to eliminate overlap with the "My Location" FAB.
  - Added 3-second smooth cinematic flight recentering animation across map renderers via `DEFAULT_MAP_CAMERA_ANIMATION_MS`.
  - Documented architectural decisions and legal context in [ADR 002 in `decisions_and_tradeoffs.md`](doc/decisions_and_tradeoffs.md#L30-L55).

## [1.2.15] - 2026-11-01

### Added & Refactored (KMP-Ready Architecture Foundation)
- **Multiplatform Image Loading Migration (Coil 2 → Coil 3)**:
  - Upgraded image loading dependency from Android-only `io.coil-kt:coil-compose:2.7.0` to Kotlin Multiplatform `io.coil-kt.coil3:coil-compose:3.1.0` and `coil-network-okhttp:3.1.0`.
  - Updated Compose `AsyncImage` imports in [`EntryEditorScreen`](app/src/main/java/com/prismpixelvertex/palatelog/feature/entry_editor/EntryEditorScreen.kt) and [`RestaurantDetailScreen`](app/src/main/java/com/prismpixelvertex/palatelog/feature/restaurant_detail/RestaurantDetailScreen.kt) with 100% backward compatibility on Android.
- **Hardware & System Service Interface Abstractions (Interface Segregation)**:
  - **Location Abstraction**: Extracted pure Kotlin [`LocationService`](app/src/main/java/com/prismpixelvertex/palatelog/core/location/LocationService.kt) domain interface; refactored `LocationClient` to implement it, bound via Hilt in [`LocationModule`](app/src/main/java/com/prismpixelvertex/palatelog/di/LocationModule.kt), and updated all 5 consuming ViewModels (`DashboardViewModel`, `EntryEditorViewModel`, `MapViewModel`, `QuickCaptureViewModel`, `RestaurantDetailViewModel`) to depend solely on the domain interface.
  - **Navigation Abstraction**: Introduced [`MapNavigator`](app/src/main/java/com/prismpixelvertex/palatelog/core/navigation/MapNavigator.kt) and [`AndroidMapNavigator`](app/src/main/java/com/prismpixelvertex/palatelog/core/navigation/AndroidMapNavigator.kt), decoupling platform-specific intent handling from caller features.
  - **Photo Storage Abstraction**: Introduced [`PhotoStorageManager`](app/src/main/java/com/prismpixelvertex/palatelog/core/util/PhotoStorageManager.kt) and [`AndroidPhotoStorageManager`](app/src/main/java/com/prismpixelvertex/palatelog/core/util/AndroidPhotoStorageManager.kt) to decouple Android `FileProvider` internals from feature presentation.
- **Multiplatform HTTP Client Migration (OkHttp → Ktor Client)**:
  - Added Ktor 3.1.1 multiplatform client dependencies (`ktor-client-core`, `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`).
  - Refactored [`GoogleDriveSyncClient`](app/src/main/java/com/prismpixelvertex/palatelog/core/sync/GoogleDriveSyncClient.kt) from raw `OkHttpClient` calls to Ktor `HttpClient`, maintaining exact Google Drive v3 REST API compatibility for Mode A & Mode B sync, folder discovery/creation, multipart sync JSON and photo uploads, and permissions management.
  - Created [`NetworkModule`](app/src/main/java/com/prismpixelvertex/palatelog/di/NetworkModule.kt) to provide a centralized singleton `HttpClient`.
- **Map View Facade**:
  - Implemented [`PalateInteractiveMap`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateInteractiveMap.kt) composable facade and [`PalateMapMarker`](app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt) to isolate underlying OSMDroid Android views from future iOS MapKit rendering.

## [1.2.14] - 2026-10-31

### Added
- **Floating Scroll Indicators across Explore & Restaurant Detail Screens**:
  - **Explore & Restaurant Detail Scroll Indicators**: Introduced a semi-transparent circular down-arrow indicator (`Icons.Default.KeyboardArrowDown`) floating at the bottom center of the main Explore restaurant list and the Restaurant Detail screen.
  - **Auto-Hide & Scroll Re-Trigger**: Automatically fades out after 2.5 seconds of inactivity to keep card details and visit entries completely unblocked, and reappears whenever the user scrolls or new content loads.
  - **Smart Content-Awareness**: Guarantees that the indicator is only shown when there is more content extending beyond the bottom of the visible screen (`canScrollForward == true`). Hides automatically when the user reaches the bottom or if the content fits entirely on screen.
  - **1-Tap Scroll Down**: Tapping the indicator smoothly scrolls down to reveal more items or visit history cards.

### Fixed
- **Restaurant Detail Screen System Navigation Bar Padding**:
  - **Safe Insets & Clear Space**: Applied `Scaffold`'s `innerPadding` directly to the root `Box` container on `RestaurantDetailScreen` so the floating scroll indicator floats cleanly above the Android system 3-button navigation bar.
  - **Bottom Content Clearance**: Added `bottom = 80.dp` internal scroll padding to the main detail column so that visit cards and action buttons are never obscured by system navigation bars or floating controls when scrolled to the end.

## [1.2.13] - 2026-10-31

### Fixed
- **Dark Theme Help Screen Tip Box Contrast**:
  - **High-Contrast Callout Styling**: Updated text and icon colors in `HelpGuideScreen` tip boxes (`help_quick_log_reuse_tip` and `help_sync_whatsapp_tip`) to use dedicated dark contrast text colors (`VerdictGoGreenText` and `VerdictCautionAmberText`) matching light green/amber container backgrounds.
  - **Dark Mode Mode B Badge Legibility**: Enhanced Mode B sync badge text color (`0xFF8B5CF6`) for high legibility across both dark and light surfaces.
- **Map Screen Preview Card Button Layout & Text Wrapping**:
  - **Single-Line Button Layout**: Adjusted `contentPadding` on map preview action buttons (`New Visit`, `Navigate`, `View Details`) to `PaddingValues(horizontal = 6.dp, vertical = 8.dp)` and added `maxLines = 1` with `TextOverflow.Ellipsis`.
  - **Eliminated Awkward Text Breaks**: Reclaimed 36dp internal horizontal width per button, preventing "Navigate" from breaking across lines ("Navig ate") on narrow phone displays.

## [1.2.12] - 2026-10-31

### Refactored & Localization
- **Complete Hardcoded UI String Extraction & Localization Support**:
  - **Comprehensive Audit**: Conducted a project-wide search for hardcoded user-facing strings across all Compose screens (`SettingsScreen`, `QuickCaptureScreen`, `EntryEditorScreen`, `RestaurantDetailScreen`, `DashboardScreen`, `MapScreen`, `LocationPickerDialog`).
  - **Centralized String Resources**: Extracted over 80+ hardcoded UI strings, dialog titles, placeholders, toast messages, and dynamic format strings into `res/values/strings.xml` to support full app internationalization/localization (i18n).
  - **Settings & Sync Refactoring**: Converted Google Drive connection statuses (`Google Drive Connected`, `Disconnect`, `Family Sync Setup Mode`, `Shared Account`, `Individual Accounts`), member profile strings, backup import/export labels, and permission pending WhatsApp prompt texts to `stringResource` / `context.getString`.
  - **Quick Capture & Visit Log Refactoring**: Refactored verdict sublabels (`Never Again!`, `Must Visit!`, `Mixed / Okay`), quick reason chip default suggestions, highway corridor placeholders, remembered dish headers, and dish editor cards to use localized string resources.
  - **Map & Restaurant Details Refactoring**: Replaced map marker titles, location coordinate readouts, delete confirmation dialog copy, and visit channel section headers with string resources.
  - **Button Icon & Copy Cleanup**: Updated `detail_action_new_visit` string from `"+ New Visit"` to `"New Visit"`, removing duplicate plus symbol in action buttons that already render `Icons.Default.Add`.

## [1.2.11] - 2026-10-30

### Added
- **Help & User Guide Screen with Contextual Deep-Linking**:
  - **Dedicated Guide Destination (`HelpGuideScreen`)**: Introduced an interactive Material 3 guidebook (`Screen.HelpGuide`) organizing PalateLog's primary paradigms into expandable visual cards with category tags, comparative layouts, and embedded live badge previews (`VerdictBadge`, `DishVerdictBadge`, `ChannelBadge`).
  - **Google Drive Family Sync Demystified**: Clear side-by-side walkthrough of **Mode A** (Single Shared Family Account: zero-config, 1-tap setup) versus **Mode B** (Individual Accounts: host provisioning, automated permissions API, static QR code, and WhatsApp recovery).
  - **3-Second Quick Regret Log vs. Full Visit Log**: Explains when to use speed capture (highway roadside tea stalls, dirty restrooms, immediate regret) versus full dining/delivery logging (channels, bills, receipt photos).
  - **The Dish Matrix & Philosophy**: Highlights why high-verdict restaurants can have avoid dishes (e.g., soggy delivery sides) and how family member preferences and remembered dish chips prevent repeat mistakes.
  - **Interactive Jump Chips**: Horizontal filter chips (`All`, `☁️ Family Sync`, `⚡ Quick Log`, `📝 Full Visit`, `🍽️ Dish Matrix`, `🗺️ Map & Travel`, `🔒 Privacy & Offline`) at the top of the guide for instant auto-scrolling and card expansion.
  - **Contextual `?` Shortcuts**: Embedded help icon buttons across the app linking directly to the relevant card with auto-scroll and highlight:
    - Settings: Next to *Google Shared Drive Sync* (`?topic=sync`) and a dedicated *Help & User Guide* card row.
    - Dashboard: In the TopAppBar for general guide access.
    - Quick Log: In the TopAppBar (`?topic=quick_log`).
    - Restaurant Details: Beside the *DISH INSIGHTS & MEMORY MATRIX* header (`?topic=dish_matrix`).
    - Entry Editor: Beside the *Dishes Breakdown* header (`?topic=dish_matrix`) and in the TopAppBar (`?topic=visit_log`).
  - **Design System & Localization Refactoring**: Centralized all guide copy and `RestaurantDetailScreen` section headers/dialogs into `res/values/strings.xml` for internationalization/localization (i18n), and standardized all layout dimensions, borders, and icon containers in `Dimens.kt` (`HelpIconContainerSize`, `MapPinIndicatorSize`, `BorderThin`, `BorderThick`).
  - **Type-Safe Topic Navigation (`HelpTopicKey`)**: Refactored `onNavigateToHelpGuide` to accept the `HelpTopicKey` enum rather than raw string literals (`dish_matrix`, `quick_log`, `visit_log`, `sync`), providing compile-time type safety across all navigation triggers.
  - **Scroll Affordance & Inset Optimization**: Eliminated duplicate bottom window insets between the root navigation Scaffold and child screens to reclaim unused dead space, and introduced an animated floating "More topics below ↓" pill that allows 1-tap downward scrolling and auto-hides when users reach the bottom of the list.
  - **First-Launch Onboarding Welcome Dialog**: Added a one-time welcome dialog on the Explore screen for newly installed devices, offering users a quick guided tour to the Help & User Guide or the option to explore later via the Settings menu or top-bar '?' icon.

## [1.2.10] - 2026-10-30

### Fixed & Enhanced
- **In-App Update Background Download Completion & Restart Prompt**:
  - **Prioritized `InstallStatus.DOWNLOADED` Check**: Reordered update status evaluation in `InAppUpdateHelper` to check `installStatus() == DOWNLOADED` before `updateAvailability() == UPDATE_AVAILABLE`, ensuring downloaded flexible updates are detected and not blocked by the available update check.
  - **Safe `onResume()` Lifecycle Query (`checkStatusOnResume`)**: Restored `onResume()` in `MainActivity` with a dedicated, loop-safe update check that queries only whether an update has completed downloading or whether an interrupted immediate update needs resuming. It never initiates new flexible update flows, eliminating recursive prompt loops while guaranteeing foreground returns detect completed downloads.
  - **Explicit Restart Modal Dialog (`AlertDialog`)**: Added a clear Material 3 `AlertDialog` ("PalateLog Update Ready! 🟢") upon download completion, prompting users to restart now to apply the update or postpone for later.
  - **Collision-Free Bottom Banner Card Layout**: Adjusted bottom banner padding (`padding(bottom = 96.dp)`) so that when the dialog is postponed, the persistent reminder card sits cleanly above the Material 3 `NavigationBar` without overlapping bottom navigation tabs.
  - **Diagnostic Logging**: Added structured logging for update installation states (`DOWNLOADING`, `DOWNLOADED`, `FAILED`, `CANCELED`) in `InAppUpdateHelper` for runtime visibility.

## [1.2.9] - 2026-10-29

### Fixed & Optimized
- **Static Analysis & Lint Cleanup (ADR 008)**:
  - Corrected WorkManager on-demand initialization in `AndroidManifest.xml` with `android:value="androidx.startup"` on the removal tag, resolving the `RemoveWorkManagerInitializer` lint error.
  - Eliminated Compose primitive boxing in `RestaurantDetailScreen.kt` by switching coordinate state to `mutableDoubleStateOf`.
  - Added explicit `Locale.getDefault()` to `GeoUtils.kt` distance formatting to prevent implicit locale conversion issues.
  - Modernized `Channel.values()` to Kotlin 2.0 `Channel.entries` in `VisitEntry.kt` for allocation-free enumeration.
  - Migrated URL parsing to KTX `.toUri()` in `ExternalMapNavigator.kt` and `SettingsScreen.kt`.
  - Migrated `SharedPreferences` write calls to KTX `prefs.edit { ... }` in `SyncRepository.kt` and `EntryEditorViewModel.kt`.
  - Migrated marker rendering in `MapMarkerFactory.kt` to Android KTX (`.toColorInt()`, `createBitmap()`, `.toDrawable()`).
  - Removed unused XML namespace declaration from `file_paths.xml`.
  - Quoted Mermaid diagram node strings containing emojis and quotes in `architecture_and_design.md` to resolve parser syntax errors.
  - Removed dead imports across `DashboardScreen.kt`, `DashboardViewModel.kt`, `MapScreen.kt`, `MapViewModel.kt`, `RestaurantDetailScreen.kt`, and `SettingsScreen.kt`.
  - Cleaned redundant properties in `.editorconfig`.

### Added & Documented
- **Lint Configuration & Intentional Suppressions (`app/lint.xml`)**:
  - Added `app/lint.xml` to globally ignore `UnusedResources` pending the planned full-app UI localization session.
  - Annotated intentional product design constraints: `UnnecessaryRequiredFeature` on touchscreen requirement in manifest, `@SuppressLint("HardwareIds")` on stable device ID in `SyncRepository.kt`, and `@Suppress("SameParameterValue")` in `SettingsViewModel.kt`.
- **Architecture Decision Record (ADR 008)**:
  - Added `ADR 008: Static Analysis & IDE Lint Observation Triage Strategy` to `doc/decisions_and_tradeoffs.md`.
- **Roadmap Tracked Task**:
  - Added comprehensive Compose UI localization and hardcoded string migration TODO to `doc/requirements_traceability_and_roadmap.md`.

## [1.2.8] - 2026-10-21

### Changed & Fixed
- **Touchscreen Hardware Filtering (`AndroidManifest.xml`)**:
  - Added `<uses-feature android:name="android.hardware.touchscreen" android:required="true" />` to manifest so Google Play filters out non-touchscreen Android TV set-top boxes and streaming devices.
- **Gradle Build Portability (`gradle.properties`)**:
  - Removed local machine hardcoded `org.gradle.java.home` path from `gradle.properties` so builds are portable across developer machines, operating systems, and CI/CD pipelines.
- **GitHub Documentation Mermaid Diagram Syntax (`architecture_and_design.md`)**:
  - Fixed a Mermaid flowchart rendering error in `doc/architecture_and_design.md` by escaping node label parentheses in double quotes (`"Tap 'Save & Finish (3s)'"`).
- **Public Repository Readiness & Git Author History**:
  - Unified repository Git commit history to use GitHub no-reply email (`5286594+karthikeyan-s@users.noreply.github.com`).
- **Repository Code Ownership (`.github/CODEOWNERS`)**:
  - Added `.github/CODEOWNERS` defining `@karthikeyan-s` as default code owner for repository governance.

## [1.2.7] - 2026-10-21

### Added & Refactored
- **Design System Dimensions & Spacing Tokens (`Dimens.kt`)**:
  - Created `core/designsystem/Dimens.kt` establishing central design system tokens for Spacing (`SpacingSmall = 8.dp`, `SpacingDefault = 16.dp`), Corner Radii (`RadiusMedium = 12.dp`, `RadiusLarge = 16.dp`, `RadiusDialog = 20.dp`), Icon Sizing (`IconSizeSmall = 16.dp`, `IconSizeDefault = 24.dp`, `IconSizeHero = 64.dp`), and Component Heights (`ButtonHeightStandard = 56.dp`).
  - Standardized component padding and dimensions across `Components`, `LocationPickerDialog`, `DashboardScreen`, `QuickCaptureScreen`, `EntryEditorScreen`, `RestaurantDetailScreen`, `MapScreen`, and `SettingsScreen`.
- **Strict Non-Transitive R Classes (`android.nonTransitiveRClass`)**:
  - Added `android.nonTransitiveRClass=true` to `gradle.properties`, enforcing strict submodule resource scoping and reducing compiled R class bytecode overhead.
- **UI String Resource Extraction & Localization Preparedness (`strings.xml`)**:
  - Extracted hardcoded UI string literals across all Jetpack Compose screens, navigation components, dialogs, badges, and design system elements into `app/src/main/res/values/strings.xml`.
  - Refactored `PalateLogNavGraph`, `Components`, `LocationPickerDialog`, `DashboardScreen`, `QuickCaptureScreen`, `EntryEditorScreen`, `RestaurantDetailScreen`, `MapScreen`, and `SettingsScreen` to use `@StringRes` and `stringResource(...)`.
  - Prepared codebase for internationalization (i18n) and localized screen readers (TalkBack) while resolving Android Lint `HardcodedText` warnings.

## [1.2.6] - 2026-10-21

### Added
- **Dish-Aware Unified Search & Tokenized Relevance Ranking (Phase 2A)**:
  - Added `getAllDishes()` in `DishFeedbackDao` to stream active dish feedback entries across all restaurants.
  - Implemented tokenized query processing in `DashboardViewModel` matching queries across restaurant metadata and specific dishes.
  - Ranked search results by relevance (exact phrase match > all-token match > partial-token match) so searching e.g. `"Chicken Fry"` lists exact matches at top and partial matches (like `"Chicken 65"`) down the order.
  - Displayed matched dish chips with verdict badges directly on `RestaurantCard` on Dashboard.
- **Camera & System PhotoPicker Integration (Phase 2A)**:
  - Added 1-tap **Gallery 🖼️** (`PickVisualMedia`) and **Camera 📷** (`TakePicture` via `FileProvider`) attachments in `EntryEditorScreen` (bill receipt and per-dish photos) and `QuickCaptureScreen`.
  - Stored persistent copies of media in internal app storage via `MediaUtils.copyUriToInternalStorage()` for zero-permission offline rendering.
- **Restaurant Details Photo Gallery & Fullscreen Preview (Phase 2A)**:
  - Displayed dish photo thumbnails in the Dish Insights Matrix and bill receipt photo thumbnails in `VisitHistoryCard` via Coil `AsyncImage`.
  - Implemented interactive tap-to-expand `FullscreenPhotoDialog` for full-screen photo viewing.
- **Two-Way Family Photo Sync via Google Drive**:
  - Implemented `PalateLog_Family/Photos/` subfolder management in `GoogleDriveSyncClient`.
  - Added photo filename mapping in `SyncVisit` (`billReceiptPhotoName` / `photoNames`) and `SyncDish` (`photoNames`) using unique UUID filenames.
  - Automatically uploads local photo files and downloads remote family photo files during Google Drive 2-way sync for offline rendering on all family devices.
- **Multiple Visit Photo Attachments**:
  - Upgraded visit-level photo attachments in `EntryEditorScreen` and `QuickCaptureScreen` to support attaching multiple photos (receipts, menu cards, ambience, etc.).
  - Added `VisitEntry.photoUris` helper and rendered horizontal thumbnail gallery for all visit photos on `VisitHistoryCard` in `RestaurantDetailScreen`.

### Fixed & Cleaned
- **Camera Permission & Gallery Folder Access**:
  - Removed `android.permission.CAMERA` declaration from `AndroidManifest.xml` so `ActivityResultContracts.TakePicture()` delegates camera captures directly to system camera app without requiring runtime permissions.
  - Replaced `PickVisualMedia` with `ActivityResultContracts.GetContent("image/*")` allowing browsing all phone folders (WhatsApp Images, Pictures, Downloads, SD Card, etc.) in system picker.
- **In-App Update Activity Result API Migration**:
  - Migrated `InAppUpdateHelper` and `MainActivity` from deprecated `onActivityResult` callback to `ActivityResultContracts.StartIntentSenderForResult()`, eliminating deprecation build warnings and improving type safety.
- **Code Hygiene & Kotlin Modernization**:
  - Updated `DishVerdict.values()` to `DishVerdict.entries` in `EntryEditorScreen`.
  - Added explicit `Locale.getDefault()` to `String.format` bill total formatting in `RestaurantDetailScreen`.
  - Migrated `SharedPreferences` writes to Kotlin KTX `prefs.edit { ... }` blocks in `InAppUpdateHelper`, `SettingsViewModel`, and `SyncRepository`.
  - Removed unused imports and unneeded exception parameters across location, dashboard, and detail modules.

## [1.2.5] - 2026-10-21

### Added
- **Dual-Mode Family Google Drive Sync**:
  - Implemented choice between **Mode A (Single Shared Google Account)** and **Mode B (Individual Google Accounts)** in Family Sync settings.
  - Added automated in-app family email invitations via Google Drive Permissions API (`POST /drive/v3/files/{folderId}/permissions`).
  - Added static, reusable QR Code & WhatsApp share link generation (`palatelog://sync?folderId=<folderId>`) encoding the immutable family `folderId`.
  - Implemented graceful permission pending error handling catching HTTP 404/403 when an uninvited family member scans the QR code, with a 1-tap **Ask Host via WhatsApp** pre-filled message action.

### Changed
- **Release Build R8 Minification & Resource Shrinking**:
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` to optimize release AAB size, protect bytecode, and automatically supply deobfuscation mapping files to Google Play Console.
  - Added ProGuard rules in `proguard-rules.pro` for Room Database and Kotlinx Serialization data models.

## [1.2.4] - 2026-10-21

### Fixed
- **In-App Update Background Loop Fix**:
  - Prevented re-triggering update check loops in `onResume()`, resolving app backgrounding/closing when launching Google Play's system update dialog.
- **In-App Update "Restart App" Banner Layout**:
  - Added `.navigationBarsPadding()` to the bottom update banner in `MainActivity`, preventing overlap with Android system navigation gesture/button bar.
- **Layout & Double Inset Padding Fixes**:
  - Eliminated double Scaffold bottom padding in `PalateLogNavGraph`, `SettingsScreen`, and `DashboardScreen`.
  - Added bottom scroll spacer ensuring cards like "Offline Backup & Transfer" and "About PalateLog" are fully visible and never cut off.

## [1.2.3] - 2026-10-21

### Fixed & Enhanced
- **Google Shared Drive Single-File In-Place Sync**:
  - Replaced timestamped sync files with a single canonical file per device (`changes_<deviceId>.json`).
  - Implemented in-place `PATCH` updates on Google Drive, preventing folder clutter and eliminating missed entries caused by cutoff timestamps.
- **Visit & Dish Member Attribution**:
  - Automatically attaches the primary user's display name (`createdByMemberName`) to visit entries and dish feedback.
  - Displayed `👤 Logged by <MemberName>` on Visit History timeline cards in `RestaurantDetailScreen`.
- **Expandable Speed Dial FAB (Dashboard)**:
  - Replaced static action buttons with an expandable Material 3 Speed Dial FAB menu featuring smooth rotation and fade animations.
  - Added `140dp` bottom list padding so the last restaurant card can scroll completely above the action buttons.
- **Google Shared Drive Card UI Improvements**:
  - Re-laid out the Google Drive Connected status card with text truncation (`TextOverflow.Ellipsis`) and single-line wrapping to prevent button overlap on narrow screens.
- **Personalized Primary Profile Setup**:
  - Updated default primary member profile name to `"Me"`.
  - Added an **Edit Profile Name ✏️** action in `SettingsScreen` allowing each user to set their personalized name (e.g., `"Karthik"`, `"Priya"`).

## [1.2.2] - 2026-10-21

### Added
- **Google Play In-App Updates Integration**:
  - Integrated `com.google.android.play:app-update-ktx` for automatic update detection and seamless installation flows.
  - Implemented background **Flexible Updates** with an interactive Material 3 **Bottom "Restart App" Banner** upon download completion.
  - Implemented **Immediate / Forced Updates** for critical releases or releases matching configurable version staleness thresholds.

## [1.2.1] - 2026-10-21

### Added
- **Custom Color-Coded Map Pin Markers**:
  - Replaced plain map markers with vector-rendered pin badges on `MapScreen` and `LocationPickerDialog`.
  - Color-coded by overall verdict rating: 🟢 Emerald Green for **GO**, 🔴 Crimson Red for **NO-GO**, and 🟡 Amber Yellow for **CAUTION**.
  - Added category vector icons inside pins corresponding to restaurant type: Dine-in / Restaurant (🍽️), Highway Dhaba (🛖), Delivery / Cloud Kitchen (🛵), Cafe (☕), and Takeaway / Drive-Thru (🚗).
  - Designed distinct GPS user location pulse marker and custom location target pin.
  - Implemented `MapMarkerFactory` with memoized bitmap caching for zero-overhead rendering when panning or zooming the map.
- **Interactive Map Location Picker for Log Visit & Quick Capture**:
  - Added a **Pick on Map 🗺️** / **Change on Map** button to both **Log visit / Order** (`EntryEditorScreen`) and **3-Second Quick Log** (`QuickCaptureScreen`).
  - Implemented `LocationPickerDialog` featuring an interactive `osmdroid` map view with tap-to-pin placement and single-touch location locking.
  - Added automatic reverse-geocoding upon map location confirmation via `LocationClient.reverseGeocode(lat, lng)`, updating latitude, longitude, area/street, city, and nearby restaurant suggestion dynamically.

## [1.2.0] - 2026-10-21

### Added
- **Google Shared Drive Direct Cloud Sync (Phase 2B)**:
  - Integrated Google Account selection (`com.google.android.gms.common.AccountPicker`) and OAuth2 authorization (`https://www.googleapis.com/auth/drive.file` scope) for seamless cloud sync.
  - Implemented `GoogleDriveSyncClient` using OkHttp REST API calls to search, create, upload, list, and download JSON delta payloads to/from a dedicated `PalateLog_Family` folder in Google Drive.
  - Updated `SyncRepository` with 2-way delta merge capability (`performGoogleDriveSync()`) using Last-Write-Wins (LWW) conflict resolution and unique per-device delta files (`changes_<deviceId>_<timestamp>.json`).
  - Updated `SyncWorker` to run Google Drive cloud sync automatically in the background via WorkManager when connected.
  - Enhanced `SettingsScreen` with Google Drive Connection Card showing signed-in Google account email, connection status, Disconnect button, and manual "Sync Now" trigger.
- **JSON Backup File Import Picker**:
  - Added a **Select Backup File 📁** button in the Settings Import Dialog launching Android's system file picker (`ActivityResultContracts.GetContent()`), allowing users to restore backup files directly from device storage or cloud drives.

### Changed
- **Target SDK Upgrade to API 36**:
  - Updated `compileSdk` and `targetSdk` to level 36 (Android 16) in `app/build.gradle.kts` to meet Google Play Console publishing requirements.
  - Bumped `versionCode` to `5`.

### Fixed
- **WorkManager Startup Initialization Lint Error**:
  - Removed default `androidx.work.WorkManagerInitializer` from `AndroidManifest.xml` via `tools:node="remove"` to resolve fatal lint error (`RemoveWorkManagerInitializer`) during release bundle assembly when using Hilt's custom `Configuration.Provider`.

## [1.1.0] - 2026-03-09

### Changed
- **Application ID & Namespace Rename**:
  - Refactored package name and app ID from `com.karthik.palatelog` to `com.prismpixelvertex.palatelog` across Gradle scripts, source code, and project documentation.

### Fixed
- **JSON Export & Share Crash**:
  - Added root cache path (`<cache-path name="cache_files" path="." />`) and files path (`<files-path name="internal_files" path="." />`) to `res/xml/file_paths.xml` so `FileProvider` can grant URI access to generated backup JSON files.
  - Wrapped `FileProvider.getUriForFile` and `startActivity` share intent chooser inside `SettingsScreen.kt` in a `try-catch` block to prevent app crashes if sharing fails or if no sharing application is available.

## [1.0.2] - 2025-02-23

### Added
- **Interactive Map Location Picker**:
  - Added a **Pick on Map 🗺️** button in `EditRestaurantDialog` launching `LocationPickerDialog`.
  - Embedded an interactive OSMDroid map view in a modal dialog with single-tap and long-press pin placement and real-time reverse geocoding via `LocationClient`.
- **Restaurant Details & Geolocation Editing**:
  - Added an **Edit Restaurant ✏️** action in the top app bar of `RestaurantDetailScreen`.
  - Added an `EditRestaurantDialog` allowing users to edit restaurant name, street address, city, highway/route, cuisine, restaurant type, and overall verdict.
  - Added precise geolocation editing with latitude and longitude numeric fields and a 1-tap **Use GPS** action to fetch live GPS coordinates and auto-fill address/city.
  - Updated `RestaurantDetailViewModel` to use `restaurantDao.update` (and `EntryEditorViewModel` for existing restaurant edits).
- **Visit Entry Editing & Deletion**:
  - Added **Edit ✏️** and **Delete 🗑️** action icons to each `VisitHistoryCard` in `RestaurantDetailScreen`.
  - Added `EditVisitDialog` allowing users to modify order channel, total bill amount, visit verdict, and general notes.
  - Added soft-deletion for individual visit entries via `deleteVisit` in `RestaurantDetailViewModel`.
- **Reusable Verdict Choice Chips**:
  - Extracted `VerdictChoiceChip` into `com.karthik.palatelog.core.designsystem` for consistent design system usage across `EntryEditorScreen` and `RestaurantDetailScreen`.
- **Documentation & Architecture Decision Records (ADR)**:
  - Added **ADR 007: In-Place Metadata & Geolocation Correction vs. Immutable Logging** in `doc/decisions_and_tradeoffs.md`.
  - Updated requirements traceability matrix in `doc/requirements_traceability_and_roadmap.md`.

### Fixed
- **Foreign Key Cascade Deletion on Restaurant Updates**:
  - Fixed an issue where updating an existing restaurant replaced the parent row in SQLite via `OnConflictStrategy.REPLACE` (`DELETE` + `INSERT`), triggering foreign key `ON DELETE CASCADE` and wiping child visit history and dish feedback records. Replaced `restaurantDao.insert` with `restaurantDao.update` for existing entity updates, keeping all visit history completely intact.

## [1.0.1] - 2025-02-23

### Added
- **Interactive Map Spot Selection & Location Logging**:
  - Added single tap and long-press gesture handling on `MapScreen` using `MapEventsOverlay` to drop or move a custom **Selected Spot 📍** pin anywhere on the map.
  - Asynchronous reverse geocoding via `LocationClient` automatically resolves area/street and city details for tapped map points.
  - Added interactive bottom banner on `MapScreen` for custom map pins with one-tap actions for **Quick Log (3s)** and **Full Log** pre-filled with the selected coordinates and geocoded location.
  - Added a **Log Visit ➕** button to existing restaurant map cards, allowing users to quickly log a new visit to any existing restaurant directly from the map view.
  - Updated `QuickCaptureViewModel` and `EntryEditorViewModel` to accept custom latitude, longitude, area, and city navigation arguments passed from map selections.
  - Added editable Area, City, and Pinned Map Location indicators to `EntryEditorScreen`.
- **Custom Order / Visit Channels**:
  - Expanded default channels to include **Ownly**, **EatClub**, and **Magicpin**.
  - Added an `+ Add Option` chip and custom option creation dialog in `EntryEditorScreen` so users can add any new channel (e.g., "Ownly") on the fly.
  - Custom channel choices persist across app launches in local preferences and are retrieved from Room DB visit history.
  - Updated `ChannelBadge` to support dynamic channel display names with contextual icons (e.g., `DeliveryDining`).
  - Updated `DashboardViewModel` delivery filter to recognize delivery apps like Ownly.
- **Project Guidelines**:
  - Added `GEMINI.md` defining strict development guidelines for Git confirmation, issue analysis/RCA, feature proposals, and changelog maintenance.

### Fixed
- **External Map Navigation (`ExternalMapNavigator`)**:
  - Added `<queries>` declarations in `AndroidManifest.xml` for package visibility on Android 11+ (API Level 30+).
  - Implemented a robust 3-tier fallback chain (`google.navigation:` app -> `geo:` scheme -> `https://www.google.com/maps/...` browser) in `ExternalMapNavigator.kt`. Navigation now reliably opens driving directions across all devices, tablets, and emulators with or without Google Maps installed.
- **Keyboard Overlap on Log Visit / Order Screen**:
  - Added `.consumeWindowInsets(innerPadding).imePadding()` on main scrollable containers in `EntryEditorScreen`, `QuickCaptureScreen`, and `SettingsScreen`.
  - Input fields near the bottom (such as "General Visit Remarks", bill amounts, dish notes) now scroll smoothly above the software keyboard.

---

## [1.0.0] - 2025-02-23

### Added
- Initial Clean Architecture implementation using Jetpack Compose, Room Database, Hilt Dependency Injection, and Kotlin Coroutines.
- Core screens: Dashboard, Interactive OpenStreetMap, Settings & Google Shared Drive Sync, 3-Second Quick Capture Logger, and Full Visit/Dish Editor.
- Unit tests for distance calculation and location bounding box in `GeoUtilsTest`.

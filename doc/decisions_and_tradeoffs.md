# PalateLog: Architecture Decision Records (ADRs) & Trade-offs

This document records the architectural and design decisions made for PalateLog, including context, alternatives considered, decision rationale, and trade-offs.

---

## ADR 001: App Identity & Naming

### Context
We needed an identity that conveys both positive discoveries and negative warnings (e.g. roadside tea regrets, delivery dish disappointments), suitable for personal and family use.

### Candidates Evaluated
1. **BiteMark**: Catchy, punchy ("never get bitten twice").
2. **TasteTrail**: Travel & highway-focused.
3. **NeverAgain!**: Humorous, warning-focused.
4. **PalateLog** *(Selected)*: Clean, gourmet, journal-like, balances recommendations with warnings.

### Decision
Adopted **PalateLog** with application ID `com.prismpixelvertex.palatelog` and tagline:  
> *"Never repeat a dining regret; remember every family favorite."*

---

## ADR 002: Plug-and-Play Map Architecture: Mappls (MapmyIndia) with OSMDroid Fallback & Navigation Handoff

### Context
Requirement 2 calls for interactive map search and geocoded visual exploration. Initially, PalateLog adopted OSMDroid (OpenStreetMap). However, Indian statutory regulations (the Criminal Law Amendment Act, 1961, the National Geospatial Policy, 2022, and Survey of India mandates) require maps published and distributed in India to depict the official international borders of India (including the entirety of Jammu & Kashmir, Ladakh, Aksai Chin, and Arunachal Pradesh). Standard OpenStreetMap raster tiles render borders based on de facto ground control (Line of Control / Line of Actual Control), which does not adhere to official Indian boundaries.

Furthermore, we needed a plug-and-play architecture so that changing map providers in the future (e.g., to Google Maps, Ola Maps, or iOS MapKit) incurs zero changes to feature screens or data models.

### Options Considered
* **Option A: Google Maps Android SDK**: Native Google maps rendering with dynamic Indian boundary localization, but requires mandatory Google Cloud project creation, billing account, and credit card setup.
* **Option B: Pure OSMDroid (OpenStreetMap)**: 100% free and open-source, but non-compliant with Survey of India boundary guidelines by default.
* **Option C: Mappls (MapmyIndia) Android SDK**: 100% compliant with Survey of India and Government of India geospatial mandates, rich Indian POI/dhaba data, but introduces vendor SDK dependency.
* **Option D: Plug-and-Play Strategy & Facade Architecture (Selected)**:
  - Universal Composable Facade: [`PalateInteractiveMap`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateInteractiveMap.kt) exposes a vendor-neutral contract (`PalateMapMarker`, `MapProviderType`).
  - Primary Renderer: [`MapplsMapRenderer`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/MapplsMapRenderer.kt) using Mappls Android SDK v2.0+ and signed application configuration files (`.a.conf` / `.a.olf`).
  - Graceful Fallback Renderer: [`OsmDroidMapRenderer`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/OsmDroidMapRenderer.kt) automatically activates if Mappls credentials are missing, uninitialized, or encounter runtime quota errors.
  - Zero-Cost Live Navigation: Preserved via Android `geo:` intent handoff in [`ExternalMapNavigator`](../app/src/main/java/com/prismpixelvertex/palatelog/core/navigation/ExternalMapNavigator.kt).

### Decision & Trade-offs
* **Benefits**: 
  - **100% Regulatory Compliance**: Guaranteed Survey of India border compliance for Google Play Store distribution in India.
  - **Plug-and-Play Extensibility**: Feature screens ([`MapScreen`](../app/src/main/java/com/prismpixelvertex/palatelog/feature/map/MapScreen.kt), [`LocationPickerDialog`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/LocationPickerDialog.kt), [`RestaurantDetailScreen`](../app/src/main/java/com/prismpixelvertex/palatelog/feature/restaurant_detail/RestaurantDetailScreen.kt)) have zero direct dependencies on any vendor SDK.
  - **KMP / iOS Readiness**: Clean separation allows plugging in Apple MapKit (`MKMapView`) for iOS or future alternative providers without altering UI logic.
  - **Zero Breakage Guarantee**: Automatic fallback to OSMDroid ensures maps never crash or render blank even if API limits are reached.
* **Trade-off**: Requires bundling Mappls SDK dependencies alongside OSMDroid for fallback capabilities.

---

## ADR 003: Dual-Mode Data Entry (The 3-Second Quick Logger vs. Full Editor)

### Context
When having a bad tea at a roadside stall or dhaba, a user does not have 3-5 minutes to fill out a 15-field form with dish prices, categories, and photos. If data entry is cumbersome, the user skips logging and the regret happens again.

### Options Considered
* **Option A: Comprehensive Form Only**: All fields (dishes, bills, tags, photos) on one screen. Too slow for field capture.
* **Option B: 3-Second Speed Dial + Deferred Full Editor (Selected)**:
  - **Quick Regret Logger (Screen A)**: 1 tap for location, 1 tap for verdict (defaults to NO-GO 🔴), 1-line reason with one-tap suggestions ("Watery tea", "Soggy food", "Dirty restrooms"). Saves in 3 seconds.
  - **Full Visit Editor (Screen B)**: Used for sitting down after a meal or online order; includes multi-dish matrix, remembered past dish chips, bill amount, and family preferences.

### Decision & Trade-offs
* Maximizes capture rate by reducing cognitive friction to under 3 seconds during live visits, while preserving rich details when the user has time.

---

## ADR 004: Serverless Google Shared Drive Sync vs. Dedicated Cloud Backend

### Context
Requirement 6 requires syncing data with family members while separating private and shared entries. Setting up a dedicated backend (e.g. Firebase, AWS Amplify, Supabase, Postgres) incurs recurring monthly costs, maintenance burden, and privacy concerns.

### Options Considered
* **Option A: Firebase / Supabase Backend**: Fast real-time sync, but incurs monthly subscription fees, vendor lock-in, and requires hosting servers.
* **Option B: Google Shared Drive Folder-Based Distributed Sync (Selected)**:
  - Family members share a dedicated Google Drive folder (e.g. `PalateLog_Family/`).
  - The app writes lightweight append-only JSON deltas (`changes_<deviceId>_<timestamp>.json`) and WebP compressed photos.
  - Two-way sync is orchestrated by AndroidX WorkManager or triggered on-demand by the user.
  - Conflict resolution uses Last-Write-Wins (LWW) with millisecond timestamps and UUID primary keys.
  - `isShared: Boolean` controls whether an entry is exported to the cloud or kept strictly local.

### Decision & Trade-offs
* **Benefits**: Zero monthly cloud costs, full user data sovereignty, private entries never leave the device.
* **Trade-off**: Sync is eventual rather than real-time sub-second WebSocket sync (ideal for restaurant visits where multi-second latency is completely harmless).

---

## ADR 005: Spatial Search & Proximity Filtering in Room SQLite

### Context
Requirement 2 & 4 require querying restaurants near the user's current GPS location or along highway corridors.

### Options Considered
* **Option A: SpatiaLite Extension**: Adds geospatial SQL functions to SQLite, but requires custom NDK binaries (.so files) and increases APK size significantly.
* **Option B: Bounding Box Pre-filter + Haversine Formula (Selected)**:
  - SQLite query filters by latitude/longitude bounding box (`latitude BETWEEN :minLat AND :maxLat`).
  - Precise distance is computed in Kotlin using the Haversine formula in `GeoUtils.kt`.
  - Results are sorted by distance and filtered by radius chips (<15km).

### Decision & Trade-offs
* Extremely fast, lightweight, zero native C++ dependencies, and 100% compatible with standard Android Room SQLite.

---

## ADR 006: JDK Runtime Strategy (OpenJDK 21 LTS)

### Context
Android Studio's bundled internal JBR is version 25 (`25.0.3` preview). Android Gradle Plugin (AGP) 8.7+ and Kotlin 2.0 compiler fail when encountering Java 25 (`java.lang.IllegalArgumentException: 25.0.3`).

### Decision
We installed **Microsoft OpenJDK 21 LTS** (`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`) via winget, set `org.gradle.java.home` in `gradle.properties`, and updated `gradlew.bat`.  
This guarantees clean, repeatable, LTS-backed Gradle builds across command line and Android Studio.

---

## ADR 007: In-Place Metadata & Geolocation Correction vs. Immutable Logging

### Context
When logging entries or quick regrets in the field, GPS precision might vary or address geocoding might yield inaccurate street names/coordinates (e.g., misidentified "California Burrito" address). Users need a simple way to edit restaurant metadata (Name, Address, City, Highway/Route, Map Latitude/Longitude, Cuisine, Type, Verdict) and visit records without deleting and recreating entities or creating duplicates.

### Options Considered
* **Option A: Immutable Log Entries**: Disallow editing past entities. Require deleting and recreating a restaurant or visit entry. High user friction and risk of orphaned records.
* **Option B: In-Place Contextual Editing in Restaurant Detail View (Selected)**:
  - Provide an "Edit Restaurant Details" dialog in `RestaurantDetailScreen` with editable fields for address, city, route, cuisine, type, overall verdict, and precise map coordinates (lat/lng) supported by a 1-tap "Use Current GPS Location" button.
  - Provide inline edit and delete actions on `VisitHistoryCard` components for past visits.
  - Retain the entity's UUID primary key (`id`) and update the `updatedAt` timestamp to ensure Last-Write-Wins (LWW) conflict resolution compatibility during offline-first JSON delta syncs.

### Decision & Trade-offs
* **Benefits**: High flexibility, instantaneous correction of address/coordinate errors, zero orphaned child entities (dishes/visits remain correctly linked), and full sync compatibility.

---

## ADR 008: Static Analysis & IDE Lint Observation Triage Strategy

### Context
A comprehensive IDE code inspection audit (351 reported items in `index.html`) was conducted. While static analysis provides valuable insights into memory efficiency and deprecated APIs, indiscriminately applying automated quick-fixes can introduce build instability, break existing runtime mechanisms, or reverse intentional product constraints. We categorized all observations and established an explicit policy distinguishing genuine fixes from intentional exceptions and deferred tasks.

### Decisions & Untouched Warnings (False Positives / Intentional Design)

1. **Device Form-Factor Gate (`android.hardware.touchscreen` `required="true"`)**:
   - *Decision*: Retained intentionally with `tools:ignore="UnnecessaryRequiredFeature"`.
   - *Rationale*: PalateLog is engineered strictly for mobile handheld dining and travel journal capture. We explicitly restrict Play Store eligibility away from Android TV and non-touch form factors.
2. **Stable Device Identity (`Settings.Secure.ANDROID_ID`)**:
   - *Decision*: Retained with `@SuppressLint("HardwareIds")`.
   - *Rationale*: Used exclusively as a persistent device ID anchor in personal Google Drive sync delta filenames (`changes_<deviceId>.json`). Migrating to a randomized app UUID at this phase would invalidate existing family device sync states and cause delta fragmentation.
3. **Dependency & Toolchain Upgrades**:
   - *Decision*: Kept stable library versions in `libs.versions.toml`; deferred upgrades.
   - *Rationale*: Upgrades flagged by Lint (e.g., AGP 9.5.x, Kotlin 2.4.x, compileSdk 37, Hilt 2.60.x) contain breaking changes. Core toolchain upgrades are managed as isolated, planned milestone tasks with full regression testing.
4. **Framework & Reflection Entry Points (`ChannelBadge`, Room `Converters`)**:
   - *Decision*: Retained without modifications.
   - *Rationale*: Flagged as "unused declarations" because IDE static reachability does not trace Room SQLite type converter generation and dynamic Compose entry points.
5. **`forceFullExport` Parameter Extensibility in `SettingsViewModel`**:
   - *Decision*: Retained with `@Suppress("SameParameterValue")`.
   - *Rationale*: Preserves clean API extensibility for upcoming background or incremental sync triggers without mutating the function signature.
6. **Domain Vernacular & Programmatic Pluralization**:
   - *Decision*: Retained "Drive-Thru" and singular/plural visits strings with `tools:ignore="Typos,Spelling,PluralsCandidate"`.
   - *Rationale*: "Drive-Thru" is standard industry vernacular. Plural formatting is already handled deterministically in Kotlin Compose code (`if (item.totalVisits == 1)`).
7. **Complete String Extraction & Localization Support (Completed in v1.2.12)**:
   - *Decision*: Successfully extracted 80+ hardcoded Compose strings into `res/values/strings.xml`.
   - *Rationale*: Achieved 100% localization readiness across all Compose screens and dialogs while eliminating `HardcodedText` warnings.

### Genuine Fixes Applied
- Quoted Mermaid node strings containing emojis and quotes in `architecture_and_design.md` to resolve AST parser errors.
- Added `android:value="androidx.startup"` on WorkManagerInitializer removal in `AndroidManifest.xml` and removed redundant `MainActivity` label.
- Replaced `mutableStateOf` with `mutableDoubleStateOf` in `RestaurantDetailScreen.kt` to eliminate primitive boxing.
- Passed explicit `Locale.getDefault()` in `GeoUtils.kt` distance formatting.
- Replaced `Channel.values()` with `Channel.entries` for allocation-free enumeration in Kotlin 2.0.
- Replaced `Uri.parse()` with `.toUri()` extension in `ExternalMapNavigator.kt` and `SettingsScreen.kt`.
- Replaced `prefs.edit().apply()` with KTX `prefs.edit { ... }` in `SyncRepository.kt` and `EntryEditorViewModel.kt`.
- Migrated `MapMarkerFactory.kt` bitmap, color, and drawable instantiation to Android KTX (`.toColorInt()`, `createBitmap()`, `.toDrawable()`).
- Removed redundant `xmlns:android` namespace declaration from `file_paths.xml`.
- Cleaned dead imports across Compose screens/ViewModels and removed redundant `.editorconfig` properties.

---

## ADR 009: Interactive Help & User Guide Architecture with Contextual Deep-Linking (`HelpTopicKey`)

### Context
PalateLog incorporates unique paradigms—such as Dual-Mode Family Sync (Mode A vs Mode B), 3-Second Quick Capture vs Full Visit Editor, and the Dish Matrix. Users need immediate, in-context explanations without leaving their current task flow.

### Decision & Trade-offs
* **Type-Safe `HelpTopicKey` Enum**: Defined a type-safe enum (`HelpTopicKey.SYNC`, `HelpTopicKey.QUICK_LOG`, `HelpTopicKey.VISIT_LOG`, `HelpTopicKey.DISH_MATRIX`, `HelpTopicKey.MAP_TRAVEL`, `HelpTopicKey.PRIVACY_OFFLINE`) to drive deep-linking navigation.
* **Contextual `?` Shortcuts**: Embedded subtle help icons beside complex section headers across screens (Settings, Quick Log, Entry Editor, Restaurant Details) navigating directly to `Screen.HelpGuide(topicKey)`.
* **Auto-Scroll & Expand**: Upon receiving a `topicKey`, `HelpGuideScreen` automatically expands the target guide card and scrolls it into view.
* **First-Launch Onboarding Welcome Dialog**: Added a one-time onboarding welcome dialog on newly installed devices offering a guided tour or deferred exploration.

---

## ADR 010: In-App Update Lifecycle & User Prompt Strategy

### Context
Google Play In-App Updates allow notifying users when a new version is published. However, aggressive update dialogs during app startup can cause background loops or disrupt the user's primary workflow.

### Decision & Trade-offs
* **Prioritized `InstallStatus.DOWNLOADED` Check**: Reordered update evaluation in `InAppUpdateHelper` to verify completed downloads before checking availability.
* **Loop-Safe `onResume()` Query**: Restored `onResume()` update checking restricted solely to completed flexible downloads or interrupted immediate updates.
* **Debug Modal (`AlertDialog`) & Non-Intrusive Bottom Banner**: Restricted modal `AlertDialog` to debug builds (`BuildConfig.DEBUG`) for testing, while production release builds use the non-intrusive bottom banner card (`AnimatedVisibility`). Enforced mutual exclusion so both prompts never render simultaneously.
* **Non-Overlapping Banner Card**: Positioned bottom update banners (`padding(bottom = 96.dp)`) safely above bottom navigation bars.

---

## ADR 011: Content-Aware Floating Scroll Indicator UI Pattern

### Context
On long scrollable lists (such as Explore restaurant cards or Restaurant Detail visit timelines), users may not realize additional cards exist below the fold.

### Decision & Trade-offs
* **Content-Aware Scroll Detection**: Evaluates `LazyListState.canScrollForward` or `ScrollState.canScrollForward` so the down-arrow indicator only appears when additional off-screen content actually exists.
* **Auto-Hide Inactivity Timer**: Fades out after 2.5 seconds of scroll inactivity to avoid obscuring card details or action buttons, reappearing when scrolling resumes.
* **1-Tap Action**: Tapping the floating indicator smoothly scrolls down to reveal hidden content.
* **Inset Clearance**: Insets and bottom scroll padding (`bottom = 80.dp`) guarantee floating indicators sit cleanly above system 3-button navigation bars.

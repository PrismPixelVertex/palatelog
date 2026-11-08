# PalateLog: Architecture & Technical Design Specification

> **Application ID**: `com.prismpixelvertex.palatelog`  
> **Repository**: [https://github.com/karthikeyan-s/palatelog](https://github.com/karthikeyan-s/palatelog)  
> **Platform**: Android (Kotlin 2.0+, Jetpack Compose Material 3)

---

## 1. System Overview

**PalateLog** is an offline-first Android application designed to track, organize, and resurface personal and family dining learnings—from roadside tea stalls and outstation highway dhabas to online food delivery orders (Zomato/Swiggy).

The system addresses three critical memory failure modes:
1. **The Roadside Regret**: Stopping at a tea stall on a highway drive, having terrible tea, and accidentally stopping there again months later because human memory fades.
2. **The Delivery Dish Dichotomy**: Ordering from a restaurant (e.g. *Nagarjuna* on Zomato) where the Biriyani is extraordinary, but the chicken fry arrives soggy and cold. PalateLog isolates dish-level performance so you never order the soggy dish again.
3. **Outstation Family Drives**: Recalling clean restrooms, exceptional highway dhabas, or tourist traps along highway travel corridors (e.g., NH44).

---

## 2. Architectural Blueprint

PalateLog follows Google's recommended **Clean Architecture** with strict Unidirectional Data Flow (UDF) and MVVM:

```
app/src/main/java/com/prismpixelvertex/palatelog/
├── PalateLogApp.kt             # Application class (@HiltAndroidApp, WorkManager config)
├── MainActivity.kt             # Single Activity host with Edge-to-Edge & Compose NavHost
├── core/
│   ├── model/                  # Pure Kotlin domain entities
│   │   ├── Restaurant.kt       # Restaurant entity & Verdict enum (GO, NO_GO, CAUTION)
│   │   ├── VisitEntry.kt       # Visit & order entry (Channel: Walk-in, Zomato, Highway, etc.)
│   │   ├── DishFeedback.kt     # Dish feedback, verdicts (Must Try, Avoid, Meh), family reactions
│   │   └── FamilyMember.kt     # Family member profiles for taste attribution
│   ├── database/               # Room SQLite persistence layer
│   │   ├── PalateLogDatabase.kt# Room DB definition
│   │   ├── *Entity.kt          # Room entities mapping domain models
│   │   ├── *Dao.kt             # DAOs with spatial queries, FTS search, reactive Flows
│   │   └── Converters.kt       # kotlinx.serialization TypeConverters for lists & maps
│   ├── location/               # Location services & Geo mathematics
│   │   ├── LocationClient.kt   # FusedLocationProviderClient + Geocoder wrapper
│   │   └── GeoUtils.kt         # Haversine distance & bounding box calculations
│   ├── navigation/             # External integration
│   │   └── ExternalMapNavigator.kt # Zero-cost turn-by-turn navigation via Google Maps intent
│   ├── sync/                   # Offline-first synchronization & distributed conflict resolution
│   │   ├── SyncModels.kt       # Serializable JSON delta payload schemas
│   │   ├── SyncRepository.kt   # Delta export, LWW conflict-merging, JSON backup
│   │   ├── GoogleDriveSyncClient.kt # Drive REST API client for Mode A & Mode B sync
│   │   └── SyncWorker.kt       # WorkManager CoroutineWorker for background sync
│   ├── update/                 # Google Play In-App Updates
│   │   └── InAppUpdateHelper.kt# Flexible & immediate update manager
│   ├── util/                   # Storage & media utilities
│   │   └── MediaUtils.kt       # Internal image persistence & compression
│   └── designsystem/           # Material 3 UI design system
│       ├── Color.kt            # Curated gourmet color palette (Amber, Emerald, Crimson)
│       ├── Dimens.kt           # Centralized spacing, radii, icon, and button tokens
│       ├── Theme.kt            # Material 3 dark/light theme definitions
│       ├── Type.kt             # Typography scale
│       ├── LocationPickerDialog.kt # Map location selection dialog
│       ├── CameraXCaptureDialog.kt # In-app CameraX capture dialog with guaranteed rear-camera selection
│       ├── MapMarkerFactory.kt # Color-coded map pin generator
│       ├── PalateInteractiveMap.kt # Universal plug-and-play composable map facade
│       ├── MapplsMapRenderer.kt # Mappls (MapmyIndia) Survey-of-India compliant vector renderer
│       ├── OsmDroidMapRenderer.kt # OpenStreetMap resilient offline/error fallback renderer
│       ├── PalateMapModels.kt  # MapProviderType & PalateMapMarker data contracts
│       └── Components.kt       # Badges (VerdictBadge, DishVerdictBadge, ChannelBadge, etc.)
├── feature/                    # UI Feature modules
│   ├── navigation/             # App navigation graph & bottom navigation bar
│   ├── dashboard/              # Home explore screen, unified search, category filters, cards
│   ├── quick_capture/          # The 3-second rapid regret logger
│   ├── entry_editor/           # Full visit editor with prefilled remembered dish chips
│   ├── restaurant_detail/      # Dish Matrix (Avoid vs Must Try) & visit history timeline
│   ├── map/                    # Interactive Map (Mappls / OSM fallback) with color-coded markers & preview
│   ├── help/                   # Help & User Guide screen with deep linking (HelpTopicKey)
│   └── settings/               # Family profiles, sync controls, and backup export/restore
└── di/                         # Hilt Dependency Injection modules
    ├── DatabaseModule.kt
    └── LocationModule.kt
```

---

## 3. Data Model & Entity Relations

```mermaid
erDiagram
    RestaurantEntity ||--o{ VisitEntryEntity : "has multiple visits"
    RestaurantEntity ||--o{ DishFeedbackEntity : "accumulates dish history"
    VisitEntryEntity ||--o{ DishFeedbackEntity : "records dishes in visit"
    FamilyMember ||--o{ DishFeedbackEntity : "has individual preference"

    RestaurantEntity {
        string id PK "UUID"
        string name
        double latitude
        double longitude
        string address
        string city
        string highwayOrRoute
        string cuisine
        string restaurantType "DINE_IN, DHABA, CAFE, CLOUD_KITCHEN, DRIVE_THRU"
        string overallVerdict "GO, NO_GO, CAUTION"
        list tags
        boolean isShared
        long createdAt
        long updatedAt
        boolean isDeleted "Tombstone for sync"
    }

    VisitEntryEntity {
        string id PK "UUID"
        string restaurantId FK
        long visitDate
        string channel "WALK_IN, HIGHWAY_STOP, ZOMATO, SWIGGY, TAKEAWAY"
        string visitVerdict "GO, NO_GO, MIXED"
        double billAmount
        string billReceiptPhotoUri
        string generalNotes
        string createdByMemberId
        boolean isShared
        long updatedAt
        boolean isDeleted
    }

    DishFeedbackEntity {
        string id PK "UUID"
        string visitId FK
        string restaurantId FK
        string dishName
        string category "MAIN, APPETIZER, BEVERAGE, DESSERT, SIDE, SNACK"
        string verdict "GO (Must Try), NO_GO (Avoid), MEH (Neutral)"
        list tags "e.g. Soggy, Spicy, Authentic"
        map memberPreferences "MemberName -> Reaction (e.g. Wife: Loved it)"
        double price
        list photoUris
        boolean isShared
        long updatedAt
        boolean isDeleted
    }
```

---

## 4. Primary Workflows

### A. The 3-Second Quick Regret Logger (Speed-First Capture)
```mermaid
graph TD
    A["Tap Floating 'Quick Regret Log' 🔴"] --> B[Auto-fetch GPS & Geocoded Address]
    B --> C{Nearby Spot Found within 50m?}
    C -->|Yes| C1[Show One-Tap Reuse Banner]
    C -->|No| C2[Auto-fill Area Name]
    C1 --> D[Select 1-Tap Verdict: NO-GO / GO / CAUTION]
    C2 --> D
    D --> E["Select Quick Reason Chip: 'Watery tea', 'Soggy food', 'Dirty restrooms'"]
    E --> F["Tap 'Save & Finish (3s)'"]
    F --> G[Atomic insert to Room DB & return to Explore]
```

### B. The Zomato / Swiggy Delivery Check & Re-ordering
```mermaid
graph TD
    A[Open App before ordering on Zomato] --> B[Search 'Nagarjuna']
    B --> C[Tap Restaurant Card to Open Details]
    C --> D[Review Dish Matrix]
    D --> D1["🟢 Must-Try: Biriyani - 'Fragrant spices'"]
    D --> D2["🔴 Avoid: Chicken Fry - 'Became soggy during delivery'"]
    D --> D3["👥 Family Notes: 'Wife loved Mutton Fry; Karthik avoids Chicken Fry'"]
    D --> E[Order safely on Zomato without regrets]
    E --> F[Tap 'New Visit' in PalateLog]
    F --> G[Dishes pre-filled as selectable chips for instant 1-tap re-logging]
```

### C. Contextual Help & User Guide Navigation
```mermaid
graph TD
    A["Tap '?' Shortcut Icon on Any Screen"] --> B["Navigate to Screen.HelpGuide with HelpTopicKey"]
    B --> C["HelpGuideScreen expands target card & auto-scrolls"]
    C --> D["User reviews visual paradigms (Sync Mode A/B, Quick Log vs Full Log, Dish Matrix)"]
    D --> E["Tap '← Back' to return seamlessly to previous screen"]
```

---

## 5. Security, Privacy & Sync Architecture (v1.2.14)

- **Private vs. Shared Isolation**: Each entity carries an `isShared: Boolean` flag. Private entries remain strictly in the local SQLite database. Only shared entries are synchronized to the shared family storage.
- **Offline-First & Sovereign**: The app operates 100% offline. All writes go directly to Room SQLite.
- **Zero-Cost Family Cloud Sync (Dual-Mode)**:
  PalateLog supports flexible, zero-cost cloud sync across family devices without requiring custom server infrastructure. Users can choose between two modes based on their technical preference:

  ### Mode A: Single Shared Family Google Account (Zero-Config)
  - **Setup**: All family members sign into PalateLog using a single designated Gmail address (e.g. `ourfamily.palatelog@gmail.com`).
  - **Behavior**: The app automatically searches for or creates the `PalateLog_Family/` folder in the root directory of that account's Google Drive.
  - **Target Audience**: Non-technical families looking for a 1-tap setup with zero QR codes or invite steps.

  ### Mode B: Individual Google Accounts (Automated In-App Invites & Static QR Code)
  - **Setup**: Each family member signs into PalateLog using their personal Gmail account.
  - **Host Provisioning**: The family host creates the `PalateLog_Family/` folder once. The generated `folderId` is permanent and static.
  - **In-App Automated Invites**: The host enters family member email(s) inside PalateLog. PalateLog automatically grants write access via the Google Drive Permissions REST API (`POST /drive/v3/files/{folderId}/permissions`).
  - **Static QR Code & Deep Link Sharing**: The host shares a static QR code or WhatsApp deep link (`palatelog://sync?folderId=<folderId>`). Family members scan or tap the link to store the target `folderId` in their app.
  - **Graceful Error Recovery & WhatsApp Request**: If an uninvited user scans the QR code before the host adds their email, Google Drive API returns an authorization error (HTTP 404/403). PalateLog catches this error and presents a friendly *Permission Pending* screen with a 1-tap **"Ask Host via WhatsApp"** pre-filled request message.

- **Sync Payload Format**: Data is synchronized as compact canonical JSON delta files (`changes_<deviceId>.json`) updated in-place via Google Drive `PATCH` API. Photos are synchronized to `PalateLog_Family/Photos/`.
- **Conflict Resolution**: UUID primary keys prevent collisions. Conflicts across devices are resolved using Last-Write-Wins (LWW) based on `updatedAt` timestamps. Soft-deleted records (`isDeleted = true`) act as tombstones so deletions propagate cleanly across all family phones.

---

## 6. Plug-and-Play Map Architecture & Provider Integration Guide (v1.2.16)

### A. Architectural Philosophy & Strategy Pattern
PalateLog uses the **Facade & Strategy Design Patterns** for all geospatial visualization. Feature screens ([`MapScreen`](../app/src/main/java/com/prismpixelvertex/palatelog/feature/map/MapScreen.kt), [`LocationPickerDialog`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/LocationPickerDialog.kt), [`RestaurantDetailScreen`](../app/src/main/java/com/prismpixelvertex/palatelog/feature/restaurant_detail/RestaurantDetailScreen.kt)) have **zero direct dependencies** on vendor map libraries (such as Mappls SDK, OpenStreetMap/OSMDroid, or Google Maps Play Services).

Instead, all map interactions pass through a single universal composable facade:
- **Facade**: [`PalateInteractiveMap`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateInteractiveMap.kt)
- **Data & State Contracts**: [`PalateMapMarker`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt), [`PalateMapState`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt), [`MapProviderType`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt)
- **Runtime Orchestrator**: `MapProviderConfig` handles dynamic provider selection, initialization status, and automated runtime error fallback.

```mermaid
graph TD
    subgraph UI Feature Layer
        MS[MapScreen]
        LPD[LocationPickerDialog]
        RDS[RestaurantDetailScreen]
    end

    subgraph Facade and Strategy
        PIM["PalateInteractiveMap (Composable Facade)"]
        MPC["MapProviderConfig (Fallback and Status StateFlow)"]
    end

    subgraph Concrete Renderers
        MMR["MapplsMapRenderer (Primary: Survey of India Compliant)"]
        OMR["OsmDroidMapRenderer (Fallback: Offline Capable)"]
        GMR["GoogleMapsRenderer (Future Plug-and-Play)"]
        OLR["OlaMapsRenderer (Future Plug-and-Play)"]
    end

    MS -->|Uses| PIM
    LPD -->|Uses| PIM
    RDS -->|Uses| PIM

    PIM -->|Reads active and fallback state| MPC
    PIM -->|Dispatches to| MMR
    PIM -->|Fallback on error| OMR
    PIM -.->|Plugs into| GMR
    PIM -.->|Plugs into| OLR
```

---

### B. Developer Guide: Steps to Add a New Map Provider

Follow these 6 steps to add any new map vendor (e.g. Google Maps, Ola Maps, Mapbox, or iOS Apple MapKit for KMP) without touching UI feature code:

#### Step 1: Declare the Provider in `MapProviderType`
Open [`PalateMapModels.kt`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateMapModels.kt) and add the enum value to `MapProviderType`:

```kotlin
enum class MapProviderType(
    @StringRes val nameResId: Int,
    @StringRes val attributionResId: Int,
    val isGovernmentCompliant: Boolean,
) {
    MAPPLS(...),
    OSM_DROID(...),
    GOOGLE_MAPS(
        nameResId = R.string.map_provider_google_name,
        attributionResId = R.string.map_provider_google_attr,
        isGovernmentCompliant = true,
    ),
    NEW_PROVIDER(
        nameResId = R.string.map_provider_new_name,
        attributionResId = R.string.map_provider_new_attr,
        isGovernmentCompliant = false, // Set true if compliant with Survey of India boundaries
    );
}
```
*Add the corresponding strings in `res/values/strings.xml` for provider name and legal attribution.*

#### Step 2: Configure Dependencies & Credentials
1. **Dependencies**: Add the vendor SDK to `gradle/libs.versions.toml` and implement it in `app/build.gradle.kts`.
2. **Security**: Ensure vendor API key files or certificate tokens are added to `.gitignore`.
3. **CI / CodeQL Safeguard**: If the vendor plugin checks for license files at build time (like Mappls does with `.a.olf`), ensure dummy placeholder files are generated automatically via `ensureMapplsConfigFiles` in `app/build.gradle.kts` and `.github/workflows/codeql.yml` so that headless CI builds and open-source contributors can compile without private keys.

#### Step 3: Implement the Concrete Composable Renderer
Create `app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/<ProviderName>MapRenderer.kt`.

Implement the standard renderer signature:
```kotlin
@Composable
fun NewProviderMapRenderer(
    modifier: Modifier = Modifier,
    centerLatitude: Double,
    centerLongitude: Double,
    zoomLevel: Double = 16.0,
    animationTrigger: Long = 0L,
    markers: List<PalateMapMarker> = emptyList(),
    onSingleTap: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onLongPress: ((latitude: Double, longitude: Double) -> Unit)? = null,
    onMarkerClick: ((markerId: String) -> Unit)? = null,
    onInitializationFailed: (() -> Unit)? = null,
)
```

**Mandatory Implementation Standards**:
- **Lifecycle Management**: Wrap native view lifecycle in `DisposableEffect` with `LifecycleEventObserver` (handling `ON_START`, `ON_RESUME`, `ON_PAUSE`, `ON_STOP`, `ON_DESTROY`).
- **Smooth Camera Transitions**: Use `DEFAULT_MAP_CAMERA_ANIMATION_MS` (3000 ms) when animating the camera on `animationTrigger` changes for a smooth, consistent user experience.
- **Controls & Insets**: Keep built-in overlay controls (compass, scale bars) clear of the top system bar and away from the top-right "My Location" FAB (e.g. position compass at Top-Start with 16dp margins).
- **Error Handling**: On network quota exhaustion, initialization errors, or missing licenses, trigger:
  ```kotlin
  MapProviderConfig.notifyFallbackEngaged()
  onInitializationFailed?.invoke()
  ```

#### Step 4: Register the Renderer in `PalateInteractiveMap`
Open [`PalateInteractiveMap.kt`](../app/src/main/java/com/prismpixelvertex/palatelog/core/designsystem/PalateInteractiveMap.kt) and add the dispatch branch:

```kotlin
when (effectiveProvider) {
    MapProviderType.MAPPLS -> MapplsMapRenderer(...)
    MapProviderType.OSM_DROID -> OsmDroidMapRenderer(...)
    MapProviderType.NEW_PROVIDER -> {
        NewProviderMapRenderer(
            modifier = modifier,
            centerLatitude = targetLat,
            centerLongitude = targetLng,
            zoomLevel = targetZoom,
            animationTrigger = animationTrigger,
            markers = markers,
            onSingleTap = onSingleTap,
            onLongPress = onLongPress,
            onMarkerClick = onMarkerClick,
            onInitializationFailed = {
                fallbackToOsm = true
                MapProviderConfig.notifyFallbackEngaged()
            }
        )
    }
}
```

#### Step 5: Initialize the SDK in `PalateLogApp`
In [`PalateLogApp.kt`](../app/src/main/java/com/prismpixelvertex/palatelog/PalateLogApp.kt), add safe initialization wrapped in `try/catch`:

```kotlin
try {
    NewProviderSdk.initialize(this, apiKey)
    MapProviderConfig.notifyInitialized(success = true)
} catch (e: Exception) {
    Log.w("PalateLogApp", "New provider init failed; falling back to OSM", e)
    MapProviderConfig.notifyFallbackEngaged()
}
```

#### Step 6: Verify Automatic Settings Attribution & Testing
No UI changes are required on the Settings screen!
- [`SettingsScreen.kt`](../app/src/main/java/com/prismpixelvertex/palatelog/feature/settings/SettingsScreen.kt) observes `MapProviderConfig.providerState`.
- It automatically displays the newly configured provider's name, copyright attribution, and **Survey of India Compliant 🇮🇳** badge when active.
- If fallback engages, it automatically renders the statutory fallback notice and disclaimer.

---

## 7. Notification Architecture & Palate Radar Specification (v1.2.23)

PalateLog adopts an intentional, utility-first notification philosophy. As an offline-first dining journal and regret logger, notifications are reserved strictly for high-leverage user moments and critical sync health.

### A. Notification Channels & Categories

1. **`channel_radar_alerts` (High Importance, Sound/Vibration)**:
   - Proximity warnings when approaching logged roadside regrets (`NO_GO`) or favorite stops (`GO`) during active highway drives.
2. **`channel_family_sync` (Low Importance, Silent)**:
   - Batched digest summaries when background `SyncWorker` pulls new dining reviews or dish verdicts added by family members.
3. **`channel_sync_health` (Default Importance)**:
   - Actionable alerts dispatched **only** when user intervention is required (e.g. Google Drive OAuth expiration or revoked folder permissions). Standard offline states and transient network drops remain completely silent.
4. **`channel_radar_service` (Low Importance, Ongoing)**:
   - Persistent foreground notification while Palate Radar is actively scanning on the road.

### B. Palate Radar: On-Demand Dynamic Geofencing Lifecycle

Rather than maintaining intrusive 24/7 background location tracking, Palate Radar operates as a user-initiated trip session.

```mermaid
sequenceDiagram
    autonumber
    actor User as Driver / User
    participant QS as Quick Settings Tile (PalateRadarTileService)
    participant UI as Dashboard (Palate Radar Card)
    participant Radar as PalateRadarService
    participant Room as Room SQLite (RestaurantDao)
    participant Geo as Play Services GeofencingClient
    participant Receiver as PalateRadarReceiver

    User->>QS: Tap 'Palate Radar' (or toggle on Dashboard)
    QS->>Radar: Start Foreground Service
    Radar->>Room: Query highway stops within 500 km radius
    Room-->>Radar: Return up to 100 candidate stops (NO_GO & optional GO)
    Radar->>Geo: Register geofences (500m radius per stop)
    
    Note over User,Geo: Driving on Highway (e.g. NH44)
    
    Geo->>Receiver: Trigger GEOFENCE_TRANSITION_ENTER (Sri Krishna Dhaba)
    Receiver->>User: High-Priority Heads-up Notification: "🔴 Avoid: Watery tea, dirty restrooms"
    
    User->>QS: Tap 'Palate Radar' (Turn OFF)
    QS->>Radar: Stop Service
    Radar->>Geo: removeGeofences() (Clean teardown)
    Note over Radar,Geo: Zero background tracking & zero battery consumption
```

For complete Google Play Store declarations, policy justifications, in-app prominent disclosure copy, and reviewer video scripts, see [play_store_location_policy_justification.md](play_store_location_policy_justification.md).




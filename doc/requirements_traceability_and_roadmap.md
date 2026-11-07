# PalateLog: Requirements Traceability Matrix & Roadmap

This document maps all original user requirements against the current codebase implementation and defines prioritized next steps.

---

## 1. Requirements Traceability Matrix

| # | User Requirement | Implementation Status | Implementing Components & Code Locations |
| :--- | :--- | :---: | :--- |
| **1** | Record feedback at Restaurant level or at food level in a restaurant | **100% Complete** ✅ | • `RestaurantEntity` (`overallVerdict` [GO, NO_GO, CAUTION])<br>• `DishFeedbackEntity` (`dishName`, `category`, `verdict` [GO, NO_GO, MEH], `tags`)<br>• `RestaurantDetailScreen` (Aggregated Dish Matrix, In-Place Editing of Restaurant & Visits)<br>• `EntryEditorScreen` (Multi-dish editor) |
| **2** | Geocoded, so that I can search about it when near that place | **100% Complete** ✅ | • `LocationClient.kt` (Fused GPS + reverse geocoding via `Geocoder`)<br>• `GeoUtils.kt` (Haversine distance calculation in meters/km)<br>• `DashboardViewModel.kt` (Near Me <15km filter & distance sorting)<br>• `RestaurantCard` (Displays distance badge e.g. "1.2 km away")<br>• `MapScreen.kt` (Interactive map with location centering)<br>• `RestaurantDetailScreen` (In-place edit of address, city, & GPS coordinates) |
| **3** | Record feedback with result (Go / No-Go) | **100% Complete** ✅ | • `Verdict` enum (`GO`, `NO_GO`, `CAUTION`)<br>• `DishVerdict` enum (`GO`, `NO_GO`, `MEH`)<br>• `VerdictBadge.kt` (Green thumbs-up, Red octagon warning, Amber caution)<br>• `DishVerdictBadge.kt` (🟢 Must Try, 🔴 Avoid, 🟡 Meh) |
| **4** | Query by various parameters (geo location, restaurant name, type, cuisine, food name) | **100% Complete** ✅ | • `DashboardViewModel.kt` (Tokenized multi-field query & relevance ranking across restaurant name, city, cuisine, highway route, tags, and specific dish names)<br>• Matched dish chips rendered directly on `RestaurantCard`<br>• `RestaurantDao` & `DishFeedbackDao.getAllDishes()` |
| **5** | Record entry type (walk-in, online order, which app used, remarks like who liked food) | **100% Complete** ✅ | • `Channel` enum (`WALK_IN`, `HIGHWAY_STOP`, `ZOMATO`, `SWIGGY`, `TAKEAWAY`, `DRIVE_THRU`)<br>• `ChannelBadge.kt`<br>• `memberPreferences: Map<String, String>` on `DishFeedbackEntity`<br>• `FamilyReactionTag.kt` (e.g. "Wife: Loved it, Karthik: Avoid")<br>• Dashboard Filter Chip: "🛵 Zomato / Swiggy" |
| **6** | Sync data to Google Shared Drive (separate private vs shared data entries) | **100% Complete** ✅ | • `isShared: Boolean` on all entities<br>• `SyncRepository.kt` (Exports only `isShared == true` records; private records stay local)<br>• Two-way delta merge with Last-Write-Wins (LWW) conflict resolution<br>• `GoogleDriveSyncClient.kt` (Dual-mode Google Sign-In + Drive REST API client)<br>• `PalateLog_Family` folder and `Photos/` subfolder in Google Drive<br>• WorkManager background sync (`SyncWorker.kt`) |
| **7** | Store pictures along with entries (optionally shown when viewing details) | **100% Complete** ✅ | • Multi-photo support (`photoUris: List<String>` in `DishFeedbackEntity`, `photoUris` / `billReceiptPhotoUri` in `VisitEntryEntity`)<br>• System Gallery (`GetContent("image/*")`) & Camera (`TakePicture`) integration<br>• Internal storage persistence via `MediaUtils.copyUriToInternalStorage()`<br>• Coil `AsyncImage` gallery rendering & `FullscreenPhotoDialog` preview<br>• Automatic 2-way Google Drive family photo sync |
| **8** | Optionally store bill, price details | **100% Complete** ✅ | • `billAmount: Double` in `VisitEntryEntity`<br>• `price: Double` in `DishFeedbackEntity`<br>• Bill amount input in `EntryEditorScreen`<br>• Bill amount display in `VisitHistoryCard` (`RestaurantDetailScreen`) |
| **9** | Store multiple visits to same restaurant for aggregated metrics | **100% Complete** ✅ | • `RestaurantEntity` 1-to-many `VisitEntryEntity`<br>• Total visits counter on `RestaurantCard`<br>• Visit history timeline on `RestaurantDetailScreen`<br>• Aggregated Dish Matrix showing cumulative verdicts across multiple visits |
| **10** | Work offline (without internet access), two-way sync periodically or by user trigger | **100% Complete** ✅ | • 100% offline-first architecture via local Room SQLite DB<br>• `SyncWorker` (WorkManager periodic background sync)<br>• "Sync Now" button in `SettingsScreen`<br>• OpenStreetMap offline tile caching |
| **11** | Non-intrusive smart notifications (sync health, family updates) | **100% Complete** ✅ | • `PalateNotificationManager` (`channel_sync_health`, `channel_family_sync`)<br>• Actionable OAuth & folder permission error alerts (HTTP 401/403)<br>• Silent family sync delta digest in `SyncRepository`<br>• `POST_NOTIFICATIONS` runtime permission on Android 13+ |
| **UI 1** | Minimalist UI during data entry & pre-filling | **100% Complete** ✅ | • **3-Second Quick Regret Logger**: 1-tap GPS, 1-tap NO-GO verdict, quick tag chips ("Watery tea", "Soggy food", "Dirty restrooms"). Saves in 3s.<br>• **Pre-filling**: Selecting an existing restaurant in `EntryEditorScreen` auto-populates past dishes as instant selectable chips with past verdicts! |
| **UI 2** | Map View with custom markers & navigation | **100% Complete** ✅ | • Interactive OpenStreetMap (OSMDroid) with custom color-coded pins (🟢 Go, 🔴 No-Go, 🟡 Caution)<br>• Bottom sheet drawer on pin tap with "Navigate in Google Maps" handoff |

---

## 2. Prioritized Roadmap & Completed Milestones

Based on the gap analysis, here are the completed phases and recommended next steps in order of priority:

### Phase 2A: UX Polish & Enhanced Querying (Completed ✅)
1. **Dish-Aware Unified Search (Completed ✅)**:
   - Tokenized search across restaurant metadata and dish names, ranking exact dish matches at top with matched dish chips on `RestaurantCard`.
2. **Camera & PhotoPicker Integration (Completed ✅)**:
   - 1-tap gallery and camera photo attachments for bill receipts and dish photos with internal storage persistence, thumbnail galleries, fullscreen viewer, and 2-way Google Drive photo sync.

### Phase 2B: Google Drive Direct Cloud Sync (Completed ✅)
3. **Dual-Mode Family Google Drive Sync (Completed ✅)**:
   - Single Shared Account (Mode A) and Individual Accounts with automated in-app invitations (Mode B).
   - Static QR code / WhatsApp link sharing (`palatelog://sync?folderId=<folderId>`).

### Phase 2C: Code Hygiene, Design System & In-App Updates (Completed ✅)
4. **In-App Google Play Updates (Completed ✅)**:
   - Flexible updates, background progress tracking, loop-safe `onResume` status checks, and explicit restart prompt dialogs (`InAppUpdateHelper`).
5. **Design System Tokens & Non-Transitive R (`Dimens.kt`) (Completed ✅)**:
   - Created `Dimens.kt` for spacing, corner radii, icon sizes, and button heights.
   - Enabled `android.nonTransitiveRClass=true` in `gradle.properties`.

### Phase 2D: User Onboarding, Localization & Navigation Polish (Completed ✅)
6. **Interactive Help & User Guide (`HelpGuideScreen`) (Completed ✅)**:
   - Material 3 user guidebook with category filter chips, onboarding welcome dialog, and type-safe `HelpTopicKey` deep-linking shortcuts across screens (`?` buttons).
7. **Comprehensive UI Localization & Hardcoded String Extraction (Completed ✅)**:
   - Extracted 80+ hardcoded Compose UI strings into `res/values/strings.xml` for internationalization (i18n) and TalkBack accessibility.
8. **Dark Theme High-Contrast Polish & Layout Fixes (Completed ✅)**:
   - Fixed text contrast in Dark Theme tip callouts and eliminated awkward button text wrapping ("Navigate") in map preview cards.
9. **Content-Aware Floating Scroll Indicators (Completed ✅)**:
   - Auto-fading circular down-arrow indicators on Explore and Restaurant Detail screens with 1-tap scroll down and bottom navigation bar inset protection.

### Phase 3A: Non-Intrusive Notifications & Sync Health (Completed ✅)
10. **Actionable Sync & Auth Health Alerts (Completed ✅)**:
    - High-utility, zero-noise notifications dispatched **strictly** for critical actionable errors (Google Drive OAuth token expiration or shared folder authorization revocation `HTTP 401/403`).
    - Standard offline conditions and transient network dropouts remain completely silent.
    - Automatically clears via `clearSyncAuthAlert()` upon successful sync recovery.
11. **Family Sync Delta Digest (Completed ✅)**:
    - Low-priority, batched summary notification when background `SyncWorker` fetches new dining logs or dish warnings from family members (e.g. *"Priya added a review for Nagarjuna: Must Try Biriyani, Avoid Chicken Fry"*).
    - Deep-link intent routing from notifications directly to Settings (for auth) or Explore (for digests).

### Phase 3B: Highway Co-pilot & On-Demand Dynamic Geofencing (Upcoming)
12. **User-Controlled Highway Co-pilot (Quick Settings Tile & In-App Toggle)**:
    - Dedicated Quick Settings Tile (`HighwayTileService`) allowing 1-tap activation when starting a highway road trip.
    - Zero background tracking or battery drain when disabled; avoids perpetual 24/7 background location permission friction.
13. **Dynamic 500 km Corridor Geofencing**:
    - On Co-pilot activation, dynamically registers up to 100 geofences with Google Play Services `GeofencingClient` for saved highway stops within a 500 km radius.
    - **Mandatory Default**: High-priority "Roadside Regret Ahead" proximity warning (500m prior) for `NO_GO` spots (e.g. "Terrible watery tea, dirty restrooms").
    - **Customizable Preference**: Opt-in toggle to also alert for `GO` recommendations (e.g. "Clean Restrooms & Filter Coffee in 500m").
    - **Clean Teardown**: Immediate geofence de-registration upon toggling Co-pilot OFF.

#### Phase 3C: Travel Corridor Search & Planning
14. **Highway Route Corridor Search**:
    - Allow entering an Origin and Destination (e.g. Bangalore to Chennai) to query all logged stops within 5-10 km along that highway corridor.

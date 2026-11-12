# Privacy Policy for PalateLog

**Application ID:** `com.prismpixelvertex.palatelog`  
**Effective Date:** November 12, 2026  
**Developer:** Karthikeyan S  
**GitHub Repository:** [https://github.com/karthikeyan-s/palatelog](https://github.com/karthikeyan-s/palatelog)  
**Email Support:** prismpixelvertex@gmail.com  

PalateLog is designed with local data sovereignty and offline-first privacy. We believe your dining records, personal dish reactions, roadside tea stop regrets, and family notes belong entirely to you. PalateLog does not operate central servers, does not run user-tracking databases, and contains zero advertising or telemetry trackers.

---

## 1. Information We Do NOT Collect
- **No Central Server Storage:** PalateLog does not operate remote servers or databases collecting your reviews, bill amounts, dish preferences, or photos.
- **No Analytics Trackers:** PalateLog does not include third-party behavioral analytics (such as Firebase Analytics, Adjust, AppsFlyer, or Facebook SDK).
- **No Advertisements or Advertising ID:** We do not serve advertisements, monetize user data, or request the Android Advertising ID (`AD_ID`).
- **No Third-Party Data Sharing:** We do not sell, rent, broker, or transmit user information to data brokers, marketing agencies, or advertisers.

---

## 2. Device Permissions & Data Practices

### A. Location (Approximate & Precise Location)
PalateLog requests location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, and `FOREGROUND_SERVICE_LOCATION`) for two user-facing capabilities:
1. **Local Geotagging & Proximity Sorting:** Suggesting nearby area names when logging a dining spot and computing road distances on the Explore map.
2. **Palate Radar (On-Demand Roadside Dining Assistant):** When users embark on highway road trips, they can activate Palate Radar via an in-app toggle or a Quick Settings shade tile. Palate Radar registers Google Play Services Geofences for up to 100 user-saved highway stops within a 500 km radius and sounds a high-priority heads-up warning 500 meters before approaching previously logged roadside regrets (e.g. bad tea stalls, terrible food, or unhygienic restrooms).

**Strict Privacy Safeguards on Location:**
- **100% On-Device & Ephemeral:** GPS coordinates are queried exclusively in device memory against your local SQLite database and Play Services Geofencing. Location coordinates are never sent off your phone.
- **Zero Continuous Tracking:** Palate Radar operates only during an active user-initiated trip. It does not perform perpetual 24/7 background tracking. When stopped, geofences are immediately deleted and location monitoring ceases.
- **Clear Disclosure:** An explicit in-app prominent disclosure dialog is presented to the user before Palate Radar is activated.

### B. Camera & Storage
PalateLog requests camera access (`CAMERA`) via Android Jetpack CameraX to allow users to take photos of dishes and bill receipts directly within the app.
- **Private Internal Storage:** Captured photos are saved solely to PalateLog's private, app-scoped internal directory (`context.filesDir`).
- **No Gallery Scanning:** The app does not scan, index, or access your broader device photo library.

### C. Notifications
On Android 13+ (API 33+), PalateLog requests notification permissions (`POST_NOTIFICATIONS`) strictly for essential utility alerts:
- **Radar Alerts:** High-priority proximity warnings when approaching saved roadside regrets on highways.
- **Ongoing Trip Indicator:** A persistent foreground service notification showing active radar status with a 1-tap "Turn Off" button.
- **Family Sync Digest:** Silent background notifications when new dining entries or dish reviews are synchronized from family members.

### D. Optional Zero-Cost Cloud Sync (Google Drive)
PalateLog is fully functional 100% offline without any account or login. If users choose to enable the optional family cloud sync:
- The app authenticates directly with Google Play Services using the user's own Google account.
- Synchronization payloads (compact JSON deltas and photos) are written directly to a user-owned `PalateLog_Family/` folder in Google Drive via HTTPS.
- No developer servers or third-party intermediaries ever receive or store your credentials, authorization tokens, or dining files.

---

## 3. Google Play Data Safety Summary

| Data Type | Collected? | Shared with Third Parties? | Ephemeral Processing? | Required or Optional? | Purpose |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Location** (Approximate & Precise) | **Yes** | **No** | **Yes** (Memory only) | **Optional** | App functionality (Nearby spot suggestions & Palate Radar 500m highway alerts) |
| **Photos** | **Yes** | **No** | **No** (Saved to local private storage) | **Optional** | App functionality (Dish reviews & bill receipts) |
| **Personal Info** (Email Address) | **Yes** | **No** | **No** (Local account profile only) | **Optional** | Account management (Optional Google Drive Family Sync) |

---

## 4. Data Retention, User Control, and Deletion
Because PalateLog follows a local-first architecture, users have sovereign control over their records at all times:
- **Individual Record Deletion:** Deleting a restaurant, visit entry, dish verdict, or photo immediately purges it from your local Room database. If sync is enabled, a tombstone record safely deletes it from family devices.
- **Full Local Reset:** Uninstalling PalateLog or clearing app storage from Android Settings immediately and permanently wipes all local database records, preferences, and photos from your device.
- **Cloud Sync Removal:** If you use Google Drive sync, you can delete the `PalateLog_Family/` folder from your Google Drive or revoke PalateLog's access anytime via [Google Account Permissions](https://myaccount.google.com/permissions).

---

## 5. Security Practices
- **Encryption in Transit:** All communications between the app and external services (OpenStreetMap/Mappls map tile requests and Google Drive REST API calls) are strictly enforced over encrypted HTTPS (TLS 1.2+).
- **Sandboxed Internal Storage:** App database records and captured photos are stored inside Android's sandboxed internal storage, inaccessible to other applications installed on the device.

---

## 6. Children's Privacy
PalateLog is designed for general audiences aged 18 and older. We do not knowingly collect or solicit personal information from children under the age of 13.

---

## 7. Changes to This Policy
We may periodically update this Privacy Policy to reflect application improvements or regulatory requirements. Any updates will be posted directly to this URL with a revised effective date.

---

## 8. Contact Information
If you have any questions, feedback, or data privacy inquiries regarding PalateLog, please contact:  
- **Developer:** Karthikeyan S  
- **GitHub Repository:** [https://github.com/karthikeyan-s/palatelog](https://github.com/karthikeyan-s/palatelog)  
- **Email Support:** prismpixelvertex@gmail.com  

# Google Play Store Justification & Declaration Package: Palate Radar

> **Application ID**: `com.prismpixelvertex.palatelog`  
> **Feature**: Palate Radar (On-Demand Roadside Dining Proximity Alerts)  
> **Permissions**: `FOREGROUND_SERVICE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS`  
> **API Level Compliance**: Android 14+ (API 34/35/36)

This document provides ready-to-use declaration texts, policy justifications, in-app prominent disclosure copy, and reviewer video scripts required when submitting PalateLog to the Google Play Store Console.

---

## 1. Google Play Console Declaration Form: Core Feature Justification

### A. Core Feature Name
**Palate Radar (On-Demand Roadside Dining Proximity Alerts)**

### B. Purpose of the Feature & User Benefit
> "PalateLog is an offline-first personal dining journal. Its core capability, **Palate Radar**, is a user-initiated driving assistant that warns motorists 500 meters before approaching previously logged roadside dining regrets (e.g., terrible highway stalls with bad food or dirty restrooms) or trusted stops along national highway corridors.
> 
> Because users are actively driving on highways at speeds of 80–100 km/h, they cannot safely interact with their phone or browse a map. Palate Radar runs as an on-demand trip session using an Android Location Foreground Service and Google Play Services Geofencing to monitor up to 100 user-saved highway spots within a 500 km radius. When the vehicle enters a 500m geofence perimeter, a high-priority heads-up audio/vibration notification alerts the driver in time to safely pass or take the highway exit.
> 
> The feature operates strictly on-demand (toggled ON/OFF via a Quick Settings Tile or in-app switch) and provides a persistent foreground notification with a 1-tap 'Turn Off' button. It does not perform perpetual or 24/7 background tracking. When turned off, all geofences are immediately deregistered and location monitoring terminates completely."

### C. Why Location Cannot be Replaced by Foreground-Only or Non-Location APIs
> "Drivers lock their screens, mount their phones, or use turn-by-turn navigation apps (e.g., Google Maps) while driving. If Palate Radar was restricted to foreground-only interaction when the app is actively on-screen, the driver would miss critical proximity warnings whenever the phone screen sleeps or when another navigation app is in the foreground, directly defeating the safety and regret-prevention utility of the app."

---

## 2. In-App Prominent Disclosure & Consent Dialog Specification

Google Play policy mandates that apps accessing location in the background or via a location foreground service display a **Prominent In-App Disclosure** before the user triggers the feature or grants permissions:

- **Dialog Title**: `📡 Turn On Palate Radar?`
- **Icon**: Radar / Location pin
- **Body Content**:
  > "Palate Radar accesses your device location while driving—including when the screen is locked or while using other navigation apps—to monitor up to 100 saved highway stops within 500 km and alert you 500 meters before you reach them.
  > 
  > • **100% On-Device**: Your GPS coordinates are checked strictly against your local private database and Google Play Services Geofencing.  
  > • **Zero Tracking**: Your location is never logged, stored, or sent to any server or third party.  
  > • **You Control It**: Active only during your trip. You can stop Palate Radar at any time via the Quick Settings Tile or the persistent notification."
- **Action Buttons**:
  - `[Turn On Radar]` (Proceeds to activate service & geofences)
  - `[Not Now]` (Dismisses dialog, leaves radar disabled)

---

## 3. Google Play Data Safety Form Declarations

When completing the **Data safety** questionnaire in the Play Console:

| Question | Answer | Rationale / Explanation |
| :--- | :---: | :--- |
| **Does your app collect or share location data?** | **Collected (Ephemeral)** | Location is accessed in memory to register geofences and query local Room SQLite; it is never transmitted off the device. |
| **Approximate location collected?** | **Yes** | Used to calculate the 500 km corridor bounding box. |
| **Precise location collected?** | **Yes** | Required for accurate 500m highway geofencing. |
| **Is this data shared with third parties?** | **No** | Zero location data leaves the device. |
| **Is this data processed ephemerally?** | **Yes** | Coordinates are used at runtime and immediately discarded; they are not saved or uploaded. |
| **Is this data required or optional?** | **Optional** | Users can use PalateLog purely as a dining journal without ever turning on Palate Radar. |
| **Purposes of collection** | **App functionality** | Proximity alerts for user-saved dining spots. |

---

## 4. Google Play Reviewer Video Walkthrough Script

Google Play requires a short YouTube / cloud video link demonstrating the feature. Use this step-by-step recording checklist:

1. **Step 1 (Open App)**: Launch PalateLog on a test device showing the Explore dashboard with saved restaurant cards.
2. **Step 2 (Trigger Radar)**: Tap the **"Start Palate Radar"** card on the dashboard or pull down the notification shade and tap the **"Palate Radar" Quick Settings Tile**.
3. **Step 3 (Prominent Disclosure)**: Show the in-app disclosure dialog clearly explaining location usage, and tap **"Turn On Radar"**.
4. **Step 4 (Persistent Notification)**: Show the ongoing foreground notification in the notification shade (*"📡 Palate Radar Active: Scanning X stops within 500 km"*).
5. **Step 5 (Lock Screen / Background Transition)**: Lock the phone or switch to Google Maps to prove background operation.
6. **Step 6 (Trigger Proximity Alert)**: Simulate vehicle entering a 500m geofence (via Android Studio emulator Location tab or road test) and show the high-priority heads-up notification (*"🔴 Roadside Regret Ahead (500m): Sri Krishna Dhaba"*).
7. **Step 7 (Clean Teardown)**: Tap **"Turn Off"** on the notification or tap the Quick Settings Tile again, demonstrating immediate notification removal and complete teardown.

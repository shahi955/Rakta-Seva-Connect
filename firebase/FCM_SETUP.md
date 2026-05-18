# Rakta-Seva Connect — Firebase Cloud Messaging (FCM) setup

This project uses **FCM + Firestore + Cloud Functions** so emergency alerts reach matched donors with **high priority**, while the app opens **request details** when the user taps the notification.

---

## 1. Firebase Console (project)

1. Open [Firebase Console](https://console.firebase.google.com/) → your project.
2. **Build → Cloud Messaging** — no extra toggle; ensure the Android app is registered with the same `applicationId` as `google-services.json`.
3. **Project settings → Cloud Messaging** — for HTTP v1, Google manages keys; **Cloud Functions** use the default service account (no server key in the Android app).

---

## 2. Android app configuration

### Gradle

- `com.google.gms.google-services` on the `app` module.
- Dependencies include **`firebase-messaging-ktx`** (see `app/build.gradle.kts`).

### Manifest

- `POST_NOTIFICATIONS` (Android 13+): request at runtime before expecting heads-up alerts.
- `RaktaFcmService` registered with `com.google.firebase.MESSAGING_EVENT`.
- Optional meta-data (already in `AndroidManifest.xml`):
  - `com.google.firebase.messaging.default_notification_channel_id` = `raktaseva_emergency`
  - `com.google.firebase.messaging.default_notification_icon` = `@drawable/ic_notification`

### `MainActivity`

- `android:launchMode="singleTop"` so a tap delivers `onNewIntent` and extras are merged.
- `ingestNotificationLaunchIntent` reads `request_id` / `requestId` / `relatedRequestId` via `RaktaNotificationHelper.readRequestIdFromExtras`.
- Pending navigation is stored on **`RaktaApplication.pendingOpenRequestId`** (survives activity recreation within the process).
- **`RaktaNavHost`** observes that id and navigates to **`request_detail/{requestId}`**, then calls **`consumePendingOpenRequest()`**.

### Client code map

| Piece | Role |
|--------|------|
| `service/RaktaFcmService` | `onNewToken` → Firestore `mergeFcmToken`; `onMessageReceived` → high-priority local notification via `EmergencyNotificationHelper` |
| `service/EmergencyNotificationHelper` | Channel `IMPORTANCE_HIGH`, `PRIORITY_MAX`, tap `PendingIntent` → `MainActivity` + `request_id` |
| `notification/RaktaNotificationHelper` | Intent extras + `PendingIntent` builders |
| `fcm/FcmTokenManager` | After login / on Home: `getToken()` + `UsersRepository.mergeFcmToken` (keeps last 5 tokens) |
| `repository/UsersRepository.mergeFcmToken` | Transaction: append token, trim to 5 |
| `ui/request/RequestDetailScreen` | Loads `blood_requests/{requestId}` |

---

## 3. Why Cloud Functions (recommended)

**Do not** send FCM from the Android app using the legacy server key or a service account JSON (secret would ship in the APK).

Flow used here:

1. App creates **`notifications`** documents for each matched donor (already implemented).
2. **Cloud Function** `sendEmergencyFcmOnNotificationCreate` runs on `notifications/{docId}` **onCreate**.
3. Function loads `users/{userId}.fcmTokens` and calls **`admin.messaging().sendEachForMulticast`** with:
   - `android.priority = "high"`
   - `android.notification.channelId = "raktaseva_emergency"`
   - `data.requestId` for deep link

---

## 4. Deploy Cloud Functions

From the repo root **`RaktaSevaConnect/`** (where `firebase.json` lives):

```bash
cd firebase/functions
npm install
npm run build
cd ../..
firebase login
firebase use <your-project-id>
firebase deploy --only functions
```

First-time Functions setup:

```bash
npm install -g firebase-tools
firebase init functions
```

(If you already have `firebase/functions` from this repo, `npm install` + `deploy` is enough.)

### Blaze plan

Cloud Functions require the **Blaze** billing plan for outbound network to FCM. The free Spark plan cannot call FCM from Functions.

---

## 5. Firestore rules (notifications)

Rules must allow:

- Donors to **read** their own notifications.
- Request creators to **create** `BLOOD_REQUEST` notifications that reference a `blood_requests` doc they own (see `firebase/firestore.rules` in this repo).

Deploy:

```bash
firebase deploy --only firestore:rules
```

---

## 6. Behaviour matrix

| App state | Message type | What happens |
|-----------|----------------|---------------|
| Foreground | Data + notification | `onMessageReceived` runs → `EmergencyNotificationHelper` shows high-priority local notification |
| Background | Notification + data | System tray; tap → `MainActivity` extras → navigate to request detail |
| Killed | Same | Tap launches app → `onCreate` reads extras → navigate |

If `onMessageReceived` does not run in background when only a **notification** payload is sent, the system still shows the tray notification; **data** keys are still delivered to the Activity intent on tap (FCM behaviour).

---

## 7. Testing checklist

1. Install app, sign in, grant **notifications** + **location** as needed.
2. Confirm `fcmTokens` appears on `users/{uid}` in Firestore after opening Home.
3. Post an emergency request that matches a second test device user (donor with same blood group, coords, etc.).
4. Confirm a `notifications` row is created for that user, then the **Function log** shows multicast success.
5. Lock device → verify heads-up alert and tap → **Blood request** screen shows the correct `blood_requests` document.

---

## 8. “Direct” approach (not recommended)

Sending from a **trusted backend** (your server with a service account) is fine. Sending from **only the client** without a server is not supported securely. This repo standardises on **Firestore + Functions**.

---

## 9. Troubleshooting

| Issue | What to check |
|--------|----------------|
| No push, Function runs | `fcmTokens` empty; token sync after login; correct Firebase project |
| `PERMISSION_DENIED` on mergeFcmToken | Firestore rules for `users/{uid}` |
| No tray in foreground | Expected; app shows its own notification from `onMessageReceived` |
| Tap does not open detail | `requestId` in `data`; `MainActivity` `singleTop` + `readRequestIdFromExtras` |
| Channel not high importance | Reinstall app or bump channel id after changing importance (Android caches channels) |

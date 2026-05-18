# Viva / interview — questions and answers

## 1. What does Rakta-Seva Connect do?

It is an Android app for emergency blood assistance: users register as donors, see a dashboard of their activity and alerts, find nearby compatible donors using GPS, post blood requests to Firestore, and receive FCM notifications that deep-link to request details.

## 2. Why Jetpack Compose instead of XML layouts?

Compose reduces boilerplate, makes state-driven UI explicit, and pairs naturally with `StateFlow` from ViewModels. The same codebase is easier to test and refactor than imperative `findViewById` trees.

## 3. Explain MVVM in this project.

The **View** is a Composable function. The **ViewModel** holds `MutableStateFlow` / `StateFlow` for UI state and runs coroutines in `viewModelScope`. The **Model** side is implemented as **Repositories** wrapping Firebase APIs. UI never calls Firestore directly.

## 4. How is navigation handled?

A single `NavHost` (`RaktaNavHost`) defines routes for login, register, home (main shell), admin login/home, and request detail with a `requestId` argument. `rememberNavController()` drives back stack and deep links.

## 5. How do notification taps open a specific request?

`RaktaNotificationHelper` builds a `PendingIntent` into `MainActivity` with extras containing `requestId`. `MainActivity` forwards the id to `RaktaApplication.enqueueOpenRequestFromNotification`. Compose reads `pendingOpenRequestId` and `RaktaNavHost` navigates to `request_detail/{id}` then clears the pending value.

## 6. What Firebase products are used?

- **Authentication** (email/password)
- **Cloud Firestore** (profiles, requests, donations, notifications)
- **Cloud Messaging** (push + on-device display in `FirebaseMessagingService`)

## 7. How are Firestore security rules enforced?

Rules live in `firebase/firestore.rules`. Users can generally read other profiles (for donor discovery) but only mutate their own document unless they are **admins** (`role == 'admin'`). Blood requests are creatable only with `createdBy == auth.uid`; admins can delete bad data. Donations are readable by donor or admin.

## 8. Why `collectAsStateWithLifecycle`?

It ties Flow collection to the lifecycle (default `minActiveState = STARTED`), so when the app is in the background the UI stops collecting, saving CPU and avoiding updates to detached UI.

## 9. What crash risks did you mitigate?

- Blank `requestId` for detail: `RequestDetailViewModel` short-circuits instead of querying Firestore with an empty id.
- **NotificationManager** cast with correct imports and permission guard on API 33+.
- **Location** token cancellation in `finally` to avoid leaking `CancellationTokenSource`.
- **Main thread**: FCM token fetch and Firestore merge moved to `Dispatchers.IO` inside `FcmTokenManager`.

## 10. Memory leak considerations

- `viewModelScope` cancels work when the ViewModel is cleared.
- `RaktaApplication.applicationScope` is cancelled in `onTerminate` (best-effort).
- Snapshot listeners in repositories should use `awaitClose` in `callbackFlow` (pattern used in donor flows) so listeners unregister when collectors disappear.

## 11. How does nearby donor search work?

Firestore returns donor candidates by blood group and availability flags. Kotlin filters enforce **10 km** radius and **90-day** post-donation cooldown because Firestore cannot express Haversine distance natively.

## 12. What would you improve for scale?

Server-side geohash queries, paginated lists, Cloud Functions for notification fan-out only (client never writes other users’ notification docs except controlled cases), and stricter admin update rules (field-level diffs).

## 13. Why is `google-services.json` a placeholder in the repo?

Real files contain project secrets and identifiers. Each developer or CI pipeline must use their own Firebase project file and must not commit production keys to public source control.

## 14. How do admins get the `admin` role?

Only via Firebase Console or Admin SDK by setting `role: "admin"` on `users/{uid}`. Registration always writes `user` to prevent privilege escalation from the client.

## 15. What is `AppGraph`?

A small **service locator** (`object`) lazily constructing repositories. It avoids Hilt setup for a coursework-sized app but documents where to swap in dependency injection later.

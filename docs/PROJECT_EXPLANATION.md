# Rakta-Seva Connect — project explanation

## Problem statement

Hospitals and families often need compatible donors quickly. Rakta-Seva Connect gives registered users a single place to declare donor availability, search for nearby donors filtered by blood group and distance, post urgent requests to Firestore, and receive high-priority alerts when others need help.

## User journeys

### Donor / responder (default)

1. **Register** creates a Firebase Auth user and merges a `users/{uid}` profile (blood group, city, donor flag, availability).
2. **Main shell** (bottom navigation) hosts:
   - **Home**: dashboard cards—open requests the user created, recent `BLOOD_REQUEST` notifications, donation cooldown summary, availability toggle, shortcuts to donors and emergency flow.
   - **Donors**: blood group filter, runtime location permission, Firestore query for available donors, client-side radius and 90-day cooldown filtering.
   - **Emergency**: validated form, GPS capture, repository writes `blood_requests` and fan-out notifications to matched donors.
   - **Profile**: sign-out; admins see a link to the admin console.

3. **Request detail** opens from navigation or from a **notification tap**: `MainActivity` reads extras, `RaktaApplication` exposes a pending request id as `StateFlow`, and `RaktaNavHost` navigates when the graph is ready.

### Administrator

- Accounts with `users/{uid}.role = "admin"` (set in Console) can use **Administrator sign-in** or the profile shortcut.
- Admin console lists recent users (block/unblock), blood requests (delete abusive entries), and donation rows for aggregate statistics—backed by extended Firestore rules.

## Data flow (simplified)

```
Compose UI → ViewModel (StateFlow) → Repository → Firestore / Auth / FCM
                      ↑
               FcmTokenManager (app scope, IO)
```

- **MVVM**: screens collect `uiState` with `collectAsStateWithLifecycle()` so collectors pause when the lifecycle is not at least `STARTED`, reducing wasted work and stale UI after configuration changes.
- **Errors**: repositories throw; ViewModels map failures with `Throwable.toUserMessage()` for consistent copy.

## Notifications

- FCM delivers data payloads; `RaktaFcmService` builds a local high-importance notification via `EmergencyNotificationHelper`.
- On **Android 13+**, `POST_NOTIFICATIONS` must be granted: `MainShell` launches the permission request once the user reaches the authenticated experience, and `EmergencyNotificationHelper` no-ops if the permission is still denied (avoids silent failures or inconsistent behaviour).

## Location

`LocationHelper` uses the fused location provider on the **main** dispatcher (Play Services requirement), cancels the `CancellationTokenSource` after the call, and falls back to `lastLocation` when a fresh fix is unavailable.

## Extensibility

- Replace `AppGraph` with Hilt or Koin when the project grows.
- Move fan-out and abuse prevention fully to **Cloud Functions** for stronger guarantees than client-only checks.
- Add pagination (`startAfter`) for admin and dashboard lists when collections grow.

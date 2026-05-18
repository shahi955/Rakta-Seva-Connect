# Resume — Rakta-Seva Connect (copy-ready)

## One-line pitch

Android (Kotlin) blood-donor connectivity app with Firebase Auth/Firestore/FCM, Jetpack Compose UI, MVVM, and location-aware donor matching for emergency requests.

## Short paragraph (resume / LinkedIn)

Built **Rakta-Seva Connect**, an Android application for emergency blood assistance using **Kotlin**, **Jetpack Compose**, and **Material 3**. Implemented **MVVM** with `StateFlow`, repository layer on **Firebase Auth**, **Cloud Firestore**, and **FCM**, including deep-linked notification handling into request detail screens. Integrated **Google Play Services location** for nearby donor discovery with client-side geo and eligibility filtering, an emergency request workflow with donor notifications, and an **admin moderation** surface backed by Firestore security rules. Applied lifecycle-aware UI (`collectAsStateWithLifecycle`), runtime notification permission on Android 13+, and structured Firebase deployment documentation (rules, indexes, schema).

## Technical keywords

Kotlin · Android SDK 26–35 · Jetpack Compose · Material 3 · Navigation Compose · MVVM · StateFlow · Coroutines · Firebase Auth · Cloud Firestore · FCM · Play Services Location · Cloud Functions (repo sample) · Firestore security rules

## Project bullets (two-column friendly)

- Architected **single-activity Compose** navigation with authenticated **bottom-nav shell** (dashboard, donors, emergency, profile) and stack routes for auth, admin, and deep links.
- Delivered **Firestore-backed** features: user profiles, blood requests, donations ledger, in-app notifications; extended **security rules** for admin moderation.
- Improved **reliability**: IO dispatcher for token sync, lifecycle-bound state collection, notification permission flow, and defensive handling of invalid deep-link IDs.

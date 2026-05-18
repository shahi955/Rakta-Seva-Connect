# Rakta-Seva Connect 🩸

Rakta-Seva Connect is a modern Android application designed to bridge the gap between blood donors and emergency blood requests in real-time. Built with **Kotlin** and **Jetpack Compose**, it leverages the **Firebase** ecosystem to provide a fast, secure, and location-aware platform for life-saving coordination.

## 🚀 Key Features

*   **Real-time Nearby Donor Discovery**: Uses GPS to find eligible donors within a 10km radius of the requester.
*   **Emergency Blood Requests**: Quickly post requests with patient details, hospital location, and urgency levels.
*   **Instant Push Notifications**: Powered by Firebase Cloud Messaging (FCM) to alert nearby matching donors immediately.
*   **Smart Eligibility Filtering**: Automatically hides donors who have donated within the last 90 days.
*   **Comprehensive User Profiles**: Detailed donor profiles including blood group, location linking, and availability toggles.
*   **Admin Console**: Built-in tools for moderation, user management, and tracking requests.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Navigation**: Jetpack Navigation Compose
*   **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
*   **Database**: Google Cloud Firestore (NoSQL)
*   **Authentication**: Firebase Auth (Email/Password)
*   **Backend Logic**: Firebase Cloud Functions (TypeScript)
*   **Notifications**: Firebase Cloud Messaging (FCM)
*   **Location Services**: Google Play Services Location

## 📦 Project Structure

```text
├── app/                  # Main Android module (Kotlin/Compose)
├── firebase/             # Firebase configuration and logic
│   ├── functions/        # TypeScript Cloud Functions for FCM
│   ├── sample-documents/ # JSON data for initial setup
│   └── firestore.rules   # Security rules for database
├── docs/                 # Documentation and internship reports
└── gradle/               # Build configuration
```

## 🛠️ Setup Instructions

### Prerequisites
*   **Android Studio** Koala or newer
*   **JDK 17**
*   A **Firebase Project** with Firestore, Auth, and Cloud Messaging enabled.

### Quick Start
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/shahi955/Rakta-Seva-Connect.git
    ```
2.  **Add Firebase**:
    *   Download your `google-services.json` from the Firebase Console.
    *   Place it in the `app/` directory.
3.  **Deploy Firebase Logic**:
    *   Deploy Firestore rules: `firebase deploy --only firestore:rules`
    *   (Optional) Deploy Cloud Functions: `cd firebase/functions && npm install && firebase deploy --only functions`
4.  **Run the app**:
    *   Sync Gradle in Android Studio.
    *   Click **Run** (`Shift + F10`) to deploy to your device/emulator.

## 🛡️ Permissions
The app requires the following permissions for full functionality:
*   `ACCESS_FINE_LOCATION`: Required to find donors near you.
*   `POST_NOTIFICATIONS`: Required to receive emergency alerts (Android 13+).
*   `INTERNET`: Required for Firebase data sync.

## 📄 Documentation
*   [Detailed Project Explanation](docs/PROJECT_EXPLANATION.md)
*   [Firestore Schema](firebase/FIRESTORE_SCHEMA.md)
*   [FCM Setup Guide](firebase/FCM_SETUP.md)

---
**Disclaimer**: This project was developed as part of a healthcare logistics initiative to streamline voluntary blood donation.

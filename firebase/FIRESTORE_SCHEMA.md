# Rakta-Seva Connect — Firestore schema

## Collections overview

| Collection | Document ID | Purpose |
|------------|-------------|---------|
| `users` | Firebase Auth `uid` | Profile, donor flags, location, FCM tokens |
| `blood_requests` | Auto-ID | Emergency blood requests |
| `donations` | Auto-ID | Donation history / audit |
| `notifications` | Auto-ID | In-app notification log (FCM companion) |

---

## 1. `users/{uid}`

**Donor-capable users** store matching fields on the same document (no separate `donors` collection required).

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `displayName` | string | ✓ | |
| `email` | string | ✓ | |
| `phone` | string | ✓ | Digits or E.164 |
| `role` | string | ✓ | `user`, `hospital`, `admin` |
| `isDonor` | bool | | Default `false` |
| `isBlocked` | bool | | Default `false` |
| `bloodGroup` | string | if donor | `A+`, `O-`, … |
| `gender` | string | | |
| `age` | number | | |
| `city` | string | | |
| `taluk` | string | | |
| `lastDonationDate` | timestamp | | `null` if never donated |
| `availabilityStatus` | string | if donor | `AVAILABLE`, `BUSY`, `UNAVAILABLE` |
| `latitude` | number | if donor | WGS84 |
| `longitude` | number | if donor | WGS84 |
| `geohash` | string | optional | For future server-side geo indexing |
| `fcmTokens` | array&lt;string&gt; | | Cap length in app |
| `createdAt` | timestamp | ✓ | |
| `updatedAt` | timestamp | | |

**Indexes (suggested):** `isDonor` + `bloodGroup` + `availabilityStatus`; `city`; `isBlocked`.

---

## 2. `blood_requests/{requestId}`

| Field | Type | Notes |
|-------|------|--------|
| `createdBy` | string | Auth uid |
| `patientName` | string | |
| `bloodGroupNeeded` | string | |
| `unitsRequired` | number | |
| `hospitalName` | string | |
| `latitude` | number | |
| `longitude` | number | |
| `geohash` | string | optional |
| `contactNumber` | string | |
| `emergencyLevel` | string | e.g. `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `notes` | string | |
| `status` | string | `OPEN`, `FULFILLED`, `CANCELLED` |
| `createdAt` | timestamp | |
| `updatedAt` | timestamp | |

**Indexes:** `status` + `bloodGroupNeeded`; `createdBy` + `createdAt` desc.

---

## 3. `donations/{donationId}`

| Field | Type | Notes |
|-------|------|--------|
| `donorId` | string | uid |
| `requestId` | string | nullable if walk-in |
| `units` | number | |
| `donationDate` | timestamp | When blood was drawn |
| `hospitalName` | string | optional |
| `notes` | string | optional |
| `verified` | bool | Staff verified |
| `createdAt` | timestamp | Record creation |

**Indexes:** `donorId` + `donationDate` desc.

---

## 4. `notifications/{notificationId}`

| Field | Type | Notes |
|-------|------|--------|
| `userId` | string | Recipient uid |
| `title` | string | |
| `body` | string | |
| `type` | string | `BLOOD_REQUEST`, `SYSTEM`, … |
| `read` | bool | Default `false` |
| `payload` | map | Deep-link keys, requestId, etc. |
| `relatedRequestId` | string | optional |
| `createdAt` | timestamp | |

**Indexes:** `userId` + `read` + `createdAt` desc.

---

## Geo + 90-day rules (client-side)

Firestore has no native radius query. Recommended flow:

1. **Query** donors: `isDonor == true`, `bloodGroup == needed`, `availabilityStatus == AVAILABLE`, `isBlocked != true` (model `isBlocked` explicitly).
2. **Filter** in Kotlin with `DonorFilter.excludeRecentDonationsWithin90Days`.
3. **Filter** with `GeoUtils.filterWithinRadiusKm` (Haversine) using donor `latitude`/`longitude` vs request point.

For large scale, add **geohash** + Cloud Functions to precompute matches.

---

## Admin operators (in-app console)

Trusted accounts have `users/{uid}.role` set to **`admin`** in the Firebase Console or via Admin SDK (the app registration flow always writes `user`). After deploy, `firestore.rules` treats `role == "admin"` as an operator who may delete any `blood_requests` document, read all `donations` for statistics, and update other `users` documents (for example toggling `isBlocked`).

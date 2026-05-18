import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v1";

admin.initializeApp();

/**
 * When the Android app writes a notifications/{id} row for a matched donor,
 * send a high-priority FCM so the device wakes and shows a heads-up alert.
 *
 * Data payload includes `requestId` so MainActivity can open RequestDetailScreen.
 */
export const sendEmergencyFcmOnNotificationCreate = functions.firestore
  .document("notifications/{docId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    if (!data || data.type !== "BLOOD_REQUEST") {
      return;
    }

    const userId = data.userId as string | undefined;
    const requestId = data.relatedRequestId as string | undefined;
    if (!userId || !requestId) {
      functions.logger.warn("BLOOD_REQUEST notification missing userId or relatedRequestId");
      return;
    }

    const userSnap = await admin.firestore().doc(`users/${userId}`).get();
    const raw = userSnap.get("fcmTokens");
    const tokens = Array.isArray(raw)
      ? (raw as unknown[]).filter((t): t is string => typeof t === "string")
      : [];
    if (tokens.length === 0) {
      functions.logger.info(`No fcmTokens for user ${userId}`);
      return;
    }

    const title = String(data.title || "URGENT BLOOD REQUEST");
    const body = String(data.body || "");

    const message: admin.messaging.MulticastMessage = {
      tokens,
      notification: { title, body },
      data: {
        requestId,
        title,
        body,
        click_action: "OPEN_BLOOD_REQUEST",
      },
      android: {
        priority: "high",
        notification: {
          channelId: "raktaseva_emergency",
          sound: "default",
          defaultSound: true,
          clickAction: "OPEN_BLOOD_REQUEST",
        },
      },
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    functions.logger.info(
      `FCM multicast user=${userId} success=${response.successCount} failure=${response.failureCount}`
    );
  });

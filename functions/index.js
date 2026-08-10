const functions = require('firebase-functions/v1');
const { initializeApp } = require('firebase-admin/app');
const { getDatabase } = require('firebase-admin/database');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp({
  databaseURL: 'https://hasetapp-4eeba-default-rtdb.firebaseio.com',
});

const DB = getDatabase();

exports.onNewAppointment = functions.database
  .ref('/appointments/{appointmentId}')
  .onWrite(async (change, context) => {
    const { appointmentId } = context.params;

    const before = change.before.val();
    const after = change.after.val();

    // Only fire on creation (before was null)
    if (before !== null) {
      return null;
    }

    if (!after) {
      return null;
    }

    // Only notify for pending appointments (new bookings)
    if (after.status !== 'pending') {
      return null;
    }

    // Dedup check — skip if we already sent a notification for this appointment
    const dedupRef = DB.ref(`appointment_notifications/${appointmentId}`);
    const dedupSnap = await dedupRef.once('value');
    if (dedupSnap.exists()) {
      functions.logger.info('Duplicate appointment notification skipped');
      return null;
    }

    const doctorId = after.doctorId;
    if (!doctorId) {
      return null;
    }

    // Look up the doctor's FCM token
    const tokenSnap = await DB.ref(`users/${doctorId}/fcmToken`).once('value');
    const fcmToken = tokenSnap.val();

    if (!fcmToken) {
      functions.logger.warn('No FCM token found for appointment recipient');
      // Write dedup marker anyway so we don't retry on every trigger
      await dedupRef.set({ sentAt: Date.now(), reason: 'no_token' });
      return null;
    }

    const patientName = after.patientName || 'A patient';
    const appointmentDate = after.date || '';
    const appointmentTime = after.time || '';

    const message = {
      token: fcmToken,
      data: {
        type: 'new_appointment',
        patientName,
        appointmentDate,
        appointmentTime,
        appointmentId,
        title: 'New Appointment Booking',
        message: `${patientName} booked an appointment for ${appointmentDate} at ${appointmentTime}`,
      },
      notification: {
        title: 'New Appointment Booking',
        body: `${patientName} booked an appointment for ${appointmentDate} at ${appointmentTime}`,
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'appointments',
          sound: 'default',
          priority: 'high',
        },
      },
    };

    try {
      await getMessaging().send(message);
      functions.logger.info('Appointment notification sent');

      // Write dedup marker on success
      await dedupRef.set({ sentAt: Date.now(), success: true });
    } catch (error) {
      functions.logger.error('Appointment notification delivery failed', {
        code: error && error.code ? error.code : 'unknown',
      });

      // Write dedup marker even on failure to avoid retry storms
      await dedupRef.set({ sentAt: Date.now(), error: 'delivery_failed' });
    }

    return null;
  });

// Deliver durable in-app notifications through FCM when a recipient is
// online or backgrounded. The database entry remains the source of truth.
exports.onNotificationCreated = functions.database
  .ref('/notifications/{userId}/{notificationId}')
  .onCreate(async (snapshot, context) => {
    const notification = snapshot.val() || {};
    const { userId, notificationId } = context.params;
    const tokenSnap = await DB.ref(`users/${userId}/fcmToken`).once('value');
    const token = tokenSnap.val();
    if (!token) {
      functions.logger.info('No FCM token for notification recipient', { userId, notificationId });
      return null;
    }

    const title = notification.title || 'HASET notification';
    const body = notification.message || '';
    try {
      await getMessaging().send({
        token,
        data: {
          type: notification.type || 'general',
          notificationId,
          title: String(title),
          message: String(body),
          relatedId: String(notification.relatedId || ''),
        },
        notification: { title: String(title), body: String(body) },
        android: {
          priority: 'high',
          notification: { channelId: 'general', sound: 'default', priority: 'high' },
        },
      });
    } catch (error) {
      functions.logger.error('Notification delivery failed', {
        userId,
        notificationId,
        code: error && error.code ? error.code : 'unknown',
      });
    }
    return null;
  });

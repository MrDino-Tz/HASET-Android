import { readFile } from "node:fs/promises";
import test from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { ref, set, update } from "firebase/database";

const rules = await readFile(new URL("../../database.rules.json", import.meta.url), "utf8");
const testEnv = await initializeTestEnvironment({ projectId: "hasetapp-4eeba", database: { rules } });
const patient = testEnv.authenticatedContext("patient-a").database();
const doctor = testEnv.authenticatedContext("doctor-a").database();

test.before(async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.database();
    const now = Date.now();
    await set(ref(db), {
      users: {
        "patient-a": { role: "patient" },
        "doctor-a": { role: "doctor" },
      },
      doctors: { "doctor-a": { approved: true } },
      chat_sessions: {
        "doctor-a_patient-a": {
          appointmentId: "appt-1",
          patientId: "patient-a",
          doctorId: "doctor-a",
          chatStartsAt: now - 1000,
          chatExpiresAt: now + 3600000,
          isChatActive: true,
        },
        "inactive": {
          appointmentId: "appt-2",
          patientId: "patient-a",
          doctorId: "doctor-a",
          chatStartsAt: now - 1000,
          chatExpiresAt: now + 3600000,
          isChatActive: false,
        }
      }
    });
  });
});

const baseMsg = {
  messageId: "msg1",
  senderId: "patient-a",
  senderName: "Patient",
  receiverId: "doctor-a",
  receiverName: "Doctor",
  message: "Image",
  messageType: "image",
  messageStatus: "uploading",
  timestamp: Date.now(),
  isRead: false,
  deliveredTimestamp: 0,
  readTimestamp: 0,
  attachmentFileName: "photo.jpg",
  attachmentSize: "1.2 MB",
};

test("allows image placeholder when chat session active", async () => {
  await assertSucceeds(set(ref(patient, "messages/doctor-a_patient-a/msg1"), baseMsg));
});

test("allows attachmentUrl update by sender", async () => {
  await assertSucceeds(update(ref(patient, "messages/doctor-a_patient-a/msg1"), {
    attachmentUrl: "https://res.cloudinary.com/demo/image/upload/v1/chat_attachments/photo.jpg",
    messageStatus: "sent",
  }));
});

test("allows message when chat window valid even if isChatActive false", async () => {
  await assertSucceeds(set(ref(patient, "messages/inactive/msg2"), { ...baseMsg, messageId: "msg2" }));
});

test("blocks message when no chat session", async () => {
  await assertFails(set(ref(patient, "messages/missing_room/msg3"), { ...baseMsg, messageId: "msg3" }));
});

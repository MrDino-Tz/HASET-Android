import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import test from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { equalTo, get, orderByChild, query, ref, set, update } from "firebase/database";

const projectId = "hasetapp-4eeba";
const rules = await readFile(new URL("../../database.rules.json", import.meta.url), "utf8");
const testEnv = await initializeTestEnvironment({
  projectId,
  database: { rules },
});

const patient = testEnv.authenticatedContext("patient-a").database();
const otherPatient = testEnv.authenticatedContext("patient-b").database();
const doctor = testEnv.authenticatedContext("doctor-a").database();
const otherDoctor = testEnv.authenticatedContext("doctor-b").database();
const admin = testEnv.authenticatedContext("admin-a").database();
const anonymous = testEnv.unauthenticatedContext().database();
const newPatient = testEnv.authenticatedContext("new-patient").database();
const newDoctor = testEnv.authenticatedContext("new-doctor").database();

const appointment = {
  appointmentId: "appointment-a",
  patientId: "patient-a",
  doctorId: "doctor-a",
  patientName: "Patient A",
  doctorName: "Doctor A",
  date: "12/08/2026",
  time: "10:00",
  reason: "Consultation",
  status: "pending",
  appointmentType: "Visit",
  createdAt: 1786500000000,
};

test.before(async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.database();
    await set(ref(db), {
      users: {
        "patient-a": { userId: "patient-a", email: "patient-a@example.test", fullName: "Patient A", role: "patient" },
        "patient-b": { userId: "patient-b", email: "patient-b@example.test", fullName: "Patient B", role: "patient" },
        "doctor-a": { userId: "doctor-a", email: "doctor-a@example.test", fullName: "Doctor A", role: "doctor" },
        "doctor-b": { userId: "doctor-b", email: "doctor-b@example.test", fullName: "Doctor B", role: "doctor" },
        "doctor-c": { userId: "doctor-c", email: "doctor-c@example.test", fullName: "Doctor C", role: "doctor" },
        "admin-a": { userId: "admin-a", email: "admin-a@example.test", fullName: "Admin A", role: "admin" },
      },
      doctors: {
        "doctor-a": { doctorId: "doctor-a", approved: true, verified: true },
        "doctor-c": { doctorId: "doctor-c", approved: false, verified: false },
      },
      payment_transactions: {
        "9001": {
          transactionId: 9001,
          userId: "patient-a",
          doctorId: "doctor-a",
          amount: 500,
          status: "success",
          createdAt: 1786500000000,
        },
      },
      doctor_wallets: {
        "doctor-a": { doctorId: "doctor-a", balance: 125000, totalEarnings: 200000, lastUpdated: 1786500000000 },
        "doctor-b": { doctorId: "doctor-b", balance: 75000, totalEarnings: 90000, lastUpdated: 1786500000000 },
      },
      withdrawal_requests: {
        "withdrawal-a": {
          requestId: "withdrawal-a", doctorId: "doctor-a", amount: 25000,
          status: "pending", requestedAt: 1786500000000,
        },
        "withdrawal-b": {
          requestId: "withdrawal-b", doctorId: "doctor-b", amount: 10000,
          status: "pending", requestedAt: 1786500000000,
        },
      },
      messages: {
        "patient-a_patient-b": {
          "message-a": {
            messageId: "message-a",
            senderId: "patient-a",
            senderName: "Patient A",
            receiverId: "patient-b",
            receiverName: "Patient B",
            message: "Private medical message",
            messageType: "text",
            messageStatus: "sent",
            timestamp: 1786500000000,
            isRead: false,
            deliveredTimestamp: 0,
            readTimestamp: 0,
          },
        },
      },
    });
  });
});

test.after(async () => {
  await testEnv.cleanup();
});

test("blocks anonymous database access", async () => {
  await assertFails(get(ref(anonymous, "users/patient-a")));
  await assertFails(set(ref(anonymous, "appointments/random"), appointment));
});

test("prevents users from promoting themselves to admin", async () => {
  await assertFails(update(ref(patient, "users/patient-a"), { role: "admin" }));
  await assertSucceeds(update(ref(patient, "users/patient-a"), { fullName: "Updated Patient" }));
});

test("allows patient and doctor registration without allowing admin registration", async () => {
  await assertSucceeds(set(ref(newPatient, "users/new-patient"), {
    userId: "new-patient", email: "new-patient@example.test", fullName: "New Patient", role: "patient",
  }));
  await assertSucceeds(set(ref(newDoctor, "users/new-doctor"), {
    userId: "new-doctor", email: "new-doctor@example.test", fullName: "New Doctor", role: "doctor", regNo: "TEST-002",
  }));
  await assertFails(set(ref(newPatient, "users/new-patient"), {
    userId: "new-patient", email: "new-patient@example.test", fullName: "New Patient", role: "admin",
  }));
});

test("allows safe doctor enrollment but protects approval fields", async () => {
  await assertFails(set(ref(patient, "doctors/patient-a"), {
    doctorId: "patient-a", approved: false, verified: false,
  }));
  await assertSucceeds(set(ref(otherDoctor, "doctors/doctor-b"), {
    doctorId: "doctor-b", regNo: "TEST-001", approved: false, verified: false,
  }));
  await assertFails(update(ref(otherDoctor, "doctors/doctor-b"), { approved: true }));
  await assertSucceeds(update(ref(admin, "doctors/doctor-b"), { approved: true, verified: true }));
});

test("allows only the patient to create a pending appointment with an approved doctor", async () => {
  await assertFails(set(ref(otherPatient, "appointments/appointment-a"), appointment));
  await assertSucceeds(set(ref(patient, "appointments/appointment-a"), appointment));
  await assertFails(set(ref(patient, "appointments/unapproved-doctor"), {
    ...appointment,
    appointmentId: "unapproved-doctor",
    doctorId: "doctor-c",
  }));
});

test("limits appointment reads and lifecycle updates to participants", async () => {
  await assertFails(get(ref(otherPatient, "appointments/appointment-a")));
  await assertSucceeds(get(ref(patient, "appointments/appointment-a")));
  await assertSucceeds(get(ref(doctor, "appointments/appointment-a")));
  await assertFails(update(ref(otherDoctor, "appointments/appointment-a"), { status: "approved" }));
  await assertFails(update(ref(patient, "appointments/appointment-a"), { status: "completed" }));
  await assertSucceeds(update(ref(doctor, "appointments/appointment-a"), { status: "approved", updatedAt: 1786500001000 }));
  await assertFails(update(ref(patient, "appointments/appointment-a"), { doctorId: "doctor-b" }));
  await assertSucceeds(update(ref(patient, "appointments/appointment-a"), {
    date: "13/08/2026", time: "11:00", status: "pending", lastUpdated: 1786500002000, rescheduledBy: "patient-a",
  }));
});

test("allows only scoped directory and appointment queries", async () => {
  await assertSucceeds(get(query(ref(patient, "users"), orderByChild("role"), equalTo("doctor"))));
  await assertFails(get(ref(patient, "users")));
  await assertSucceeds(get(query(ref(patient, "appointments"), orderByChild("patientId"), equalTo("patient-a"))));
  await assertFails(get(query(ref(patient, "appointments"), orderByChild("patientId"), equalTo("patient-b"))));
});

test("protects appointment indexes from unrelated writes", async () => {
  await assertSucceeds(set(ref(patient, "patient_appointments/patient-a/appointment-a"), true));
  await assertSucceeds(set(ref(patient, "doctor_appointments/doctor-a/appointment-a"), true));
  await assertFails(set(ref(otherPatient, "doctor_appointments/doctor-a/appointment-a"), true));
  await assertFails(get(ref(otherPatient, "patient_appointments/patient-a")));
});

test("keeps financial records server-owned", async () => {
  await assertFails(set(ref(patient, "payment_transactions/fake"), { userId: "patient-a", amount: 1 }));
  await assertFails(set(ref(doctor, "doctor_wallets/doctor-a"), { balance: 999999 }));
  await assertFails(set(ref(doctor, "withdrawal_requests/fake"), { doctorId: "doctor-a", amount: 999999 }));
});

test("isolates each doctor's wallet and withdrawal records", async () => {
  await assertSucceeds(get(ref(doctor, "doctor_wallets/doctor-a")));
  await assertFails(get(ref(doctor, "doctor_wallets/doctor-b")));
  await assertFails(get(ref(patient, "doctor_wallets/doctor-a")));
  await assertSucceeds(get(ref(admin, "doctor_wallets/doctor-a")));

  await assertSucceeds(get(ref(doctor, "withdrawal_requests/withdrawal-a")));
  await assertFails(get(ref(doctor, "withdrawal_requests/withdrawal-b")));
  await assertFails(get(ref(patient, "withdrawal_requests/withdrawal-a")));
  await assertSucceeds(get(ref(admin, "withdrawal_requests/withdrawal-a")));

  await assertFails(update(ref(doctor, "doctor_wallets/doctor-a"), { balance: 999999 }));
  await assertFails(update(ref(doctor, "withdrawal_requests/withdrawal-a"), { amount: 1 }));
});

test("ties service payment completion to a matching backend transaction", async () => {
  const serviceId = "service-a";
  const request = {
    serviceId,
    messageId: "message-service-a",
    chatRoomId: "doctor-a_patient-a",
    doctorId: "doctor-a",
    patientId: "patient-a",
    serviceName: "Follow-up service",
    appointmentFee: 1000,
    patientPercentage: 50,
    patientPayAmount: 500,
    status: "pending",
    createdAt: 1786500000000,
  };

  await assertSucceeds(set(ref(doctor, `service_payment_requests/${serviceId}`), request));
  await assertFails(update(ref(otherPatient, `service_payment_requests/${serviceId}`), {
    status: "paid", transactionId: "9001", paidAt: 1786500001000,
  }));
  await assertFails(update(ref(patient, `service_payment_requests/${serviceId}`), {
    status: "paid", transactionId: "missing", paidAt: 1786500001000,
  }));
  await assertSucceeds(update(ref(patient, `service_payment_requests/${serviceId}`), {
    status: "paid", transactionId: "9001", paidAt: 1786500001000,
  }));

  const paidRequest = await assertSucceeds(get(ref(doctor, `service_payment_requests/${serviceId}`)));
  assert.equal(paidRequest.child("status").val(), "paid");
});

test("limits chat reads to message participants and scoped queries", async () => {
  const room = "messages/patient-a_patient-b";
  await assertFails(get(ref(patient, room)));
  await assertSucceeds(get(query(ref(patient, room), orderByChild("senderId"), equalTo("patient-a"))));
  await assertSucceeds(get(query(ref(otherPatient, room), orderByChild("receiverId"), equalTo("patient-b"))));
  await assertFails(get(query(ref(doctor, room), orderByChild("receiverId"), equalTo("patient-b"))));
  await assertSucceeds(get(ref(patient, `${room}/message-a`)));
  await assertSucceeds(get(ref(otherPatient, `${room}/message-a`)));
  await assertFails(get(ref(doctor, `${room}/message-a`)));
});

test("prevents chat recipients from changing sender content", async () => {
  const messageRef = ref(otherPatient, "messages/patient-a_patient-b/message-a");
  await assertFails(update(messageRef, { message: "Tampered text", isRead: true, messageStatus: "read", readTimestamp: 1786500001000 }));
  await assertSucceeds(update(messageRef, { messageStatus: "delivered" }));
  await assertSucceeds(update(messageRef, { isRead: true, messageStatus: "read", readTimestamp: 1786500001000 }));
  await assertFails(update(ref(patient, "messages/patient-a_patient-b/message-a"), { receiverId: "doctor-a" }));
});

test("validates new chat identities and rejects unknown fields", async () => {
  const validMessage = {
    messageId: "message-b",
    senderId: "patient-a",
    senderName: "Patient A",
    receiverId: "doctor-a",
    receiverName: "Doctor A",
    message: "Hello doctor",
    messageType: "text",
    messageStatus: "sent",
    timestamp: 1786500002000,
    isRead: false,
    deliveredTimestamp: 0,
    readTimestamp: 0,
  };
  await assertSucceeds(set(ref(patient, "messages/patient-a_doctor-a/message-b"), validMessage));
  await assertFails(set(ref(patient, "messages/patient-a_doctor-a/wrong-key"), validMessage));
  await assertFails(set(ref(patient, "messages/patient-a_doctor-a/message-c"), {
    ...validMessage,
    messageId: "message-c",
    adminOnly: true,
  }));
});

test("sends a chat-room message through delivered and read states", async () => {
  const room = "messages/patient-a_doctor-a";
  const messageRef = ref(patient, `${room}/message-lifecycle`);
  const message = {
    messageId: "message-lifecycle",
    senderId: "patient-a",
    senderName: "Patient A",
    receiverId: "doctor-a",
    receiverName: "Doctor A",
    message: "Chat-room delivery test",
    messageType: "text",
    messageStatus: "sent",
    timestamp: 1786500003000,
    isRead: false,
    deliveredTimestamp: 0,
    readTimestamp: 0,
  };

  await assertSucceeds(set(messageRef, message));

  const received = await assertSucceeds(get(query(
    ref(doctor, room),
    orderByChild("receiverId"),
    equalTo("doctor-a"),
  )));
  assert.equal(received.child("message-lifecycle/message").val(), message.message);
  assert.equal(received.child("message-lifecycle/messageStatus").val(), "sent");

  const receiverMessageRef = ref(doctor, `${room}/message-lifecycle`);
  await assertSucceeds(update(receiverMessageRef, { messageStatus: "delivered" }));
  await assertSucceeds(update(receiverMessageRef, {
    isRead: true,
    messageStatus: "read",
    readTimestamp: 1786500004000,
  }));

  const finalMessage = await assertSucceeds(get(receiverMessageRef));
  assert.equal(finalMessage.child("messageStatus").val(), "read");
  assert.equal(finalMessage.child("isRead").val(), true);
});

test("allows a sender to batch-delete selected chat messages", async () => {
  const room = "messages/patient-a_doctor-a";
  const makeMessage = (messageId, timestamp) => ({
    messageId,
    senderId: "patient-a",
    senderName: "Patient A",
    receiverId: "doctor-a",
    receiverName: "Doctor A",
    message: `Delete test ${messageId}`,
    messageType: "text",
    messageStatus: "sent",
    timestamp,
    isRead: false,
    deliveredTimestamp: 0,
    readTimestamp: 0,
  });

  await assertSucceeds(set(ref(patient, `${room}/batch-delete-a`),
    makeMessage("batch-delete-a", 1786500005000)));
  await assertSucceeds(set(ref(patient, `${room}/batch-delete-b`),
    makeMessage("batch-delete-b", 1786500006000)));
  await assertSucceeds(update(ref(patient, room), {
    "batch-delete-a": null,
    "batch-delete-b": null,
  }));

  const remaining = await assertSucceeds(get(query(
    ref(patient, room),
    orderByChild("senderId"),
    equalTo("patient-a"),
  )));
  assert.equal(remaining.child("batch-delete-a").exists(), false);
  assert.equal(remaining.child("batch-delete-b").exists(), false);
});

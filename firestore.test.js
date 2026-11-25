const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validDailyQuotaPayload(uid, dateIso = "2026-11-24") {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    dateIso,
    architectUsed: 0,
    visionUsed: 0,
    coachUsed: 0,
    bonusGraceCredits: 0,
    graceClaimed: false,
    ecoLiteMode: false,
    fallbackCascadeCount: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write usageQuota", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ref = unauthDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily");
  await assertFails(ref.get());
  await assertFails(ref.set(validDailyQuotaPayload(ALICE_UID)));
});

test("Authenticated owner can create, read, update usage, toggle eco mode, claim grace once, and rollover", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily");

  // Create initial daily ledger
  await assertSucceeds(ref.set(validDailyQuotaPayload(ALICE_UID, "2026-11-24")));
  await assertSucceeds(ref.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("usageQuota").get());

  // Action 1: Increment usage counters & fallback count
  await assertSucceeds(
    ref.update({
      architectUsed: 1,
      visionUsed: 1,
      fallbackCascadeCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 2: Claim one-time daily +3 grace credits
  await assertSucceeds(
    ref.update({
      graceClaimed: true,
      bonusGraceCredits: 3,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject second grace claim on same day
  await assertFails(
    ref.update({
      graceClaimed: true,
      bonusGraceCredits: 3,
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Action 3: Toggle Eco Lite Mode
  await assertSucceeds(
    ref.update({
      ecoLiteMode: true,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Action 4: Daily rollover on new dateIso (preserves ecoLiteMode)
  await assertSucceeds(
    ref.update({
      dateIso: "2026-11-25",
      architectUsed: 0,
      visionUsed: 0,
      coachUsed: 0,
      bonusGraceCredits: 0,
      graceClaimed: false,
      fallbackCascadeCount: 0,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Reject cross-user access, ghost fields, immutable mutation, future timestamp, and delete", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("usageQuota")
      .doc("daily")
      .set(validDailyQuotaPayload(BOB_UID, "2026-11-24"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRef = aliceDb.collection("users").doc(BOB_UID).collection("usageQuota").doc("daily");
  await assertFails(bobRef.get());
  await assertFails(bobRef.update({ ecoLiteMode: true, updatedAt: new Date(Date.now() - 500) }));

  // Reject ghost field on create
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID).collection("usageQuota").doc("daily");
  await assertFails(
    aliceRef.set({
      ...validDailyQuotaPayload(ALICE_UID),
      isProUnlimited: true,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceRef.set({
      ...validDailyQuotaPayload(ALICE_UID),
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Create valid doc then test immutable field mutation & delete rejection
  await assertSucceeds(aliceRef.set(validDailyQuotaPayload(ALICE_UID, "2026-11-24")));
  await assertFails(
    aliceRef.update({
      createdAt: new Date(Date.now() - 50),
      updatedAt: new Date(Date.now() - 50),
    })
  );
  await assertFails(aliceRef.delete());
});

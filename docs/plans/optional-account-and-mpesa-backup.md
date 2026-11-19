# Plan: optional account, cloud-sync toggle, and opt-in M-Pesa backup

Status: approved direction, ready to implement
Flavors: phases 1–4 apply to both `store` and `personal`; phase 5 is `personal` only.

## Goals

1. A user can use FinTrack **without an account**. Their data stays on the device.
2. A signed-in user can **turn cloud sync off**, and can choose to delete their cloud copy when they do.
3. A local-only user can **create an account or sign in later** without losing their data.
4. (`personal` only) A user can **opt in** to an **end-to-end encrypted backup** of their M-Pesa transactions, so the history survives a lost phone or a phone change.

## Non-goals (don't do these here)

- Fixing audit finding **H2** for the *main* sync. The main cloud sync still wraps its data key with a key built into the APK. Leave it as it is, but **don't reuse that key for anything new**. The M-Pesa backup gets its own passphrase-based key (phase 5).
- Deleting the Firebase Auth account itself, as opposed to the user's cloud data.
- Audit findings H5, H7, M3, M5 and M6, unless a phase below says otherwise.
- Deploying anything to Firebase. Write `firestore.rules`, but don't deploy it.

## How the app works today (verified)

- **Data is keyed by the Firebase UID.** Every repository has `private fun getUserId() = firebaseAuth.currentUser?.uid`. Room entities (`TransactionEntity`, `BudgetEntity`, `CategoryEntity`, `DebtEntity`, `SavingEntity`, `PaymentMethodEntity`, `RecurringTransactionEntity`, `UserEntity`) all have a `userId` column. When there's no user, flows return empty and writes throw `IllegalStateException("User is not logged in")`.
- **ViewModels use Firebase directly too**, e.g. `AddTransactionViewModel` (`FirebaseAuth.getInstance().currentUser?.uid`), plus `ProfileViewModel`, `ProfileSetupViewModel`, `SettingsScreen`, `MainViewModel`, `HomeViewModel`, `BudgetsViewModel`, `CategoryDetailViewModel`, `ManageCategoriesViewModel`, `SavingViewModel`, `DebtViewModel`, `EditRecurringTransactionViewModel`, `GoalCheckWorker`, `DebtCheckWorker` and the personal `MpesaOnboardingViewModel` / `MpesaSettingsViewModel`. Run `git grep -n "currentUser\|getUserId()"` for the full list. There are roughly 140 call sites.
- **The start destination** is chosen in `MainActivity` (around the `startDestination` `remember` block): no user → `Login`, unverified → `VerifyEmail`, otherwise the onboarding integration or `Home`.
- **Sync** is a write-through push in each repository ("if online, push to Firestore") plus one-time sync workers enqueued in `FinTrackApplication.scheduleWorkers()` (`TransactionSyncWorker`, `BudgetSyncWorker`, `SavingSyncWorker`, `DebtSyncWorker`) and the `MainViewModel` / `HomeViewModel` manual sync. The Firestore layout is `users/{uid}/...`.
- **Encryption** (`KeyRepositoryImpl`, `EncryptionManagerImpl`) is only used for *cloud payloads*. Room stores plaintext. The data key lives in `users/{uid}/keys/master`, wrapped with a hard-coded app key (audit H2). Getting the key requires Firestore on first use.
- **Default data**: `categoryRepository.initDefaultCategories()` is called from `AuthViewModel` after sign-up/sign-in, and `paymentMethodRepository.initDefaultPaymentMethods()` from `AddTransactionViewModel`.
- **App lock**: PIN/biometric (`LocalAuthManager`) is already local. "Forgot PIN?" (`MainViewModel.onForgotPin`) signs out and clears the PIN, so the user re-authenticates with their account (audit fix C1). A local-only user has no account to fall back on (see phase 3).
- **M-Pesa (personal)**: `mpesa_db` (`MpesaDatabase` v3), `MpesaTransactionEntity` keyed by `mpesaReceiptNumber` with non-null `rawBody` and `smsId`. Receipt-level category overrides live in `mpesa_category_mappings` (main DB, `MpesaCategoryMappingDao`) and merchant rules in `merchant_categories` (`MerchantCategoryDao`). `SmsTransactionRepository.setCategory()` writes receipt overrides.

## Core design decision: a profile ID owns the data

Introduce a **local profile ID** that owns all Room rows, kept separate from the Firebase UID that names the cloud location:

- **Room `userId` column = `profileId`**, always.
- **Firestore path = `users/{linkedUid}/...`**, used only when an account is linked **and** sync is allowed.

Migrating existing installs is free: on first launch after the update, if a Firebase user is signed in, set `profileId = currentUser.uid`, `accountMode = ACCOUNT` and `cloudSyncEnabled = true`. No Room rows change.

A new local-only user gets `profileId = UUID`. When they later sign in, nothing in Room is re-keyed. We just record `linkedUid` and start syncing.

---

## Phase 1: profile identity and sync gate (no UI changes)

Create in `core/data/local/` (or `core/profile/`):

```kotlin
enum class AccountMode { NONE, LOCAL_ONLY, ACCOUNT }   // NONE = fresh install, not chosen yet

interface ProfileManager {
    val state: StateFlow<ProfileState>          // profileId, mode, linkedUid, cloudSyncEnabled
    fun profileId(): String?                    // non-null once a mode is chosen
    suspend fun createLocalProfile(): String    // LOCAL_ONLY, new UUID
    suspend fun linkAccount(uid: String)        // sets linkedUid, mode = ACCOUNT
    suspend fun setCloudSyncEnabled(enabled: Boolean)
    suspend fun signOutToFreshState()           // see phase 3 for semantics
}

interface SyncGate {
    /** UID to sync to, or null when sync must not happen (no account, sync off, signed out). */
    fun cloudUidOrNull(): String?
    val canSync: Flow<Boolean>
}
```

- Persist the state in a new DataStore (`profile_prefs`). Run the **migration** above on first read.
- Provide both through Hilt (`@Singleton`). For `profileId()` to work in non-suspending code, load the state once at app start (the app already uses `runBlocking` there) or keep an in-memory `StateFlow` that's populated before UI starts.
- Unit tests: fresh install → `NONE`. Existing signed-in user → `ACCOUNT` with `profileId == uid`. `createLocalProfile` → `LOCAL_ONLY` with a stable UUID. `SyncGate` returns null for `LOCAL_ONLY`, for sync off, and when `linkedUid` doesn't match the current Firebase user.

Commit: `Add ProfileManager and SyncGate`.

## Phase 2: move data access off the Firebase UID

Mechanical but wide. Do it repository by repository and keep it compiling:

1. Every repository: replace `getUserId()` for **Room** queries and inserts with `profileManager.profileId()`. Use `syncGate.cloudUidOrNull()` for **Firestore** paths. Where the code does "if online, push", make it "if `cloudUidOrNull() != null` and online, push". When it's null, still write locally and leave `isSynced = false`, so enabling sync later uploads everything.
2. Every ViewModel or worker that reads `FirebaseAuth.currentUser` to scope *data*: use `ProfileManager`. Keep Firebase only where it's genuinely about the account (showing the email, sign-out, Google linking).
3. Sync workers (`TransactionSyncWorker`, `BudgetSyncWorker`, `SavingSyncWorker`, `DebtSyncWorker`) return `Result.success()` immediately when `cloudUidOrNull()` is null. Move their scheduling into a small `CloudSyncScheduler` that enqueues them when sync becomes possible and cancels them when it stops. Observe `syncGate.canSync`.
4. `KeyRepository` / `EncryptionManager`: only call them on the cloud path. Local-only users must never trigger `getOrGenerateMasterKey()`, because it needs Firestore.
5. Default data: call `initDefaultCategories()` / `initDefaultPaymentMethods()` for the profile when it's created (local or account), and keep them idempotent.
6. **Sync timestamps (audit M2):** key `SyncTimestampManager` by `linkedUid`, so a different account never inherits another account's watermark. This is in scope because the refactor touches it anyway.

Acceptance: with a signed-in account and sync on, behaviour is unchanged. Run the unit tests and do a quick manual check that add/edit/delete still syncs.

Commit per area if it's large, e.g. `Scope transactions by profile`, `Scope budgets/savings/debts by profile`, `Gate sync workers on SyncGate`.

## Phase 3: "Continue without an account" and the account & sync settings

**Login screen:** add a secondary button, "Continue without an account". It opens a dialog:

> Your data will be stored only on this phone. If you lose the phone or uninstall the app, it's gone, unless you export it or create an account later. You can create an account any time in Settings.

Confirming calls `createLocalProfile()` and goes to the existing profile setup (name, currency), stored locally only, then the flavor onboarding, then Home.

**Start destination** (`MainActivity`):
- `LOCAL_ONLY` → skip Login/VerifyEmail and go to onboarding or Home.
- `ACCOUNT` → today's logic.
- `NONE` → Login.

**Settings → new "Account & sync" section:**
- **Local-only:** a "Back up your data" card. "Create account" / "Sign in" runs the **link flow** below.
- **Account:** shows the email, a **Cloud sync** switch, and Sign out.
  - Turning sync **off** asks: *Keep my cloud copy* (default) or *Delete my cloud data*. Delete runs phase 4. Either way, local data stays.
  - Turning sync **on** marks everything for upload and starts the sync workers.

**Link flow (local → account),** after a successful sign-in or registration:
- The cloud account has no data → `linkAccount(uid)`, enable sync, mark all local rows `isSynced = false`, and enqueue a sync.
- The cloud account already has data → confirm: "This account already has FinTrack data. Combine it with the data on this phone?"
  - **Combine:** link, push local, pull cloud. IDs are UUIDs, so rows don't collide.
  - **Cancel:** sign out of Firebase and stay local-only.

**Forgot PIN for local-only users:** there's no account to re-authenticate with, so the dialog offers *Export my data first* (the CSV exporter) and *Erase this app's data and start over*. Don't add any path that unlocks without the PIN or biometric. Audit C1 must stay fixed.

**Sign out (account):** keep the current behaviour (back to Login, `NONE`). Don't change audit M6 (data left in Room) here, but make sure a *different* account signing in afterwards doesn't see the previous profile's rows. With profile scoping that means a new `profileId`. Add a test or a manual check.

**Export / import:** the CSV export already exists. Make it reachable from Settings for local-only users, and add a matching **import** if it's small. If it isn't small, list it as a follow-up in your final report.

Commit: `Add local-only mode and Account & sync settings`.

## Phase 4: delete cloud data

`CloudDataDeleter.deleteAll(uid)` removes, in batches of at most 450, every subcollection under `users/{uid}` that the app writes: transactions, recurring transactions, budgets, savings, debts, categories, payment methods, the user profile doc, and `keys/master`. If phase 5 has run, also the `mpesa_backup*` collections. Show progress, handle offline ("try again when online"), and only clear `linkedUid` / turn sync off after it succeeds.

Also add `firestore.rules` to the repo root: owner-only read and write for `users/{uid}/{document=**}` (`request.auth.uid == uid`). **Don't deploy it.** Say so in the final report so the owner can review and deploy it themselves.

Commit: `Add cloud data deletion and Firestore rules`.

## Phase 5 (personal flavor only): opt-in end-to-end encrypted M-Pesa backup

All code goes in `app/src/personal/`. The `store` flavor must not gain any M-Pesa or backup code. `./gradlew compileStoreDebugKotlin` must keep passing.

### UX

- **Settings → M-Pesa → "Cloud backup"**, off by default.
  - Local-only users see "Requires an account" with a link to the Account & sync settings.
  - It's independent of the main Cloud sync switch, but it needs a linked account.
- **Turning it on opens a consent screen** that lists exactly:
  - *What's uploaded:* receipt number, amount, date, type, transaction kind, merchant name, paybill/till number, account number, transaction cost, and your category choices for M-Pesa transactions.
  - *What's never uploaded:* the SMS text, M-Pesa balances, phone numbers.
  - *How it's protected:* encrypted on this phone with a backup passphrase that only you know. FinTrack can't read it.
  - *The trade-off:* **if you forget the passphrase, the backup can't be restored.**

  The user must tick "I understand" and set a passphrase (at least 8 characters, entered twice) before it turns on.
- **Turning it off asks:** *Delete my backup from the cloud* (default) or *Keep it*.
- **Restore:** after signing in on a new install, if a backup exists, offer "Restore your M-Pesa history". It's also available from Settings. The user enters the passphrase, gets a progress bar, then "Restored N transactions". A wrong passphrase gives a clear error and they can retry.
- **Forgot passphrase:** "Reset backup" deletes the cloud backup and starts over with a new passphrase, after a warning.

### Crypto (pure Kotlin, unit-tested; `BackupCrypto`)

- KEK = PBKDF2WithHmacSHA256(passphrase, salt = 16 random bytes, iterations = 310_000, 256 bits).
- Backup DEK = 256-bit random AES key. Wrap it with the KEK using AES-256-GCM (12-byte random IV).
- Store `users/{uid}/mpesa_backup/meta`: `{version: 1, kdf: "PBKDF2-SHA256", iterations, salt, wrappedDek, iv, createdAt}`, with byte fields base64-encoded.
- Records: AES-256-GCM with the DEK, a fresh 12-byte IV per record, and **AAD = the document ID**, so a record can't be swapped between docs.
- Document IDs = base64url(HMAC-SHA256(macKey, receiptNumber)), where `macKey = HKDF(DEK, "fintrack-mpesa-docid")` or another HMAC-derived subkey. This keeps receipt numbers out of the IDs, which are visible in plaintext.
- **On-device cache:** keep the DEK wrapped by a **non-exportable Android Keystore key** generated on the device, not imported, so the user doesn't re-enter the passphrase after every launch. A new device needs the passphrase.
- **Changing the passphrase** only re-wraps the DEK.
- Never log the passphrase, keys or plaintext.
- Tests: round-trip, a wrong passphrase fails to unwrap, a tampered ciphertext or wrong AAD fails, document IDs are deterministic per DEK, and the serialized payload contains no `rawBody`, `phoneNumber`, `smsId` or `smartClues` fields.

### Data

- `users/{uid}/mpesa_backup_records/{docId}` → `{iv, ciphertext, v: 1}`. The plaintext is JSON of `{receipt, amount, type, transactionType, merchantName, paybillNumber, tillNumber, accountNumber, timestamp, transactionCost, parserVersion}`.
- `users/{uid}/mpesa_backup_mappings/{docId}` → encrypted receipt overrides (`mpesa_category_mappings`) and merchant rules (`merchant_categories`). Use the same scheme, with a `kind` field inside the plaintext.
- **Incremental upload:** add an `isBackedUp` flag. This needs an `mpesa_db` migration (v3 → v4), and the main DB if you track mappings there. Alternatively use a separate local "uploaded hashes" table. Pick one and explain why in the commit.
- `MpesaBackupWorker`: periodic (daily, `NetworkType.CONNECTED`), plus one run after a successful M-Pesa sync or a category change. It returns success as a no-op when backup is off or there's no linked account.
- **Restore:** insert only receipts that aren't already in the database. Restored rows have no SMS, so set `smsId = "backup:<receipt>"` and `rawBody = ""`. Add a `source` column (`SMS` / `BACKUP`) in the same migration, so re-parsing logic (`PARSER_VERSION` upgrades) skips rows that have no SMS body. Then restore the mappings.
- Disabling with delete removes `meta`, both collections and the local cached key.

### Docs

- `README.md`: change "M-Pesa SMS messages and transaction data are NEVER sent to the cloud" to make clear it's **never uploaded unless you turn on Cloud backup, and then only end-to-end encrypted, without the SMS text**.
- Update the privacy policy screen (`PrivacyPolicyScreen.kt`) to match.

Commits: `Add BackupCrypto with tests`, `Add M-Pesa backup upload`, `Add M-Pesa backup restore and settings UI`, `Update README and privacy policy for M-Pesa backup`.

---

## Rules for the implementer

- Work phase by phase, with **one or more commits per phase** on your own branch. Don't push and don't open PRs.
- **Never modify Firebase projects:** no `firebase` CLI commands that write, and no rules deployment. Never touch the prod project (`fintrack-prod-ac5af`). Debug builds use `fin-track-f1ed4`.
- Keep `./gradlew testStoreDebugUnitTest testPersonalDebugUnitTest assembleStoreDebug assemblePersonalDebug` green after every phase.
- Build environment:
  - Set `JAVA_HOME` to `C:/Program Files/Android/Android Studio/jbr`.
  - `local.properties` needs `sdk.dir=C:/Users/Admin/AppData/Local/Android/Sdk`.
  - Real Firebase configs are in `app/src/debug/google-services.json` and `app/src/release/google-services.json`. They're gitignored and not in your worktree, so copy them from `C:/projects/fin-track/app/src/{debug,release}/` into your worktree (don't commit them).
  - Debug builds install as `com.fintrack.app.debug`.
- If a decision in this plan turns out to be wrong once you're in the code, choose the safer option, note it in the commit message, and list it in your final report.
- The final report should cover: what's done, what's left, anything that needs the owner (deploying rules, a manual test checklist), and every deviation from this plan.

## Manual test checklist (for the owner, on a phone, debug build)

1. Fresh install → Continue without an account → add transactions → kill and reopen the app: the data is still there and nothing appears in Firestore.
2. Local-only → Settings → Create account → the data appears in Firestore under the new UID.
3. Account → Cloud sync off → *Delete my cloud data* → Firestore `users/{uid}` is empty and the local data is intact.
4. Sign out → sign in as a different account → none of the first account's transactions are visible.
5. (personal) Turn on M-Pesa backup → set a passphrase → wait for the worker or trigger it → the Firestore docs show only ciphertext, with no receipt numbers in the IDs.
6. (personal) Uninstall → reinstall → sign in → Restore with a wrong passphrase gives an error; with the right passphrase the M-Pesa history and category choices come back.
7. (personal) Turn backup off with delete → the backup collections are gone.
8. The store flavor builds and has no backup UI.

# FinTrack bug and code-quality audit

Audited on 2026-11-18 at commit `0d0a9c0`: `f1845df` plus the previous session's uncommitted work (stale-tree removal, `SpendingPeriods`, weekly card, CI).
Line numbers refer to that audited commit; later fix commits on this branch shift some of them.
Paths are relative to `app/src/main/java/com/fintrack/app/` unless they start with `app/` or name a flavour (`personal/…`, `test/…`).

Severity scale:

- **Critical**: security bypass or silent loss of user data.
- **High**: wrong numbers shown to the user, or sync that diverges between devices.
- **Medium**: an edge-case bug, or a meaningful performance or maintainability cost.
- **Low**: hygiene.

The **Status** column records what this branch did about each finding.

## Summary

| # | Sev | Area | Finding | Status |
|---|-----|------|---------|--------|
| C1 | Critical | Security | "Forgot PIN?" on the lock screen unlocks the app | **Fixed** |
| C2 | Critical | Security / Sync | Encryption key cache isn't per user and isn't race-safe, so data can be encrypted with a lost key | **Fixed** (local race; cross-device race documented) |
| C3 | Critical | Sync | Pulling from the cloud overwrites unsynced local edits | **Fixed** |
| H1 | High | Security | 4-digit PIN with a single SHA-256 can be brute-forced instantly offline | **Fixed** (PBKDF2 + migration) |
| H2 | High | Security | KEK hard-coded in the APK, so cloud "encryption" can be reversed by anyone with the APK | Open (needs a design decision) |
| H3 | High | Sync | Soft deletes never reach other devices (`updatedAt` not bumped); deleting a never-uploaded item fails forever | **Fixed** |
| H4 | High | Correctness | Planned (future-dated) transactions leak into date-range totals (budgets, budget alerts) | **Fixed** |
| H5 | High | Sync | Recurring transactions: Firestore doc id ≠ Room id, so deletes resurrect and sync duplicates rows | Open |
| H6 | High | Correctness | M-Pesa parser: wrong merchant names and account numbers (3 failing tests) | **Fixed** |
| H7 | High | Correctness | Decryption failure becomes `"[Error: Decryption Failed]"` / `0.0`, gets stored as synced data and re-uploaded | Open |
| H8 | High | Sync | Manual sync calls a blocking Room query on Main, so the push silently never runs | **Fixed** |
| M1 | Medium | Correctness | Transaction list pagination: first page 30, resets to 20; "All" merge truncates M-Pesa | **Fixed** |
| M2 | Medium | Sync | Global sync timestamp isn't per user and isn't cleared on logout, so account B misses its old data | Open |
| M3 | Medium | Sync | Incremental sync uses device clocks (`updatedAt > lastSync`), so clock skew drops updates | Open |
| M4 | Medium | Correctness | Room queries take `currentTime` as a default argument, frozen at subscription | Partly fixed (via H4) |
| M5 | Medium | Security | No PIN attempt limit or lockout | Open |
| M6 | Medium | Security | Logout leaves all financial data in Room | Open |
| M7 | Medium | Perf | View models load every transaction with `getAllTransactionsPaged(Int.MAX_VALUE)` and filter in memory | Open |
| M8 | Medium | Correctness | `Calendar` month bounds end at `23:59:59.000`; month/week captured once per view model | Open |
| M9 | Medium | Sync | Sync errors are swallowed, so `TransactionSyncWorker` always returns success | Open |
| M10 | Medium | Tests | `MonthlyMpesaCostWorkerTest` NPEs (worker constructed directly); personal-only tests break the store test build | **Fixed** |
| L1 | Low | Security | Logging: user IDs and amounts in `Log.d`/`Log.e`; mixed `Log` and `AppLogger` | Open |
| L2 | Low | Arch | Category icon and colour mapping duplicated in ~17 places | Open |
| L3 | Low | Arch | Stringly-typed filters (`"All"`, `"Income"`, `"M-Pesa"`) | Open |
| L4 | Low | Arch | ~94 inline fully-qualified names | Open |
| L5 | Low | Correctness | Budget "spent" subtracts income in the same category | Open |
| L6 | Low | Perf | `SimpleDateFormat` allocated per row; "Yesterday" computed as `now - 86400000` | Open |
| L7 | Low | Correctness | Paybill parser is skipped for any SMS containing "till" as a substring | Open |

---

## Critical

### C1. "Forgot PIN?" on the lock screen bypasses the app lock
- `MainActivity.kt:181-184` (lock overlay): `onForgotPin = { mainViewModel.setAppLocked(false) }`.
- **Failure scenario:** someone picks up an unlocked phone and opens FinTrack. The PIN overlay appears. They tap **Forgot PIN?** and the overlay disappears. Because the Firebase session is still valid, `startDestination` resolves to Home and they get full access with no credential.
- **Fix (done):** `MainViewModel.forgotPin()` signs out of Firebase and clears the local PIN and biometric settings before unlocking. The user lands on Login and must re-authenticate with their account password, then set a new PIN.

### C2. Encryption master-key cache: not per user, not race-safe
- `core/security/EncryptionManagerImpl.kt:23-30`: `cachedMasterKey` is a plain `var` on a `@Singleton`.
- `core/security/KeyRepositoryImpl.kt:126-190`: `getOrGenerateMasterKey()` does a non-transactional read-then-`set()` on `users/{uid}/keys/master`.
- **Failure scenario 1 (user switch):** user A logs out and user B logs in during the same process. The cache still holds A's key, so all of B's data is encrypted with A's key. On B's other devices it decrypts as `"[Error: Decryption Failed]"` (see H7).
- **Failure scenario 2 (first-login race):** on a new account, `AuthViewModel.kt:170-173` launches several syncs in parallel. Each finds no cached key and no Firestore doc, so each generates a different DEK, and the last `set()` wins. Anything already encrypted with a losing DEK can never be decrypted again.
- **Fix (done, local part):** cache the key per uid and serialise `getMasterKey()` with a `Mutex`.
- **Still open:** across devices, create the key doc inside a Firestore transaction (create-if-absent) and re-read on conflict.

### C3. Cloud pull overwrites unsynced local edits
- `core/data/repository/TransactionRepositoryImpl.kt:215-267` maps every changed cloud doc to an entity and calls `insertAll` (`OnConflictStrategy.REPLACE`, `TransactionDao.kt:73`).
- `presentation/home/HomeViewModel.kt:289-292` runs `syncTransactionsFromCloud()` **before** `syncUnsyncedTransactions()`.
- **Failure scenario:** offline, the user edits transaction T from 500 to 50. Meanwhile another device touches T (or it simply changed in the cloud since the last sync). Back online, the pull REPLACEs local T with the cloud's 500 and `isSynced=true`. The 50 edit is lost and never uploaded. A local soft delete is resurrected the same way.
- **Fix (done):** merge rule in `core/data/sync/TransactionSyncMerge.kt`. A remote row never replaces a local row that is still unsynced, unless the remote copy is strictly newer (`updatedAt`). Remote deletes are likewise skipped while a newer local edit is pending. Push also now runs before pull in `HomeViewModel`. Tested in `TransactionSyncMergeTest`.

## High

### H1. PIN hashing is a single unsalted-strength SHA-256 over a 4-digit space
- `core/util/SecurityUtils.kt:32-44` computes one SHA-256 over PIN plus salt. `PinLoginViewModel.kt:22` limits PINs to 4 digits.
- **Failure scenario:** anyone who obtains the DataStore file (rooted device, ADB backup on a debug build, forensic image) recovers the PIN in about 10,000 hashes, which takes milliseconds. A salt doesn't help when the keyspace is 10⁴.
- **Fix (done):** PBKDF2-HMAC-SHA256 with 120k iterations, stored as `v2$iterations$salt$hash`. Legacy `salt:hash` and plaintext values are verified once and transparently re-hashed (`LocalAuthManager.verifyPin`). Tested in `SecurityUtilsTest`.
- **Better still (open):** with only 10⁴ PINs, no KDF is enough on its own. Bind verification to an Android Keystore HMAC key that is non-exportable and hardware-backed, so offline brute force needs the device's TEE, and add lockout (M5).

### H2. Key-encryption key is a constant in the APK
- `core/security/KeyRepositoryImpl.kt:37` hard-codes `APP_KEK_BYTES = "FinTrackSuperSecretKeyForDEKWait"`.
- **Failure scenario:** anyone with read access to `users/{uid}/keys/master` decompiles the APK, unwraps the DEK and decrypts every "encrypted" field. That covers a Firestore rules mistake, a leaked admin credential and a Google-side breach. It's the threat model field encryption is supposed to address.
- **Fix (open, needs a product decision):** derive the KEK from a user secret, such as a PBKDF2/Argon2 passphrase or a recovery key shown once. Or drop the client-side encryption claim and rely on Firestore security rules. Whichever you choose, remove the constant.

### H3. Soft deletes don't propagate, and offline-created-then-deleted items wedge the queue
- `TransactionRepositoryImpl.kt:127-131` and `:408-411` use `update("deletedAt", …)` and never touch `updatedAt`.
- **Failure scenario A:** a transaction is deleted on phone 1. Phone 2's incremental pull queries `updatedAt > lastSync` (`:204`), and that query never matches the delete. The transaction stays on phone 2 forever.
- **Failure scenario B:** a transaction is created offline and deleted before it was ever uploaded. `update()` on a missing document throws `NOT_FOUND`, so the row is never marked synced and every sync retries it forever.
- **Fix (done):** write the deletion as `set(mapOf("deletedAt" to t, "updatedAt" to t), SetOptions.merge())`, built by `TransactionSyncMerge.deletionPatch`. The local `softDelete` also bumps `updatedAt`.

### H4. Planned transactions leak into date-range totals
- `core/data/local/dao/TransactionDao.kt:35-36`: `getTransactionsByDateRange` has no `isPlanned` filter. Every other list query has one.
- **Failure scenario:** on 18 Nov the user schedules rent of 30,000 for 28 Nov. `BudgetCheckWorker.kt:60` sums the month, so the Housing budget immediately reports "exceeded" and fires an alert for money not yet spent.
- **Fix (done):** `Transaction.hasOccurred(now)` in `core/domain/model/Transaction.kt`. `TransactionRepositoryImpl.getTransactionsByDateRange` filters with it and evaluates `now` per emission, which also fixes M4 for this query. Tested in `TransactionTest`.

### H5. Recurring transactions use mismatched IDs between Room and Firestore
- `TransactionRepositoryImpl.kt:288-304`: the local row gets a Room auto-increment `Int` id, but a **new** recurring item (`id == 0`) is uploaded under a random Firestore id. `:358` maps it back with `doc.id.toIntOrNull() ?: 0`, and `:328-330` deletes by `id.toString()`.
- **Failure scenario:** the user creates "Netflix monthly". Deleting it removes the local row, but the Firestore delete targets doc `"7"`, which doesn't exist. On the next sync the random-id doc maps to id 0, and auto-generate inserts it as a **new** row, so the item comes back. Each sync adds another copy.
- **Fix (open):** make the DAO `insert` return the row id and upload under `rowId.toString()`. Better, migrate the entity to a `String` primary key matching `TransactionEntity` (requires a Room migration, DB version 2 → 3). Also apply the soft-delete and `updatedAt` scheme from H3.

### H6. M-Pesa SMS parser extracts wrong names and accounts
- `personal/…/core/util/MpesaSmsParser.kt:22-24`: in `SENT_MONEY_PATTERN`, `(.+?)\s+(\d+)?.*?on` lets the lazy name stop at the first space, so "JOHN DOE 0712…" becomes `JOHN`.
- `MpesaSmsParser.kt:45-48`: `PAYBILL_PATTERN` swallows ", account number" into the name, producing `PAYBILL 400200, ACCOUNT NUMBER` and `KPLC PREPAID. PAYBILL 888880, ACCOUNT NUMBER`.
- **Failure scenario:** every sent-money SMS to a two-word name is filed under the first name only, which breaks merchant→category learning. Paybill names never match saved mappings.
- **Fix (done):** both patterns are anchored on `on <date>`. The phone number is optional and must have 9 or more digits. The paybill segment is post-processed to strip the `account number N` suffix and the `. Paybill N` suffix. `sent to X for account N` messages (the real paybill format) are now classified as PAYBILL with the account number. Existing tests pass, and a new test covers the "for account" case.

### H7. Decryption failures become silent data corruption
- `core/security/EncryptionManagerImpl.kt:77-84` returns `"[Error: Decryption Failed]"`. The callers (`TransactionRepositoryImpl.kt:230-235`, and the same pattern in Debt and Saving) turn that into `amount = 0.0` and `category = "[Error…]"`, then store it with `isSynced = true`.
- **Failure scenario:** after C2's key mix-up, a sync stores every transaction as 0.0. If the user then edits one, the corrupt values are re-encrypted with the current key and uploaded over the original ciphertext, which permanently destroys the cloud copy.
- **Fix (open):** throw a typed `DecryptionException`. In sync, skip that document, leave the local row untouched and surface a "some items couldn't be decrypted" state. Never write placeholder values back.

### H8. Manual sync runs a blocking Room query on the main thread
- `core/data/local/dao/TransactionDao.kt:84`: `getUnsyncedTransactions` was a non-`suspend` DAO method.
- `HomeViewModel.kt:289-292` calls `syncUnsyncedTransactions()` from `viewModelScope`, which runs on `Dispatchers.Main`.
- **Failure scenario:** Room throws `IllegalStateException: Cannot access database on the main thread`. The repository's `catch` logs and swallows it, so pull-to-sync shows "Success" while nothing is uploaded. Only the WorkManager path, which runs off Main, ever pushed.
- **Fix (done):** made it `suspend`, so Room moves it off the main thread.

## Medium

### M1. Transaction list pagination is inconsistent
- `presentation/transactions/TransactionListViewModel.kt:75` starts at 30, `:309` loads 30 more, and `:323` and `:328` reset to **20**.
- **Failure scenario:** the user opens the list and sees 30 items. They change the filter and back, and now see 20. The next "load more" jumps to 50 with no stable page boundary.
- Also, in "All", manual rows are limited at the DB (`limit`) but M-Pesa rows come from a fixed 50-item source. After merging and `take(limit)`, scrolling past roughly 50 M-Pesa items shows only manual entries.
- **Fix (done):** single `PAGE_SIZE = 30` constant used for the initial limit, increments and resets. Merge, filter and limit logic is extracted into the pure `TransactionListFilter`, tested in `TransactionListFilterTest`.
- **Open:** paginate the M-Pesa source as well.

### M2. Sync timestamp is global, not per user
- `core/data/local/SyncTimestampManager.kt:24-43`: one `last_transaction_sync` key. `clearAllTimestamps()` is never called; logout (`SettingsViewModel.kt:49-59`) doesn't clear it.
- **Failure scenario:** user A syncs at T. Later, user B logs in on the same device. B's first sync runs as an incremental `updatedAt > T`, so all of B's older transactions never download.
- **Fix:** key the timestamp by uid, or clear it (and the user's Room rows, see M6) on logout.

### M3. Incremental sync depends on device clocks
- `TransactionRepositoryImpl.kt:196` and `:204`: `lastSync = System.currentTimeMillis()` on this device, compared against `updatedAt` written by other devices' clocks (`:65`, `:416`).
- **Failure scenario:** phone 2's clock runs 5 minutes behind. An edit it makes is stamped T−5m, so phone 1, which last synced at T−1m, never pulls it.
- **Fix:** write `updatedAt` as `FieldValue.serverTimestamp()` (or a separate `serverUpdatedAt`) and store the max server timestamp seen as the sync cursor.

### M4. `currentTime` default parameters freeze at subscription
- `TransactionDao.kt:32`, `:48`, `:59`, `:70` and `:95` declare `currentTime: Long = System.currentTimeMillis()` on `Flow` queries. Room re-runs the query on table changes but reuses the captured value.
- **Failure scenario:** the user leaves the Transactions screen open past a planned transaction's time. It stays under "Planned" and stays out of the list until the screen is re-subscribed.
- **Fix:** partly done in H4 for date ranges. For the rest, tick a `currentTime` flow (for example every minute, or at the next planned item's time) and `flatMapLatest` into the DAO.

### M5. No PIN attempt limiting
- `presentation/auth/pin/PinLoginViewModel.kt:53-86` allows unlimited attempts with a 200 ms delay.
- **Failure scenario:** someone with the device tries all 10,000 PINs in under an hour.
- **Fix:** exponential back-off after 5 failures, persisted in DataStore so it survives restarts. After N failures, force the account-password flow (as in C1).

### M6. Logout leaves financial data on the device
- `SettingsViewModel.kt:49-59` signs out and clears the PIN, but no Room tables. The `deleteAllForUser` DAOs exist (`BudgetDao.kt:34`, etc.) but nothing calls them.
- **Failure scenario:** on a shared or sold phone, the previous user's transactions are still in `finance.db`. They're readable on a rooted device and they keep counting towards local storage.
- **Fix:** add `clearUserData(uid)` on logout covering all DAOs, sync timestamps and the key cache.

### M7. Every screen loads every transaction
- `HomeViewModel.kt:180` and `:247`, `ReportsViewModel.kt:67` and `:175`, `BudgetsViewModel.kt:145`, `SetupViewModel.kt:173` and `:187`, and `GetTransactionsUseCase.kt:14` all call `getAllTransactionsPaged(Int.MAX_VALUE)`, then filter by date or type in memory, often in several `combine`s at once. Each also re-maps all categories.
- **Failure scenario:** with 5 years of M-Pesa history (~10k rows), each Room change re-materialises and re-filters 10k objects several times on Home. That causes jank and GC churn.
- **Fix:** add DAO aggregate queries, for example `SELECT category, SUM(amount) … WHERE date BETWEEN … GROUP BY category` and a weekly-sum query, and use `getTransactionsByDateRange` for windows. Keep `Int.MAX_VALUE` only for CSV export.

### M8. `Calendar`-based period math
- `HomeViewModel.kt:189-204`, `BudgetsViewModel.kt:185-202` and `:302-316`, and `BudgetCheckWorker.kt:40-53` build month ranges by mutating a `Calendar`, ending at `23:59:59` (sometimes without `.999`). That leaves a one-second gap, so a transaction at 23:59:59.5 on the last day is excluded.
- `BudgetsViewModel.kt:90-91` captures `currentMonth` once, so a view model alive across midnight on the 1st shows last month's budgets.
- **Fix:** use the `java.time` helpers in `core/domain/util/SpendingPeriods.kt` (half-open `[start, nextStart)` ranges, zone-aware) everywhere, and inject `TimeProvider`.

### M9. Sync failures reported as success
- `TransactionRepositoryImpl.kt:273-275` and `:430-438` catch and log everything. `TransactionSyncWorker.kt:38-41` therefore always returns `success()`, so WorkManager's retry/back-off never applies.
- **Fix:** return a result (succeeded/failed counts) from the sync, and use `Result.retry()` when anything failed.

### M10. `MonthlyMpesaCostWorkerTest` constructs a `CoroutineWorker` directly
- `test/…/MonthlyMpesaCostWorkerTest.kt` passed a mocked `WorkerParameters`. `CoroutineWorker`'s initialiser dereferences `taskExecutor`, causing 4 NPEs.
- **Fix (done):** build through `TestListenableWorkerBuilder` with a custom `WorkerFactory` injecting the fakes.
- Also, `MpesaSmsParserTest` and `MonthlyMpesaCostWorkerTest` lived in the shared `src/test` but reference personal-only classes, so `testStoreDebugUnitTest` didn't compile. They now live in `src/testPersonal`.

## Low

### L1. Logging
- `Log.d` calls with user IDs: `BudgetRepositoryImpl.kt:124`, `CategoryRepositoryImpl.kt:152`, `DebtRepositoryImpl.kt:185`, `SavingRepositoryImpl.kt:209`, `TransactionRepositoryImpl.kt:342`, `UserRepositoryImpl.kt:96` and `:136`.
- `Log.d` calls with amounts and receipts: `personal/…/MpesaTransactionRepositoryImpl.kt:128` (logs the start of the SMS body) and `:180`, and `personal/…/InsightsStep.kt:88`.
- R8 strips `Log.d/v/i` in release (`app/proguard-rules.pro:158`), but `Log.w/e` stay, and debug builds handed to testers log everything.
- **Fix:** route everything through `AppLogger`, using `AppLogger.sanitizeUserId`, and never log SMS bodies.

### L2. Duplicated category icon and colour mapping
- `parseColor(...)` with try/catch appears in about 17 places, including `TransactionListViewModel.kt:216` and `:266`, `HomeViewModel.kt:152` and `:224`, `ReportsViewModel.kt:225`, `BudgetsViewModel.kt:218`, `ManageTransactionViewModel.kt:66`, `PaymentMethodsScreen.kt:114`, `:446` and `:679`, and `CategoryDetailScreen.kt:130`, `:206`, `:311` and `:313`.
- There are three icon lookups: `getIconByName` (`CategoryDetailScreen.kt:319` and `ManageCategoriesScreen.kt:360`, two copies), `getCategoryIcon` (`GoalUtils.kt:32`) and a private `getCategoryIcon` (`RecurringTransactionsScreen.kt:294`).
- **Fix:** add one `CategoryVisuals` (`fun Category.toUi(): CategoryUiCache`) in `presentation/utils`.

### L3. Stringly-typed filters
- `TransactionListViewModel.kt:55` and `:59` (`"All"`/`"Income"`/`"Expense"`, `"All"`/`"Manual"`/`"M-Pesa"`) and 23 such literals across `presentation/`.
- `filter.uppercase()` (`:98`) relies on the label matching the enum name.
- **Fix:** add `enum class TypeFilter { ALL, INCOME, EXPENSE }` and `enum class SourceFilter`, with labels only in the UI.

### L4. Inline fully-qualified names
About 94 occurrences, for example `kotlinx.coroutines.flow.flowOf` throughout `TransactionRepositoryImpl.kt`, `com.fintrack.app.core.security.EncryptionManager` in four repository constructors, and `com.fintrack.app.presentation.settings.getIconByName` in `TransactionListViewModel.kt:213`. Use imports.

### L5. Budget "spent" nets income
`BudgetCheckWorker.kt:66-72` subtracts INCOME in the budget's category, so a refund or reimbursement can make a blown budget look healthy. Either intentional (document it) or count EXPENSE only.

### L6. Date grouping
`TransactionListViewModel.kt:335-347` allocates two `SimpleDateFormat`s per row, and "Yesterday" is `now - 86400000`, which is wrong on DST-transition days. Use `LocalDate` comparisons with a single cached `DateTimeFormatter`.

### L7. "till" substring check
`personal/…/MpesaSmsParser.kt` (`parsePaybill`) returns early when the SMS contains "till" anywhere, so a paybill to, say, "DISTILLERS LTD" is misclassified. Match `till` (or "till number") instead.

---

## Not covered
- Compose UI behaviour and instrumentation tests.
- Firestore security rules (not in the repo; check that `users/{uid}/**` is restricted to `request.auth.uid == uid`).
- `MpesaAnalyticsEngine` and `CategorySuggestionAnalyzer` heuristics.

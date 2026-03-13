# RegexCaller Critical Issues (Highest → Lowest Priority)

## P0 — Exposed release signing credentials and keystore in repository
**Impact:** Immediate supply-chain/security risk. Anyone with repo access can sign malicious builds as your app.

**Evidence:**
- `regexcaller-release.jks` exists in repo root.
- `app/build.gradle.kts` contains hardcoded credentials in `signingConfigs.release`:
  - `storePassword = "regex123"`
  - `keyPassword = "regex123"`

**Fix now:**
1. Rotate/recreate keystore immediately.
2. Remove keystore from git history and repository.
3. Move signing secrets to local/CI secret store (`gradle.properties`/env vars).
4. Add `*.jks`, `*.keystore` to `.gitignore`.

---

## P1 — Over-privileged manifest permissions (high Play policy/review risk)
**Impact:** Increased policy rejection and user trust risk; unnecessary access to sensitive call data.

**Evidence:**
- `app/src/main/AndroidManifest.xml` includes:
  - `android.permission.READ_CALL_LOG`
  - `android.permission.ANSWER_PHONE_CALLS`

For `CallScreeningService` + `ROLE_CALL_SCREENING`, these are typically not required for your current implementation.

**Fix:**
1. Remove unused dangerous permissions.
2. Keep minimum required permissions only.
3. Re-verify behavior on API 29+.

---

## P1 — Privacy leak risk via backup defaults
**Impact:** Rule database (phone patterns/labels) may be backed up to cloud/device transfer, conflicting with privacy-first claim.

**Evidence:**
- `android:allowBackup="true"` in `AndroidManifest.xml`.
- `backup_rules.xml` and `data_extraction_rules.xml` are default templates and do not explicitly exclude database content.

**Fix:**
1. Set strict backup exclusions for DB and sensitive prefs, or disable backup for this app.
2. Align README privacy claims with actual backup behavior.

---

## P1 — Lint pipeline is broken (quality gate unavailable)
**Impact:** Static analysis cannot run, so release-critical issues may ship undetected.

**Evidence:**
- `./gradlew lintDebug` fails with:
  - `Can't initialize detector androidx.compose.runtime.lint.AutoboxingStateCreationDetector`

Likely version/tooling incompatibility across AGP/Kotlin/Compose lint components.

**Fix:**
1. Align AGP/Kotlin/Compose BOM and compiler extension to compatible versions.
2. Re-run lint in CI and fail build on lint errors.

---

## P2 — Country-specific number normalization can cause false blocks/allow
**Impact:** Incorrect matching outside India numbering assumptions.

**Evidence:**
- `NumberNormalizer.normalize()` strips `91` and leading `0` using fixed length assumptions.
- `allVariants()` always injects `0`, `91`, and `+91` variants.

**Fix:**
1. Make normalization region-aware (or configurable by country).
2. Keep strict E.164 handling where possible.
3. Add tests for non-`+91` regions.

---

## P2 — Editing a rule resets creation timestamp semantics
**Impact:** Rule ordering/history integrity issues after edits.

**Evidence:**
- In `AddRuleScreen`, update path recreates `BlockRule` without preserving original `createdAt`, causing new default timestamp.

**Fix:**
1. Preserve original `createdAt` on update.
2. Update `updatedAt` explicitly on each edit.

---

## P3 — Onboarding screen assumes Activity context cast
**Impact:** Potential runtime crash in non-activity composition contexts/previews.

**Evidence:**
- `OnboardingScreen`: `val activity = context as Activity`

**Fix:**
1. Use safe cast (`context as? Activity`) and graceful fallback.
2. Guard role request launch when no activity is available.

---

## Quick remediation order
1. **P0 secret exposure** (rotate keys + purge history)
2. **Manifest permission minimization**
3. **Backup/privacy hardening**
4. **Restore lint/CI gate**
5. **Normalization + rule metadata correctness**

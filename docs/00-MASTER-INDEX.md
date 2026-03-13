# 00-MASTER-INDEX

# RegexCaller — Agent Implementation Guide (Master Index)

## Project Summary

**App Name:** RegexCaller (Pattern-Based Call Blocker)  
**Target Device:** Samsung Galaxy S23 running Android 15 (SDK 35)  
**Package:** `com.regexcaller.callblocker`  
**Architecture:** MVVM + Repository | Jetpack Compose | Room DB | Kotlin  
**Key Constraint:** Must NOT replace the Samsung default dialer — operates as a silent add-on via `ROLE_CALL_SCREENING`

---

## TDD Methodology: RED / GREEN / REFACTOR

Every implementation phase follows strict RED/GREEN TDD:

| Cycle | What Happens |
| --- | --- |
| **RED** | Write a failing test FIRST. Run it. Confirm it fails (compile error or assertion failure). |
| **GREEN** | Write the MINIMUM code to make the test pass. Run it. Confirm it passes. |
| **REFACTOR** | Clean up the code without changing behavior. Run tests. Confirm all still pass. |

**Rules for the implementing agent:**

1.  Never write production code without a failing test first
2.  Never write more production code than needed to pass the current failing test
3.  Run tests after every GREEN step — all must pass before proceeding
4.  Commit at each GREEN checkpoint (logical save point)

---

## Document Index

| #   | Document | Description |
| --- | --- | --- |
| 00  | [Prerequisites — Dev Environment Setup](00-PREREQUISITES.md) | Homebrew, JDK 17, Android Studio, SDK, emulator (macOS Apple Silicon) |
| 01  | [Phase 1: Project Setup & Configuration](01-PROJECT-SETUP.md) | Android Studio wizard setup, Gradle, dependencies |
| 02  | [Phase 2: Data Layer — Room Database (TDD)](02-DATA-LAYER-TDD.md) | Entity, DAO, Database with instrumented tests |
| 03  | [Phase 3: Number Normalizer Engine (TDD)](03-NUMBER-NORMALIZER-TDD.md) | Indian number normalization with 17 unit tests |
| 04  | [Phase 4: Pattern Matching Engine (TDD)](04-PATTERN-MATCHER-TDD.md) | Wildcard/regex matching with 32 unit tests |
| 05  | [Phase 5: CallScreeningService](05-CALL-SCREENING-SERVICE.md) | Core Android service for call interception |
| 06  | [Phase 6: Repository & ViewModel](06-REPOSITORY-VIEWMODEL.md) | Data flow wiring, MVVM pattern |
| 07  | [Phase 7: UI Layer — Compose Screens](07-UI-LAYER.md) | Home, AddRule, TestScreen, Navigation |
| 08  | [Phase 8: Onboarding & Permissions](08-ONBOARDING-PERMISSIONS.md) | ROLE_CALL_SCREENING, Samsung One UI specifics |
| 09  | [Phase 9: Samsung S23 & Android 15+ Hardening](09-SAMSUNG-ANDROID15.md) | OEM quirks, battery optimization, edge cases |
| 10  | [Phase 10: Production & Play Store Readiness](10-PRODUCTION-READINESS.md) | ProGuard, privacy policy, Play Store review |
| 11  | [User Guide — How to Use the App](11-USER-GUIDE.md) | End-user guide: setup, rules, testing, FAQ |

---

## Implementation Order (Critical Path)

```
Prereqs ──→ Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4 ──→ Phase 5
  (env)      (setup)    (data)     (normalize)  (match)     (service)
                                                    │
                                                    ▼
              Phase 6 ──→ Phase 7 ──→ Phase 8 ──→ Phase 9 ──→ Phase 10
              (MVVM)      (UI)       (onboard)   (Samsung)    (release)
```

**Dependencies:**

- **Prerequisites** must be completed before anything else (JDK, Android Studio, SDK, emulator)
- Phase 3 is pure Kotlin — can be developed and tested independently of Android
- Phase 4 is pure Kotlin logic but **imports** `BlockRule` **from Phase 2** for the `matches()` and `findMatchingRule()` APIs. Phase 2's entity must exist before Phase 4 tests compile.
- Phase 5 depends on Phase 2 (DAO) + Phase 4 (matcher)
- Phase 6 depends on Phase 2
- Phase 7 depends on Phase 6. Uses a `hasCallScreeningRole()` stub until Phase 8.
- Phase 8 depends on Phase 7
- Phase 9 is a cross-cutting concern — applies after Phase 8
- Phase 10 is final polish

---

## Final Project Structure

```
app/src/main/java/com/regexcaller/callblocker/
├── CallBlockerApp.kt                    ← Application class
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── BlockRule.kt                 ← @Entity
│   │   └── BlockRuleDao.kt             ← @Dao
│   ├── model/
│   │   └── BlockAction.kt              ← object with BLOCK|SILENCE|ALLOW constants
│   └── repository/
│       └── BlockRuleRepository.kt
├── engine/
│   ├── CallBlockerService.kt           ← CallScreeningService
│   ├── NumberNormalizer.kt
│   └── PatternMatcher.kt
├── ui/
│   ├── MainActivity.kt
│   ├── screens/
│   │   ├── AddRuleScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   └── TestScreen.kt
│   ├── theme/
│   │   └── Theme.kt
│   └── viewmodel/
│       └── BlockRuleViewModel.kt
└── util/
    └── PermissionHelper.kt

app/src/test/java/com/regexcaller/callblocker/
├── engine/
│   ├── NumberNormalizerTest.kt          ← Unit tests (JVM)
│   └── PatternMatcherTest.kt           ← Unit tests (JVM)
└── data/
    └── BlockRuleTest.kt                ← Entity validation tests

app/src/androidTest/java/com/regexcaller/callblocker/
└── data/
    └── db/
        ├── BlockRuleDaoTest.kt          ← Instrumented Room tests
        └── AppDatabaseTest.kt
```

---

## Verification Checkpoints

After each phase, the implementing agent MUST verify:

| Phase | Checkpoint |
| --- | --- |
| 1   | Wizard project compiles, Compose activity launches on emulator |
| 2   | All Room instrumented tests pass (DAO CRUD + queries) |
| 3   | All NumberNormalizer unit tests pass (17 test cases) |
| 4   | All PatternMatcher unit tests pass (32 test cases) |
| 5   | Service registered — `adb shell dumpsys role` shows role available |
| 6   | ViewModel exposes Flow, repository wraps DAO correctly |
| 7   | UI renders: empty state, add rule, rule list, test screen |
| 8   | Onboarding shows role dialog on Samsung S23, role is granted |
| 9   | Battery optimization exempt, service survives Samsung kill |
| 10  | Signed APK, ProGuard passes, no sensitive data leaks |
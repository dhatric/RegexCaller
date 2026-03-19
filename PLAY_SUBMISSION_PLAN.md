# Play Submission Remediation Plan

## Summary
Make the app submission-safe by closing policy gaps before changing product behavior: add a real privacy policy surface, remove unnecessary sensitive permission requests, and explicitly define Android 12+ backup behavior. After that, run one real-device call-screening regression pass on the release build.

## Key Changes
- Add a privacy policy entry in the app, most naturally in Settings, pointing to a public non-PDF policy URL or rendering policy text in-app.
- Create the matching Play Console materials: privacy policy URL and Data Safety answers aligned with actual behavior.
- Remove `READ_PHONE_STATE` from the manifest unless a concrete API usage is added and disclosed.
- Add explicit Android 12+ backup config with `android:dataExtractionRules`, and align the README/privacy language with the final behavior.
- Remove or keep dead the direct battery-optimization exemption flow so the release path does not present a policy-sensitive request.
- Update release docs to mention `bundleRelease` as the Play submission artifact, even though the build already supports it.

## Test Plan
- Run `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew bundleRelease`.
- Install the release build on at least one Android 14/15 Samsung device and verify:
  - onboarding grants call-screening role,
  - non-contact calls are screened,
  - contact behavior matches the contacts-permission disclosure,
  - blocked/silenced/allow rules behave as described,
  - the in-app privacy-policy entry is reachable.
- Verify Play Console declarations match the shipped manifest permissions and actual data handling.

## Assumptions
- Target store is Google Play, not Apple App Store, because this repo is Android-only.
- Current core functionality does not require `READ_PHONE_STATE`; that conclusion is based on source inspection and search results.
- The missing privacy policy and Data Safety setup are treated as release blockers for public submission, not for internal testing.

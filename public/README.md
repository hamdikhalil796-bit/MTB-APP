# Release APKs

This folder is deployed as-is to Firebase Hosting (`firebase deploy --only hosting`),
giving downloadable URLs like `https://mtbapp-fef5e.web.app/mtb-latest.apk`.

## Cutting a release

1. Build the release APK in Android Studio (or `./gradlew assembleRelease`).
2. Copy it into this folder, e.g. as `mtb-latest.apk`.
3. Run `firebase deploy --only hosting` from the project root.
4. In the Firebase Console, update the `app_meta/latest_release` Firestore document:
   - `versionCode` — must be higher than `app/build.gradle.kts`'s current `versionCode`
   - `versionName` — matching `versionName`
   - `apkUrl` — `https://mtbapp-fef5e.web.app/mtb-latest.apk`
   - `releaseNotes` — shown to users in the update dialog
   - `mandatory` — `true` to force the update (no "Later" option), `false` otherwise

Actual `.apk` files in this folder are gitignored — only this README is tracked.

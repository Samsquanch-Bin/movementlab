# MovementLab

A calisthenics tracker for Android: routines, demo videos for every move, and progress tracking for reps, holds and added weight.

## Install on a phone

1. On the phone, open this repo's **Releases** page (on GitHub, tap **Releases** on the right side or bottom of the repo page).
2. Download **MovementLab.apk** from the newest release.
3. Open the downloaded file. Android will ask to allow installs from your browser or file manager; allow it once, then tap **Install**.

Every push to `main` builds a new release automatically. Installing a newer APK updates the app and keeps all workout data.

## Features

- **Beginner / Intermediate / Advanced** picked at first launch (changeable in Settings), each with a starter program:
  - Beginner: Full Body A/B
  - Intermediate: Upper / Lower + Skills
  - Advanced: Push / Pull / Legs
- **~50 exercises** organized into progression ladders (wall push-up → … → one-arm push-up, dead hang → … → archer pull-up, assisted squat → … → pistol, plus dips, muscle-up, L-sit, handstand, front lever, core and mobility).
- **Demo videos play inside the app** for all 50 exercises (embedded YouTube player with fullscreen), on the exercise screen and mid-workout via the Demo button. Any exercise's video can be swapped by pasting a different YouTube link.
- **Workout logger** built for mid-set use: big +/− buttons, last session's numbers shown, auto rest timer with vibration, stopwatch for holds, screen stays on.
- **Added or assisted weight** per set (vest/belt as +, bands as −), in kg or lb.
- **Easier / Harder** buttons to swap to the neighbouring step of a ladder mid-workout.
- **Level-up suggestions** when every set hits its target.
- **Progress:** per-exercise charts and personal records, 12-week calendar, weekly streak, body weight log.
- **Custom exercises and routines**: copy and edit any built-in routine.
- **Backup / restore** to a JSON file.
- Logging works fully offline and all data stays on the phone (demo videos need a connection).

## Building

Built with Kotlin + Jetpack Compose. GitHub Actions (`.github/workflows/build.yml`) builds the APK on every push and pull request.

Locally, with Android Studio or the Android SDK installed:

```
./gradlew assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`.

`keystore/movementlab.keystore` is a shared signing key committed on purpose so every build can update the previous install. It is fine for a personal side-loaded app; use a private key before publishing to the Play Store.

## Code map

- `data/Models.kt`: data classes and JSON storage format
- `data/Seed.kt`: built-in exercises, ladders and programs (edit here to add moves)
- `data/Videos.kt`: the YouTube video used for each built-in exercise
- `ui/VideoPlayer.kt`: in-app YouTube player
- `data/AppViewModel.kt`: app state, workout logic, progress calculations
- `ui/screens/`: one file per screen

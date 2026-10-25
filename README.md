# Listo

A personal Android todo app with a full-page home-screen widget. Categories (pw, my, bl), recurring todos, a dated archive and automatic backup to a file on the phone. Works fully offline. Not on the Play Store: GitHub builds it and hosts each release.

This repo holds code only. Your todos live on your phone and in your backup file; they are never uploaded anywhere.

## Install on the phone (first time)

1. On the phone, open this repo's **Releases** page and tap the latest `listo-x.y.z.apk`.
2. When Android asks, allow your browser to install apps, then tap **Install**.
3. Open Listo and follow the setup card: choose the backup folder (pick **Documents**), allow background, add the widget.

## Updates

In the app: **⋮ → Check for update**. It downloads the newest release and installs it over the app; your data stays.

## Making a release

GitHub → **Actions** → **Build** → **Run workflow** → enter a version such as `1.0.1` → **Run**. A few minutes later the signed APK appears under Releases. Pushing a tag like `v1.0.1` does the same.

Every push to `main` also builds a test APK (under the run's Artifacts) to check the code compiles and the tests pass. Test APKs use a throwaway key and can't update an installed release.

## Signing key (one-time setup)

Releases are signed with your own key, stored as four repository secrets (Settings → Secrets and variables → Actions):

| Secret | What it is |
| --- | --- |
| `KEYSTORE_BASE64` | The keystore file, base64-encoded |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Keep an offline copy of the keystore and passwords. If the key is lost, updates can't install over the app: you'd uninstall, install the new build, and restore from backup.

## Code map

| Area | Where |
| --- | --- |
| Data (Room), backup triggers | `app/src/main/java/.../data/` |
| Recurring schedules (+ tests) | `.../recurring/Recurrence.kt`, `app/src/test/...` |
| Backup file (Storage Access Framework) | `.../backup/` |
| Daily background job | `.../work/DailyWorker.kt` |
| Home-screen widget (Glance) | `.../widget/` |
| Screens (Compose) | `.../ui/` |
| In-app updates | `.../update/Updater.kt` |
| Build & release | `.github/workflows/build.yml` |

Categories are set in `data/Entities.kt` (`CATEGORIES`).

# Releasing

Releases are built and published by GitHub Actions ([`.github/workflows/release.yml`](../.github/workflows/release.yml)) when you push a version tag.
Users then download the files from the [Releases page](https://github.com/zibirik/streamdeck/releases).

## One-time setup

### 1. Android signing key

Android users can only update an app if every release is signed with the **same** key. Create it once:

```powershell
.\tools\create-keystore.ps1
```

The script creates `%USERPROFILE%\streampanel-release.jks` (outside the repo), copies the base64 of the keystore to your clipboard and
prints the passwords. **Back up the `.jks` file and the password** (password manager / offline copy). If you lose them you can never ship an
update that installs over existing installs.

Add four repository secrets (GitHub → Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `ANDROID_KEYSTORE_BASE64` | base64 of the `.jks` (script put it on the clipboard) |
| `ANDROID_KEYSTORE_PASSWORD` | the password printed by the script |
| `ANDROID_KEY_PASSWORD` | same password |
| `ANDROID_KEY_ALIAS` | `streampanel` |

The workflow refuses to publish when the secrets are missing or when the APK ends up signed with the debug key.

### 2. Repository settings (recommended)

- Settings → General: set a description, topics (`android`, `streamdeck`, `obs`, `kotlin`, `dotnet`) and upload a social preview image.
- Settings → Code security: enable **Private vulnerability reporting** (used by `SECURITY.md`) and Dependabot alerts.
- Settings → Branches: protect `main` and require the **CI** workflow.

## Publishing a release

1. Make sure CI is green on `main`.
2. In `CHANGELOG.md`, rename `[Unreleased]` to the new version and date, and add a fresh empty `[Unreleased]` section on top.
3. Commit, then tag and push:

   ```powershell
   git tag v1.0.0
   git push origin main --tags
   ```

   Use `v1.0.0-beta.1` style tags for pre-releases (they are marked as pre-release automatically).
4. The workflow runs the server tests, builds the signed APK, verifies its signature, publishes the self-contained Windows server and
   attaches to the release:
   - `StreamPanel-vX.Y.Z.apk`
   - `StreamPanel-Server-vX.Y.Z-win-x64.zip`
   - `SHA256SUMS.txt`
5. Open the release, check the notes and install the APK + server on a real tablet and PC once.

The Android `versionName` comes from the tag; `versionCode` is `100 + run number`, so it only ever grows.

## Building the release files locally

```powershell
$env:STREAMPANEL_KEYSTORE_FILE     = "$env:USERPROFILE\streampanel-release.jks"
$env:STREAMPANEL_KEYSTORE_PASSWORD = "<password>"
$env:STREAMPANEL_KEY_ALIAS         = "streampanel"
$env:STREAMPANEL_KEY_PASSWORD      = "<password>"
.\tools\package-release.ps1 -Version 1.0.0
```

Output is written to `dist\release\`. Without the keystore variables the APK is signed with the debug key — fine for testing, never for publishing.

## Later: removing the SmartScreen warning

The Windows server executable is not code-signed, so SmartScreen shows a warning on first run. A code-signing certificate (OV/EV, or Azure Trusted Signing)
removes it; sign `StreamPanel.Server.exe` in `tools/package-release.ps1` before zipping.

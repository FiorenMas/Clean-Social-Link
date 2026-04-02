<h1 align="center">How To Build APK With GitHub Actions</h1>

This project uses GitHub Actions workflow:

- `.github/workflows/build.yml`

It builds a **release APK** and uploads it as artifact.

## 1. Prepare Required Secrets

Go to:

- `GitHub Repository -> Settings -> Secrets and variables -> Actions -> New repository secret`

Add these secrets:

1. `KEYSTORE_BASE64`
2. `KEY_STORE_PASSWORD`
3. `KEY_PASSWORD`
4. `ALIAS`

### Generate `KEYSTORE_BASE64`

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/keystore.jks"))
```

Linux/macOS:

```bash
base64 -w 0 app/keystore.jks
```

## 2. Trigger Build

Workflow triggers:

1. Open `GitHub Repository -> Actions`
2. Choose workflow **Build APK**
3. Click **Run workflow**
4. Select branch and run

## 4. Download APK Artifact

After workflow is complete:

1. Open the workflow run in **Actions**
2. In **Artifacts**, download `clean-social-link`
3. Extract ZIP, APK file is:

```text
clean-social-link.apk
```
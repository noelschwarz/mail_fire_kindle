# Setup Guide

This guide walks you through configuring the Mail Fire Kindle app for your own use.

## Prerequisites

- Android Studio (Arctic Fox or later)
- JDK 17
- An Azure account (free tier works)

## Step 1: Azure App Registration

### 1.1 Create the App

1. Go to [Azure Portal](https://portal.azure.com/)
2. Search for "App registrations" in the top search bar
3. Click **+ New registration**
4. Configure:
   - **Name**: `Mail Fire Kindle`
   - **Supported account types**: **Personal Microsoft accounts only**
   - **Redirect URI**: Leave empty for now
5. Click **Register**
6. **Copy the Application (client) ID** - you'll need this!

### 1.2 Configure Authentication

1. In your app, go to **Authentication** (left sidebar)
2. Click **+ Add a platform**
3. Select **Mobile and desktop applications**
4. Under "Custom redirect URIs", add:
   ```
   msauth://com.mailfirekindle.app/YOUR_SIGNATURE_HASH
   ```
   (See Step 2 for getting your signature hash)
5. Scroll down and enable **"Allow public client flows"** → **Yes**
6. Click **Save**

### 1.3 Add API Permissions

1. Go to **API permissions** (left sidebar)
2. Click **+ Add a permission**
3. Select **Microsoft Graph** → **Delegated permissions**
4. Add these permissions:
   - ✅ `User.Read`
   - ✅ `Mail.Read`
   - ✅ `Mail.Send`
   - ✅ `offline_access`
5. Click **Add permissions**

## Step 2: Get Your Signature Hash

### For Debug Builds (Development)

**Mac/Linux:**
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
```

**Windows (PowerShell):**
```powershell
keytool -exportcert -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" | openssl sha1 -binary | openssl base64
```

Password: `android`

### For Release Builds (Production)

```bash
keytool -exportcert -alias YOUR_KEY_ALIAS -keystore path/to/your-release.keystore | openssl sha1 -binary | openssl base64
```

### URL-Encode the Hash

For the Azure redirect URI configuration:
- Replace `+` with `%2B`
- Replace `/` with `%2F`  
- Replace `=` with `%3D`

Example:
- Raw: `aBcDeFg+HiJ/KlM=`
- Encoded: `aBcDeFg%2BHiJ%2FKlM%3D`

## Step 3: Configure the App

### 3.1 Update MSAL Config

Edit `app/src/main/res/raw/auth_config_single_account.json`:

```json
{
  "client_id": "YOUR-CLIENT-ID-FROM-AZURE",
  "authorization_user_agent": "BROWSER",
  "redirect_uri": "msauth://com.mailfirekindle.app/YOUR_URL_ENCODED_HASH",
  "account_mode": "SINGLE",
  "broker_redirect_uri_registered": false,
  "authorities": [
    {
      "type": "AAD",
      "authority_url": "https://login.microsoftonline.com/consumers"
    }
  ]
}
```

### 3.2 Update AndroidManifest

Edit `app/src/main/AndroidManifest.xml`, find the `BrowserTabActivity` and update:

```xml
<data
    android:host="com.mailfirekindle.app"
    android:path="/YOUR_RAW_SIGNATURE_HASH"
    android:scheme="msauth" />
```

**Note:** Use the RAW hash here (with `/` prefix), NOT URL-encoded.

### 3.3 Update Allowed Email (Optional)

Edit `app/src/main/java/com/mailfirekindle/app/AppConfig.kt`:

```kotlin
const val ALLOWED_EMAIL = "your.email@outlook.com"
```

## Step 4: Build & Run

### Using Android Studio

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Connect your device (or use emulator)
4. Click **Run** ▶️

### Using Command Line

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Step 5: Install on Fire Kindle

### Enable Developer Options

1. Go to **Settings** → **Device Options**
2. Tap **Serial Number** 7 times
3. "Developer Options" will appear

### Enable ADB & Unknown Sources

1. Go to **Settings** → **Device Options** → **Developer Options**
2. Enable **ADB debugging**
3. Go to **Settings** → **Security & Privacy**
4. Enable **Apps from Unknown Sources**

### Install via ADB

```bash
adb install app-debug.apk
```

Or transfer the APK file and open it on the device.

## Troubleshooting

### "Redirect URI mismatch" Error

- Verify your signature hash is correct
- Check that the hash in `auth_config_single_account.json` is URL-encoded
- Check that the hash in `AndroidManifest.xml` is NOT URL-encoded (raw)
- Ensure the redirect URI in Azure matches exactly

### "Only [email] is allowed to sign in"

This is intentional! The app is locked to a specific email. Change `ALLOWED_EMAIL` in `AppConfig.kt` if needed.

### App Crashes on Launch

- Check Logcat for errors
- Verify `auth_config_single_account.json` is valid JSON
- Ensure all placeholders have been replaced

### Can't Connect on Fire Kindle

- Ensure WiFi is connected
- Check that the device's date/time is correct (SSL requires accurate time)
- Try clearing app data and signing in again

## GitHub Actions (For Maintainers)

To enable automated signed releases:

1. Generate a release keystore:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Convert to Base64:
   ```bash
   base64 -i release.keystore -o release.keystore.base64
   ```

3. Add GitHub Secrets:
   - `SIGNING_KEY`: Contents of `release.keystore.base64`
   - `KEY_ALIAS`: `release` (or your alias)
   - `KEY_STORE_PASSWORD`: Your keystore password
   - `KEY_PASSWORD`: Your key password

4. Create a release by pushing a tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```


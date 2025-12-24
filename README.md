# 📧 Mail Fire Kindle

[![Build APK](https://github.com/noelschwarz/mail_fire_kindle/actions/workflows/build.yml/badge.svg)](https://github.com/noelschwarz/mail_fire_kindle/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-5.1%2B-green.svg)](https://developer.android.com)

A minimal Android email client for **Amazon Fire Kindle** devices that connects to a Microsoft personal account (Outlook/Hotmail) using OAuth 2.0 and Microsoft Graph API.

## 📥 Download

### Direct Download Links

| Version | Download |
|---------|----------|
| **Debug APK v1.1.0** (Recommended) | [⬇️ Download Debug APK](https://github.com/noelschwarz/mail_fire_kindle/releases/download/v1.1.0/app-debug.apk) |
| **Release APK v1.1.0** | [⬇️ Download Release APK](https://github.com/noelschwarz/mail_fire_kindle/releases/download/v1.1.0/app-release-unsigned.apk) |

### Latest Release
**[📦 View All Releases](https://github.com/noelschwarz/mail_fire_kindle/releases/latest)**

> **Note:** Use the Debug APK for Fire Kindle devices - it's pre-signed and ready to install.

### From Actions (Development Builds)
1. Go to [Actions](https://github.com/noelschwarz/mail_fire_kindle/actions)
2. Click the latest successful build
3. Download the `app-debug` artifact

## ✨ Features

- 🔐 **Secure OAuth Login** - Uses Microsoft's official MSAL library
- 📬 **Inbox View** - Browse up to 1000 emails with pagination
- 📖 **Read Emails** - View full message content
- ✉️ **Compose & Send** - Create and send new emails
- 🔒 **Single Account Lock** - Restricted to one specific email account
- 📱 **Fire Kindle Compatible** - Works on Fire OS 5+ (Android 5.1+)
- 📜 **Load More** - Progressively load older emails (50 at a time)

## 📱 Screenshots

| Sign In | Inbox | Compose |
|---------|-------|---------|
| ![Sign In](docs/screenshot_signin.png) | ![Inbox](docs/screenshot_inbox.png) | ![Compose](docs/screenshot_compose.png) |

## 🚀 Installation on Fire Kindle

### Prerequisites
1. Enable **Developer Options**: Settings → Device Options → tap Serial Number 7 times
2. Enable **Apps from Unknown Sources**: Settings → Security → Apps from Unknown Sources

### Install Methods

**Method 1: Direct Download**
1. Download the APK on your Kindle
2. Open the file and tap "Install"

**Method 2: ADB**
```bash
adb install mail-fire-kindle.apk
```

**Method 3: Transfer via USB**
1. Connect Kindle to computer
2. Copy APK to Downloads folder
3. Use a file manager to install

## 🔧 Configuration

> ⚠️ **This app requires configuration before use!**

The app requires an Azure App Registration to work. See [SETUP.md](SETUP.md) for detailed instructions.

### Quick Setup

1. **Create Azure App Registration** ([portal.azure.com](https://portal.azure.com))
   - Account type: Personal Microsoft accounts only
   - Platform: Mobile and desktop applications
   - Redirect URI: `msauth://com.mailfirekindle.app/<SIGNATURE_HASH>`

2. **Configure the app** - Edit these files:
   - `app/src/main/res/raw/auth_config_single_account.json` → Add your Client ID
   - `app/src/main/AndroidManifest.xml` → Add your signature hash
   - `app/src/main/java/com/mailfirekindle/app/AppConfig.kt` → Set allowed email

3. **Build & Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 🏗️ Building from Source

### Prerequisites
- Android Studio Arctic Fox+
- JDK 17
- Android SDK 34

### Build Commands

```bash
# Clone the repository
git clone https://github.com/noelschwarz/mail_fire_kindle.git
cd mail_fire_kindle

# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned)
./gradlew assembleRelease
```

### Output Locations
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 📁 Project Structure

```
mail_fire_kindle/
├── app/src/main/
│   ├── java/com/mailfirekindle/app/
│   │   ├── auth/AuthManager.kt      # MSAL authentication
│   │   ├── data/GraphClient.kt      # Microsoft Graph API
│   │   ├── data/Models.kt           # Data classes
│   │   └── ui/                      # Activities & adapters
│   └── res/
│       ├── raw/auth_config_single_account.json  # ⚠️ Configure this!
│       └── layout/                  # XML layouts
├── .github/workflows/build.yml      # CI/CD pipeline
├── SETUP.md                         # Detailed setup guide
└── README.md                        # This file
```

## 🔐 Security

- ✅ OAuth 2.0 with PKCE (no client secret in app)
- ✅ Tokens stored via MSAL's secure cache
- ✅ Single-account restriction prevents unauthorized use
- ✅ No passwords stored
- ✅ HTTPS only

### What's Safe to Publish

| Safe ✅ | Keep Private ❌ |
|---------|-----------------|
| Source code | Keystore files (`.jks`, `.keystore`) |
| Client ID | Keystore passwords |
| Signature hash | `local.properties` |

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Microsoft Authentication Library (MSAL)](https://github.com/AzureAD/microsoft-authentication-library-for-android)
- [Microsoft Graph API](https://docs.microsoft.com/en-us/graph/)
- [OkHttp](https://square.github.io/okhttp/)

## 📞 Support

- 📖 [Setup Guide](SETUP.md)
- 🐛 [Report Issues](https://github.com/noelschwarz/mail_fire_kindle/issues)
- 💬 [Discussions](https://github.com/noelschwarz/mail_fire_kindle/discussions)

---

<p align="center">
  Made with ❤️ for Amazon Fire Kindle devices
</p>

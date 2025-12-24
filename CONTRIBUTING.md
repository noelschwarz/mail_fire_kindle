# Contributing to Mail Fire Kindle

Thank you for your interest in contributing! This document provides guidelines and information for contributors.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment (see [SETUP.md](SETUP.md))
4. Create a feature branch

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API 34

### Building

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/mail_fire_kindle.git
cd mail_fire_kindle

# Build debug APK
./gradlew assembleDebug
```

### Testing on Device

1. Connect a Fire Kindle device or Android emulator
2. Enable USB debugging
3. Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions small and focused

## Commit Messages

Use clear, descriptive commit messages:

```
feat: Add message search functionality
fix: Resolve token refresh loop issue
docs: Update setup instructions
refactor: Extract auth logic to separate class
```

## Pull Request Process

1. Update documentation if needed
2. Ensure the build passes (`./gradlew assembleDebug`)
3. Test on a real Fire Kindle device if possible
4. Create a pull request with a clear description

## Reporting Issues

When reporting bugs, please include:

- Device model and Fire OS version
- Steps to reproduce
- Expected vs actual behavior
- Relevant log output (if available)

## Feature Requests

Feature requests are welcome! Please:

- Check existing issues first
- Describe the use case
- Explain why it would benefit users

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow

## License

By contributing, you agree that your contributions will be licensed under the MIT License.


# TempBox

**Disposable email for Android — instant inboxes, zero sign-up.**

TempBox gives you throwaway email addresses in one tap. Use them for app sign-ups, verification codes, and anything you do not want tied to your real inbox. Addresses expire after 7 days and the app handles renewal automatically.

---

## Why TempBox?

Most disposable-email apps feel clunky or buried in ads. TempBox is built to be fast: generate an address, copy it, and watch messages arrive in real time. No account, no phone number, no personal data.

| | |
|---|---|
| **Instant setup** | Working inbox in one tap |
| **Private by design** | No registration or identity required |
| **Auto-refresh** | New mail appears within seconds |
| **Smart expiry** | 7-day lifespan with automatic renewal |
| **Up to 3 inboxes** | Run multiple addresses at once |

---

## Features

### Core

- **One-tap address generation** — create a working `@mail.tm` inbox instantly
- **Live inbox polling** — messages refresh automatically every few seconds
- **Multiple inboxes** — manage up to 3 disposable addresses simultaneously
- **Custom prefix** — personalize the local part of your generated address
- **7-day expiry** — addresses self-destruct; renew before they expire
- **Message detail view** — read full HTML and plain-text email bodies

### Convenience

- **OTP detection** — verification codes are surfaced and copyable in one tap
- **Autofill service** — suggest your temp address inside other apps (Android 8+)
- **Home screen widget** — see your active inbox and latest mail at a glance
- **Push notifications** — get alerted when new mail arrives (Android 13+ permission prompt)
- **Quick copy** — copy your full address to the clipboard from anywhere in the app

### Appearance

- **Dark-first UI** — neon-green accent on a cyber-dark theme
- **Theme control** — follow system light/dark mode or pick manually in Settings
- **Onboarding flow** — first-run walkthrough for new users

---

## Screens

| Home | Inbox | Message |
|------|-------|---------|
| Active address, expiry countdown, recent mail preview | Full message list with sender and subject | OTP card, HTML body, copy actions |

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | ViewModel, LiveData, Navigation Compose |
| Networking | Retrofit 2 + OkHttp |
| Email API | [mail.tm](https://mail.tm) (Hydra/JSON-LD) |
| Background work | WorkManager |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Getting started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 37

### Debug build

```bash
git clone https://github.com/stewiriffin/Temporary-email.git
cd Temporary-email
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

### Release build

Create `keystore.properties` in the project root:

```properties
storeFile=path/to/your-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

Then:

```bash
./gradlew :app:assembleRelease
```

> `keystore.properties` and `*.jks` files are gitignored and must never be committed.

---

## Project structure

```
app/src/main/java/com/rank/tempbox/
├── MainActivity.kt          # Root navigation (Home / Inbox / Settings)
├── MainViewModel.kt         # Inbox state, API calls, prefs
├── ApiService.kt            # mail.tm REST interface
├── ui/screens/              # Compose screens
├── ui/components/           # Shared UI (header, logo)
├── ui/theme/                # Colors, typography, cyber theme
├── ads/                     # Start.io banner & interstitial integration
├── ActiveInboxWidgetProvider.kt
├── TempBoxAutofillService.kt
└── MailboxCleanupWorker.kt  # Expired inbox cleanup
```

---

## Privacy

- TempBox does not require or store personal information.
- Email data lives only on the mail.tm service for the lifetime of the address.
- Location permissions from ad SDK dependencies are explicitly stripped in the manifest.
- Internet access is required solely for fetching mail and serving ads.

---

## License

This project is provided as-is for personal and educational use. See the repository owner for licensing questions.

---

<p align="center">
  <sub>Built with Kotlin & Jetpack Compose</sub>
</p>

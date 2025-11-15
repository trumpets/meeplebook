# MeepleBook
*A modern, native Android app for browsing and managing your BoardGameGeek collection.*

<p align="left">
    <a href="https://boardgamegeek.com" target="_blank">
      <img src="assets/powered_by_BGG.png" alt="Powered by BGG" height="40"/>
    </a>
</p>

## 🎲 Overview
**MeepleBook** is a modern Android app built with **Kotlin**, **Jetpack Compose**, **Hilt**, and a focus on **clean architecture**, **testability**, and **future-proofing**.  

It allows BoardGameGeek (BGG) users to:

- Log in with their BGG credentials
- Browse their collection
- Track and view plays
- Record new plays
- Enjoy a sleek, minimalist, modern UI
- Benefit from thorough automated testing for long-term maintainability

---

## 🚀 Features (Milestone 1 & Beyond)

### ✔️ Current / Planned Core Features
- 🔐 **BGG Login** (XML API)
- 📚 **View User’s Collection**
- 🧩 **View User’s Plays**
- ✍️ **Record Play Sessions**
- 🌙 **Material You + Compose modern UI**
- 🧪 **Full Test Coverage** (UI tests, unit tests, integration tests)
- 🔧 **Robust architecture** with clear data/domain/ui separation

[//]: # (- 📱 **Google Play distribution**)

---

## 🏛️ Architecture
MeepleBook is designed for long-term maintainability:

- **Kotlin-first**
- **Jetpack Compose** UI
- **Hilt** for DI
- **Coroutines + Flow**
- **Retrofit/OkHttp** for BGG XML API
- **Room** for local cache
- **TDD where applicable**
- **Clean Architecture** with layered modules

---

## 🧪 Testing Strategy
- Unit tests for view models, repositories, and domain logic
- MockWebServer integration tests
- Compose UI tests
- Optional snapshot tests
- CI pipeline for automated testing (future milestone)

---

## 🧩 Powered by BoardGameGeek
MeepleBook uses the **BoardGameGeek XML API2** for all collection and play data.

<p align="left">
    <a href="https://boardgamegeek.com" target="_blank">
      <img src="assets/powered_by_BGG.png" alt="Powered by BGG" height="40"/>
    </a>
</p>

---

## 📦 Package Name

`app.meeplebook`


---

## 📱 Minimum Requirements
- **Android 8.0 (API 26)** or newer (tentative)
- Internet connection
- Valid BGG account

---

## 🛠️ Development Setup

Clone the repo:

```bash
git clone https://github.com/yourusername/meeplebook.git
cd meeplebook
```

Open with Android Studio Ladybug or newer.

```bash
./gradlew assembleDebug
```

```bash
./gradlew test
./gradlew connectedAndroidTest
```

---

## 🙌 Contributing

(Coming soon) PRs, bug reports, and suggestions will be welcome as MeepleBook expands.

---

## 📄 License

This project is released under the **MIT License**.
See the [LICENSE](LICENSE) file for details.
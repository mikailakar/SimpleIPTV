# SimpleIPTV 📺

SimpleIPTV is a modern, lightweight, and feature-rich IPTV player for Android, built entirely with Kotlin and Jetpack Compose. It is designed to provide a premium, ad-free viewing experience with advanced playback controls and playlist management.

> **⚠️ LEGAL DISCLAIMER:** 
> SimpleIPTV is strictly a media player application. It does not provide, include, or sell any playlists, streams, or content subscriptions. The user must provide their own content.

## ✨ Features

* **Lightweight & Smooth:** Highly optimized for fast performance, quick loading times, and a fluid, responsive user experience.
* **Adaptive Layout:** Fully supports both portrait and landscape orientations for seamless navigation and viewing.
* **Multi-Format Support:** Easily load playlists via M3U URLs or Xtream Codes API.
* **Smart Parsing & Caching:** Channels and categories are parsed locally and cached using binary serialization for lightning-fast loading on subsequent app launches.
* **Advanced Video Player:** Powered by Google's Media3 (ExoPlayer), featuring:
  * Native Picture-in-Picture (PiP) mode with play/pause remote actions.
  * On-screen vertical sliders for Brightness and Volume control.
  * Real-time video statistics (Resolution, FPS).
  * Audio track and Subtitle selection.
  * Aspect ratio toggling (Fit, Fill, Stretch).
* **Live EPG (Electronic Program Guide):** View current and upcoming programs for Xtream-based playlists with a sleek UI.
* **Deep Customization:**
  * Reorder categories and favorite channels to your exact preference.
  * Hide unwanted categories from your dashboard.
  * Multiple sorting options (Default, A-Z, Z-A, Custom).
  * Toggle between "Dark" and "Pure Black" themes.
* **Favorites System:** Save channels to a dedicated favorites list with custom ordering.

## 📸 Screenshots

`![Dashboard](images/dashboard.png)`

## 🛠️ Tech Stack

This project uses modern Android development standards:
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Media Player:** AndroidX Media3 (ExoPlayer)
* **Image Loading:** Coil (Compose)
* **Asynchronous Operations:** Kotlin Coroutines & Dispatchers

## 🚀 How to Build and Run

1. Clone the repository:
   `git clone https://github.com/mikailakar/SimpleIPTV.git`
2. Open the project in **Android Studio**.
3. Let Gradle sync the dependencies.
4. Build and run the app on an Android emulator or physical device running Android 8.0 (API 26) or higher.

## 🤝 Contributing

This project is open-source! If you find a bug, want to add a feature, or improve the UI:
1. Fork the repository.
2. Create a new branch for your feature (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📄 License

This project is open-source. It is built by the community, for the community.
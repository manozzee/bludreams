<div align="center">

  # BluDreams

  ### *Cinema, uninterrupted. Crafted with Liquid Glass.*

  A modern, ad-free Android streaming shell built on open-source foundations—featuring optical **Liquid Glass** surfaces, **Plus Jakarta Sans & Inter** typography, a full Compose video player HUD, and curated cinema palettes.

  <p align="center">
    <a href="https://bludreams.golposolpo.com"><img src="https://img.shields.io/badge/Official%20Website-bludreams.golposolpo.com-2563EB?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Official Website" /></a>
    <a href="https://github.com/manozzee/bludreams/releases/latest"><img src="https://img.shields.io/github/v/release/manozzee/bludreams?style=for-the-badge&color=1D4ED8&logo=android&logoColor=white" alt="Latest Release" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-10B981?style=for-the-badge" alt="License: GPL-3.0" /></a>
    <a href="https://github.com/recloudstream/cloudstream"><img src="https://img.shields.io/badge/Based%20on-CloudStream-F59E0B?style=for-the-badge&logo=github" alt="Based on CloudStream" /></a>
  </p>

  <p align="center">
    <a href="https://bludreams.golposolpo.com"><b>🌐 Visit Official Website</b></a> •
    <a href="https://github.com/manozzee/bludreams/releases/latest"><b>⚡ Download APK</b></a> •
    <a href="#-the-experience"><b>Experience</b></a> •
    <a href="#-playback--player-hud"><b>Player</b></a> •
    <a href="#-visual-design--themes"><b>Design</b></a> •
    <a href="#-installation"><b>Install</b></a>
  </p>

</div>

---

## ✨ The Experience

BluDreams removes clutter so your shows and movies stay front and center—zero ads, zero tracking, and full compatibility with standard CloudStream extension repositories.

- 🫧 **Optical Liquid Glass UI**: Real-time backdrop refraction and specular glass rims power the floating jelly-motion navigation pill, hero headers, update capsule, and player overlays.
- 🎬 **Immersive Home & Details**: Full-bleed hero carousels with seamless transparent dissolves, inline trailer playback, quick sharing, and streamlined *Continue Watching* cards.
- 👤 **Multi-Profile Switching**: A sleek *"Who's Watching?"* selector with bundled character avatars, custom avatar URLs, optional PIN lock, and your active profile avatar displayed right in the navigation bar.
- 🔄 **Non-Blocking Update Capsule**: Automatic startup checks surface a floating Liquid Glass capsule at the top of the screen with liquid download progress, release notes, and one-tap PackageInstaller / Legacy APK installation.
- 📦 **Extension & Offline Ready**: Manage extension repositories, customize poster card dimensions and depth effects, and download episodes for offline playback.

---

## 🎮 Playback & Player HUD

Every player control and overlay has been rebuilt in Jetpack Compose with matching Kyant Liquid Glass surfaces:

- **Full Glass Control Suite**: Edge-to-edge **Episodes Drawer** (season, dub, range & sort filters), **Sources & Subtitles**, **Video & Audio Tracks**, and **Subtitle Delay / Sync**.
- **Audio & Speed Tuning**: Centered Liquid Glass cards for **Playback Speed** and **Dynamic Range Compressor (DRC)** for clearer dialogue and balanced action scenes.
- **Natural Touch Gestures**: Slide for brightness or volume, double-tap to seek, hold for `2x` speed-up, and scrub with properly proportioned **Seek Bar Preview** thumbnails.
- **Subtitle & Stream Tools**: Online subtitle search, custom caption styling, quality priority profiles, live stream edge tracking, and torrent progress/confirmation overlays.

---

## 🎨 Visual Design & Themes

- **Typography**: Clean editorial hierarchy pairing **Plus Jakarta Sans** headlines with **Inter** body text and tabular playback timers.
- **AMOLED & Neutral Dark**: Deep `#000000` pure black AMOLED mode alongside elevated dark charcoal surfaces.
- **7 Curated Cinema Palettes**: **Velvet Noir**, **Aura Brass**, **Smoked Cognac**, **Obsidian Pine**, **Fincher Teal**, **Midnight Lapis**, and **Nocturne Violet**—plus **Dynamic Monet (Material You)** on Android 12+.
- **Switchable App Icons**: Choose between custom launcher icons (**Nami** or **Reze**) directly inside **Settings → UI**.

---

## 📱 Supported Form Factors

- **Android Phones**: Primary, fully refined touch and Liquid Glass experience (Android 6.0 / API 23+).
- **Tablets & Android TV *(Experimental)***: Adaptive layouts and D-pad leanback navigation are included and continue to evolve.

---

## 🚀 Installation

1. Visit **[bludreams.golposolpo.com](https://bludreams.golposolpo.com)** or download the latest **`BluDreams-v*.apk`** from **[GitHub Releases](https://github.com/manozzee/bludreams/releases/latest)**.
2. Install the APK on your Android device *(grant "Install unknown apps" permission if prompted)*.
3. Open **BluDreams**, go to **Settings → Extensions**, and add your preferred CloudStream repository URL to install providers.

---

## 🛠️ Build from Source

```bash
git clone https://github.com/manozzee/bludreams.git
cd bludreams

# Debug build
./gradlew :app:assembleStableDebug

# Release build (uses debug signing locally unless keystore.properties or SIGNING_* env vars are set)
./gradlew :app:assembleStableRelease
```

---

## ⚠️ Disclaimer

**BluDreams does not host, provide, index, or distribute any media content or video streams.**  
It is strictly an open-source media player and interface. Users are solely responsible for any third-party extensions they install and for complying with applicable local laws.

---

## 📄 License & Credits

- Released under the **[GNU General Public License v3.0 (GPL-3.0)](LICENSE)**.
- Built on the open-source architecture of **[CloudStream](https://github.com/recloudstream/cloudstream)**.
- Liquid Glass backdrop refraction powered by **[Kyant Backdrop & Shapes](https://github.com/Kyant0/AndroidLiquidGlass)** and **[Haze](https://github.com/chrisbanes/haze)**.

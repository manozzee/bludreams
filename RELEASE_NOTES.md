## BluDreams v1.1.2

A major visual, playback, and reliability upgrade—bringing **Kyant Liquid Glass** across the video player and in-app updater, a new **Plus Jakarta Sans + Inter** typographic system, redesigned profile avatars, and hardened release APK signing.

---

### ✨ New Features & Redesigns

- **Kyant Liquid Glass Video Player HUD**: Rebuilt the entire phone player overlay in Jetpack Compose with real-time optical backdrop refraction, specular glass rims, and dimmed video scrims across every dialog and drawer:
  - **Episodes Drawer** with season, dub, range, and sort selectors
  - **Sources & Subtitles** and **Video & Audio Tracks** two-pane selectors
  - **Playback Speed** and **Dynamic Range Compressor (DRC)** floating glass cards
  - **Subtitle Delay / Sync** two-pane cue previewer
  - **Quality Profiles**, **Online Subtitles Search**, **Subtitle Style Settings**, **Mirror Selection**, and **Torrent Confirmation** dialogs
  - **Liquid Glass Loading Overlay** with floating Back and Skip controls
- **Floating Liquid Glass Update Capsule**: Replaced blocking center update dialogs with a non-blocking top **Kyant Liquid Glass** capsule featuring a smooth left-to-right liquid water-flow download progress fill, an expandable Markdown changelog sheet, and one-tap **Skip** / **Install** / **Retry** actions.
- **Redesigned Profiles & Bundled Avatars**: Added a full-screen *"Who's Watching?"* profile selector with bundled character avatars (`Luffy`, `Nami`, `Niko Robin`, `Yamato`), custom avatar URL support, inline PIN verification for locked profiles, and your active profile avatar rendered directly in the floating bottom navigation bar.
- **Fresh Typography System**: Replaced Outfit and Figtree with **Plus Jakarta Sans** (display, titles, and labels) and **Inter** (body copy and tabular-numeral timers) across the app and shared Compose resources.

---

### 🎨 UI & Experience Refinements

- **Floating Navigation Bar Polish**: Switched the floating nav pill to adaptive content-wrapping width with tighter vertical height, solid accent selection pill, and improved RTL/touch hit-testing.
- **Home & Details Visuals**:
  - Updated `DetailHero` to dissolve smoothly into transparency using `BlendMode.DstIn`—eliminating solid-color seam strips.
  - Added a direct **Share** button to the Home Hero carousel.
  - Streamlined **Continue Watching** cards with compact 170dp sizing, direct **Play** and **Remove** actions, and removed legacy long-press popups across Home, Details, and Search.
- **Seek Bar Thumbnail Proportions**: Fixed seek bar preview frame scaling and container aspect ratio so preview thumbnails no longer appear squished into a thin horizontal strip.
- **Settings & Branding Polish**:
  - Updated the Wiki / Documentation link in General Settings to **`https://bludreams.golposolpo.com/`** and cleaned up unused upstream links.
  - Simplified the `AMOLED` toggle to apply theme and navigation bar styling immediately without forcing a full activity recreation.
  - Replaced legacy bitmap profile backgrounds in Quality Profiles and TV error fallbacks with clean programmatic gradients and drawables.
  - Added custom switchable launcher icons (**Nami** default and **Reze**) in **Settings → UI** *(introduced in the `v1.1.0` polish cycle)*.

---

### 🐛 Bug Fixes & Reliability

- **In-App Updater & APK Installation Fixes**:
  - Fixed startup update detection (`GithubViewModel.isNewerVersion`) using proper semantic version comparison instead of brittle exact-string equality, preventing duplicate auto-checks per session.
  - Wired `PackageInstallerStatusReceiver` events (`STATUS_PENDING_USER_ACTION`, `STATUS_SUCCESS`, `STATUS_FAILURE_ABORTED`, and failure messages) directly to `ApkUpdater` so both **PackageInstaller** and **Legacy** APK installs report cancellations, unknown-sources permission prompts, and errors cleanly.
  - Safely handled releases with empty or `null` changelog bodies in `GithubReleases`.
- **Homepage Source Reloading**: Fixed `HomeFragment` and `HomeViewModel` checks so switching or refreshing the configured homepage source accurately reloads the selected provider.
- **Player & Live Stream Sync**: Added `getBufferedPosition()`, `isAtLiveEdge()`, and Compose HUD callbacks across `IPlayer`, `CS3IPlayer`, `PlayerView`, and `PlayerGestureHelper` for responsive live-edge detection, torrent download progress, double-tap seek, volume, and brightness overlays.

---

### 🔧 Build & Release Pipeline

- **Permanent Release APK Signing**: Configured strict CI release signing in `.github/workflows/release.yml` and `app/build.gradle.kts` with secret validation (`SIGNING_KEYSTORE_BASE64`, `SIGNING_KEY_ALIAS`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_PASSWORD`), `keytool` pre-build verification, `apksigner verify --verbose --print-certs` anti-debug-certificate enforcement, and automatic temporary keystore cleanup.
- **Safe Local Builds & Git Hygiene**: Added support for gitignored `keystore.properties` / environment variables while preserving automatic debug-signing fallback for local builds without keys, and expanded `.gitignore` to block all keystore and signing property files.
- **Bumped App Version**: Updated to **`v1.1.2`** (`versionCode = 2`).

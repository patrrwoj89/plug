# TV Hub Skeleton

A clean, modern Android TV application skeleton built with Jetpack Compose for TV. It demonstrates a realistic architecture for TV apps: D-Pad focus handling, sidebar navigation, themed design tokens, Hilt DI, and ExoPlayer integration — without any scraping, torrent, or unlicensed content modules.

## Features

- **TV-first UI** with `androidx.tv.material3`
- **D-Pad focus system** with scale, outline, and glow
- **Sidebar navigation** (Home, Search, Library, Watchlist, Settings) with Material Icons
- **Modern Compose + Hilt + ViewModel architecture**
- **ExoPlayer stub** in the player screen
- **Mock data** for immediate visual feedback
- **Version catalog** for dependency management

## Tech stack

- Android Gradle Plugin 8.7.2
- Gradle 8.9
- Kotlin 1.9.24
- Compose BOM 2024.11.00
- Jetpack Compose for TV (`tv-foundation`, `tv-material`)
- Hilt 2.51.1
- ExoPlayer / Media3 1.4.1
- Coil 2.7.0

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug
```

APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/tvhub/skeleton/
├── data/              # Mock data source
├── di/                # Hilt modules
├── model/             # Data classes
├── navigation/        # Routes and NavHost
├── ui/
│   ├── components/    # TVCard, FocusableSurface, Sidebar
│   ├── screens/       # Home, Search, Detail, Player, Settings, Library, Watchlist
│   ├── theme/         # Colors, Typography, Theme, Tokens
│   └── viewmodel/     # Hilt ViewModels
└── MainActivity.kt
```

## Next steps

1. Replace `MockDataSource` with your real repository and data sources.
2. Configure ExoPlayer in `PlayerScreen` to load real media URIs.
3. Add content providers (only licensed / authorized sources).
4. Customize `AppColor`, `AppTypography`, and `Spacing` tokens.
5. Add baseline profiles, R8 shrinking, and TV launcher recommendations.

## License

MIT License — see [LICENSE](LICENSE).

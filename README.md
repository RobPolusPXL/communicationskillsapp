# Comm Skills

Android-app om de woorden en grammatica van Communication Skills 2 (PXL) te leren.

- Analyse: [ANALYSE.md](ANALYSE.md)
- Ontwerp: [ONTWERP.md](ONTWERP.md)

## Bouwen
1. Open de map in Android Studio (de Gradle-sync haalt alles op).
2. Kies een toestel of emulator en run de `app`-module.

Stack: Kotlin, Jetpack Compose (Material 3), Navigation Compose. Package: `com.robpolus.commskills`.

## Werkwijze (git flow)
- `main`: werkende, goedgekeurde versie
- `develop`: integratie
- `feature/<naam>`: elk nieuw onderdeel, gemerged in `develop` na akkoord

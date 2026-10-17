# Comm Skills

Android-app om de woorden en grammatica van Communication Skills 2 (PXL) te leren.

- Analyse: [ANALYSE.md](ANALYSE.md)
- Ontwerp: [ONTWERP.md](ONTWERP.md)

## Bouwen
1. Open de map in Android Studio (de Gradle-sync haalt alles op).
2. Kies een toestel of emulator en run de `app`-module.
3. Tests: `./gradlew test` (of `gradle test`). Ze draaien ook automatisch op GitHub (Actions) bij elke push.

Stack: Kotlin, Jetpack Compose (Material 3), Navigation Compose. Package: `com.robpolus.commskills`.

De app werkt meteen, zonder account: de 7 woordenlijsten zitten ingebouwd (`app/src/main/assets/lists`) en
je voortgang wordt op het toestel bewaard. Online opslag en Google-login komen erbij zodra je Firebase instelt.

## Online opslag en Google-login (Firebase) instellen
Zonder het bestand `app/google-services.json` staat dit uit en werkt alles lokaal.

1. Ga naar de [Firebase-console](https://console.firebase.google.com) en maak een project (of gebruik dat van de maaltijdplanner).
2. Voeg een Android-app toe met pakketnaam `com.robpolus.commskills` en het **SHA-1** van je debug-keystore
   (`./gradlew signingReport`).
3. Zet bij *Authentication → Sign-in method* **Google** aan.
4. Maak bij *Firestore Database* een database en plak deze regels bij *Rules*:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
5. Download `google-services.json` en zet het in de map `app/`. Het staat in `.gitignore` en komt niet in de repo.
6. Bouw de app opnieuw. Bij de start verschijnt nu een inlogscherm; je kan ook "zonder account" kiezen.

De gegevens staan in één document per gebruiker (`users/<uid>/data/state`). Bij een conflict wint de laatste wijziging;
een nieuw toestel zonder eigen voortgang neemt altijd de cloud over.

## Werkwijze (git flow)
- `main`: werkende, goedgekeurde versie
- `develop`: integratie
- `feature/<naam>`: elk nieuw onderdeel, gemerged in `develop` na akkoord

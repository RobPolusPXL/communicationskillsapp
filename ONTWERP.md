# Ontwerp: Communication Skills 2 – leerapp

Bouwt voort op [ANALYSE.md](ANALYSE.md). Alles wat in de analyse beslist is, wordt hier niet herhaald. Dit document vult de punten in die Rob aan de ontwikkelaar overliet (§ 14 van de analyse). Zulke **voorstellen** zijn gemarkeerd met 🔶 en wachten op zijn akkoord.

## 1. Architectuur
- Android, Kotlin, Jetpack Compose, Gradle (zelfde basis als de maaltijdplanner; minSdk 24, targetSdk 36).
- **MVVM** met drie lagen: `ui` (Compose-schermen + ViewModels), `domain` (regels: antwoordcontrole, score, ontgrendeling, examen), `data` (Firebase, import, assets).
- **Google-login** via Firebase Auth. **Firestore** met offline-persistentie: online bewaard, maar werkt ook zonder internet en synchroniseert achteraf. Er is geen aparte lokale database nodig.
- Domeinlogica is zuiver Kotlin zonder Android-afhankelijkheden, zodat ze met gewone unit tests getest kan worden.

```
app/src/main/java/.../
  ui/        schermen, navigatie, thema
  domain/    AnswerChecker, Scoring, Unlocking, ExamEngine, SessionBuilder
  data/      Firestore-repositories, AuthRepository
  importer/  DocxListImporter
  notify/    ReminderScheduler
assets/grammar/   oefeningen als JSON (bundled, versiebeheerd)
```

## 2. Schermen en navigatie

```
Login ─► Home ─┬─► Vocabulary-route ─► Lijst kiezen/combineren ─► Oefening ─► Resultaat
               │                    └► Examen ─► Resultaat
               ├─► Grammar-route    ─► Type kiezen ─► Oefening ─► Resultaat
               │                    └► Examen ─► Resultaat
               ├─► Trofeeën
               └─► Instellingen (herinnering, lijst importeren, uitloggen)
```

| Scherm | Inhoud |
|---|---|
| **Home** | Twee routes (Vocabulary, Grammar), streak, dagdoel-voortgang (x/10 min) |
| **Route** | Niveaupad (Matchen → Typen; Grammar: één niveau). Vergrendelde niveaus met slotje. Lijsttegels in rood/groen/goud. Meerdere lijsten aanvinken om te combineren. Tegel "Gemixt" vergrendeld tot alle lijsten gespeeld zijn. Knop "Examen" actief vanaf 70%. |
| **Woordenoverzicht** | Per lijst alle woorden, per woord rood of groen |
| **Matchen** | 6 Engelse + 6 Nederlandse woorden, tikken om te verbinden, kleurt meteen groen/rood. Knop (i) toont de Engelse uitleg. |
| **Typen** | Prompt, invoerveld, controleren, feedback (juist / typo / "to"-melding / accentmelding / fout + juiste oplossing + uitleg) |
| **Grammar** | Zin met opening, ontbrekend stuk typen, feedback zoals bij Typen + korte uitleg van de regel |
| **Examen** | Voortgang (n/12 of n/21), 3 levens, zelfde oefenvorm als het niveau |
| **Resultaat** | Score, foute woorden met juiste oplossing, nieuwe trofeeën |
| **Trofeeën** | Alle trofeeën, behaalde in kleur, met tellers waar van toepassing |
| **Instellingen** | Herinneringsuur, lijst importeren, uitloggen |

Waarschuwingsdialoog bij het aanklikken van een vergrendeld niveau: "Ben je zeker? Je bent mogelijk nog niet klaar voor dit niveau." met Ja/Nee.

Stijl: witte achtergrond, rustige kleuren, grote duidelijke knoppen, eigen identiteit (geen kopie van Duolingo).

## 3. Datamodel (Firestore)

Alles onder de ingelogde gebruiker: `users/{uid}/…`

```
wordLists/{listId}
  title, source, createdAt
  words: [ { id, en, type, definition, nl: [..] } ]

progress/{listId}_{wordId}
  score (0..1)        voortschrijdend gemiddelde, zie § 5
  correct, wrong      totalen
  lastSeen            tijdstip
  played              ooit gespeeld (voor het ontgrendelen van "Gemixt")

levelState/{route}_{level}
  unlocked, examPassed, bestAverage

stats/streak            current, longest, lastActiveDate
stats/daily/{yyyy-MM-dd}  activeSeconds, exercises
trophies/{trophyId}     earnedAt, count
settings                reminderTime, reminderEnabled
```

- `en` bewaart het woord zoals in de lijst (bv. `to pursue`), `type` apart (`v`).
- `nl` is een lijst varianten; één juiste variant volstaat.
- De voortgang hangt aan het woord, niet aan de richting en niet aan de oefenvorm.
- Een lijst met ~170 woorden past ruim in één document.

**Grammar-oefeningen** (bundled JSON, `assets/grammar/conditionals.json`):

```json
{ "id": "c2-014", "type": 2,
  "text": "If I ___ rich and famous, I would help people in need.",
  "hint": "(to be)",
  "answers": ["were"],
  "explain": "Type 2: if + past simple, would + infinitief. Hypothetisch → 'were'." }
```

Nieuwe types of onderdelen later = nieuw JSON-bestand toevoegen.

## 4. Antwoordcontrole (domein)

Vergelijking in deze volgorde:

1. **Voorbewerking:** trim, meervoudige spaties samenvoegen, leestekens negeren, type-aanduiding `(v)`/`(n)` uit de referentie verwijderen.
2. **Hoofdletters:** vereist **alleen** als de referentie zelf een hoofdletter bevat (bv. *Monday*). Anders niet vergelijken op hoofdletters.
3. **Accenten:** negeren bij vergelijking; ontbreken ze, dan volgt enkel een melding.
4. **"to"/lidwoord (NL→EN):** met of zonder `to`, `a`, `an` is juist. Ontbreekt `to` bij een werkwoord, dan volgt de melding "Voor 100% correct: *to …*".
5. **Meerdere vertalingen:** juist als het antwoord overeenkomt met één variant.
6. **Typo's:** 🔶 precies één vervangen letter die op het **AZERTY**-toetsenbord naast de juiste ligt (z↔a, q↔s, …) is juist, met melding "Let op: typfout". Omgewisselde letters (`ao` i.p.v. `oa`) zijn fout. 🔶 Een ontbrekende of extra letter telt ook als fout (niet vermeld door Rob; strengste lezing).
7. Bij elk fout antwoord: altijd de juiste oplossing + de Engelse uitleg tonen.

Resultaattypes: `Correct`, `CorrectWithNotes(notes)`, `Wrong(correctAnswer)`. De notes zijn de meldingen (typo, accent, "to").

## 5. Voortgang en herhaling 🔶

**Woordscore (0..1):** voortschrijdend gemiddelde per woord, nieuw woord start op 0.
`score = 0,7 · score + 0,3 · uitslag` (uitslag = 1 bij juist, 0 bij fout).
Een correct-met-melding telt als juist.

**Kleur van een woord:** rood onder 0,5 · oranje 0,5–0,8 · groen vanaf 0,8. Nog nooit gespeeld = grijs.

**Kleur van een lijst:** rood onder 0,5 · groen vanaf 0,7 · **goud** bij 100% (alle woorden score 1).

**Niveaupercentage (de 70%):** gemiddelde woordscore over **alle woorden van alle lijsten van het niveau**; woorden die nog nooit gespeeld zijn tellen als 0. Daardoor moet je dus alles gespeeld hebben om 70% te halen, wat past bij de regel dat de mix pas na alle lijsten opengaat.

**Vaker foute woorden:**
- *Binnen een sessie:* een fout beantwoord woord komt 3 à 5 posities verder in dezelfde sessie opnieuw, tot het één keer juist is.
- *Over sessies heen:* bij het samenstellen van een sessie of de gemixte oefening wordt de volgorde gewogen met `gewicht = 1 + 3·(1 − score)`: zwakke woorden komen vroeger en vaker aan bod, woorden met score nabij 1 komen laat of één keer voor.

## 6. Sessies en oefenvormen
- **Sessie** = alle woorden van de gekozen lijst(en), in volgorde bepaald door § 5.
- **Matchen:** per ronde 6 paren. Een foute koppeling telt als fout voor beide woorden en kleurt rood; de ronde gaat pas verder als alle paren juist verbonden zijn. Richting willekeurig of volgens keuze.
- **Typen:** één vraag per woord, richting EN→NL of NL→EN naar keuze of afwisselend.
- **Gemixt:** alle lijsten van het niveau door elkaar, gewogen volgens § 5.
- **Combineren:** zelf gekozen lijsten worden één sessie; elk woord telt voor zijn eigen lijst. 🔶 Een gespeelde combinatie telt als gespeeld voor de betrokken lijsten.

## 7. Examen
- Beschikbaar vanaf **70%** in het niveau. Vocabulary: **12 vragen, 3 levens**. Grammar: **21 vragen, 3 levens**.
- Vragen worden getrokken uit het niveau met de gewogen verdeling van § 5 (zwakkere woorden vaker), zonder dubbels, in dezelfde oefenvorm als het niveau.
- Matchen-examen: 12 vragen = twee rondes van 6 paren; elke foute koppeling kost een leven.
- Fout antwoord = leven kwijt + juiste oplossing tonen. 🔶 Geslaagd zodra alle vragen beantwoord zijn zonder alle 3 levens te verliezen.
- Geslaagd → volgend niveau ontgrendeld + trofee. Mislukt → opnieuw proberen, 🔶 zonder wachttijd.
- Examenuitkomsten tellen mee voor de woordscores.

## 8. Streak, dagdoel en trofeeën 🔶
- **Dag** = kalenderdag in de tijdzone van het toestel.
- **Streak:** een dag telt zodra er één oefening gemaakt is. 🔶 "Eén oefening" = één afgeronde matchen-ronde, één getypte vraag of één Grammar-vraag. Een gemiste dag zet de streak op 0.
- **Dagdoel:** 10 minuten **actieve** oefentijd (tijd op oefenschermen, pauze bij 30 seconden inactiviteit of als de app naar de achtergrond gaat).

Trofeeën:

| Trofee | Voorwaarde |
|---|---|
| Dagdoel | 10 minuten gehaald; teller met aantal dagen |
| Streak | 3, 7, 14, 30, 60, 100, 200, 365 dagen |
| Niveau | Per gehaald niveau (70% bereikt) |
| Examen | Per geslaagd niveau-examen |
| 100% | Teller: hoe vaak een lijst of type op 100% kwam |

## 9. Herinneringen
- Instelbaar uur, dagelijks, via `AlarmManager`/`WorkManager`; toestemming voor meldingen aanvragen (Android 13+).
- 🔶 De melding wordt overgeslagen als er die dag al geoefend is.
- Na herstart van het toestel opnieuw plannen.

## 10. Nieuwe lijsten importeren
- Via de bestandskiezer van Android (Word `.docx`).
- Een `.docx` is een zip met XML. `DocxListImporter` leest de tabellen met kolommen *Word and Type* · *Explanation/Definition* · *Dutch Translation*, zonder zware bibliotheken.
- Opvangen van de bekende bijzonderheden: dubbele kolommen door samengevoegde cellen, meerdere tabellen in één bestand, kop-rijen herkennen, Nederlandse varianten splitsen op komma's, `to …`/`a …` en het type `(v)` uit het Engelse veld halen.
- Na het inlezen een **voorbeeldscherm** met aantal woorden en eventuele problemen vóór bevestigen. Dubbele woorden binnen een lijst worden gemeld.
- De 7 bestaande lijsten worden via dezelfde importer toegevoegd. Rob heeft bevestigd dat de Word-lijsten gerust in de repo mogen (niet geheim of gevoelig).

## 11. Grammar-oefeningen bijmaken 🔶
Aantal: **25 oefenzinnen per if-type** (100 in totaal), zelf geschreven, in dezelfde stijl als de cursus: invullen van een werkwoordsvorm met het werkwoord tussen haakjes. Alle zinnen worden door Rob nagelezen vóór ze in de app komen; elke zin krijgt een correcte antwoordenlijst (varianten zoals `'d`/`would`) en een korte uitleg.

## 12. Testen en kwaliteit
- Unit tests voor `AnswerChecker` (alle regels van § 4), `Scoring`, `Unlocking`, `ExamEngine`, `SessionBuilder` en `DocxListImporter` (met een klein synthetisch testbestand, geen cursusmateriaal).
- Instrumentatietests voor de belangrijkste flows (login stub, sessie spelen).
- **Git flow:** `main` = werkend, `develop` = integratie, `feature/*` per onderdeel. Pipeline via GitHub Actions bouwt en test bij elke pull request naar `develop` en `main`; een rode build blokkeert de merge.

## 13. Privacy en repo
- De repo is **publiek**. De Word-lijsten mogen er volgens Rob in; geheimen (sleutels, keystores) komen er niet in. Of `google-services.json` in git komt, wordt bij de Firebase-stap bepaald.
- Firestore-regels: een gebruiker leest en schrijft enkel zijn eigen `users/{uid}/…`.

## 14. Bouwvolgorde (voorstel) 🔶
1. Projectgeraamte, thema, navigatie, Google-login
2. Domeinlogica met tests (antwoordcontrole, score, ontgrendeling)
3. Lijsten importeren en opslaan
4. Vocabulary: matchen, daarna typen, overzichten, combineren
5. Examen
6. Grammar
7. Streak, dagdoel, trofeeën
8. Herinneringen
9. Pipeline en afwerking

Elke stap = eigen `feature`-branch, merge naar `develop` na akkoord van Rob.

## 15. Nog te bevestigen door Rob
Alle 🔶-punten hierboven, samengevat:
1. Kleurgrenzen en de woordscore-formule (§ 5)
2. Typo-regel: enkel naburige toets op AZERTY; ontbrekende/extra letters fout (§ 4)
3. Wat telt als "één oefening" voor de streak (§ 8)
4. Streak-drempels en trofee-overzicht (§ 8)
5. Examen: slagen zolang levens niet op zijn; herproberen zonder wachttijd (§ 7)
6. Zelf gecombineerde lijsten tellen als gespeeld (§ 6)
7. Herinnering overslaan als er al geoefend is (§ 9)
8. 25 oefenzinnen per if-type en nalezen door Rob (§ 11)

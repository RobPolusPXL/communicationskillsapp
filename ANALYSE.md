# Analyse: Communication Skills 2 – leerapp

Opgesteld op basis van de beslissingen van Rob. Alles in dit document komt van hem; punten die hij aan de analist/ontwikkelaar overliet staan apart onder **§ 14**.

## 1. Doel
Een persoonlijke app om de leerstof van **Communication Skills 2** (PXL, Toegepaste Informatica, 2026–2027) te leren: veel woordenschat en enkele grammaticaonderdelen. Zwaartepunt in de eerste fase: **woorden leren**.

## 2. Gebruiker en platform
- Eén gebruiker (Rob), voor eigen gebruik.
- **Android-app (APK)**, gebouwd in Android Studio, naar het voorbeeld van de bestaande maaltijdplanner-app (Kotlin, Jetpack Compose, Gradle, lokale opslag, Firebase).
- Interface volledig in het **Nederlands**.
- Offline werken is niet verplicht, maar mag.
- Inloggen met **Google-account**; voortgang wordt **online bewaard** (robuust, overleeft herinstallatie).

## 3. Scope

**In scope (nu)**
- Route **Vocabulary**: woordenlijsten leren via matchen en typen, in beide richtingen (EN→NL en NL→EN).
- Meerdere lijsten **combineren** tot één oefensessie (onder meer voor de korte lijsten van 16–17 woorden).
- Route **Grammar**: if-zinnen (conditionals) type 0, 1, 2 en 3, zinnen aanvullen door zelf te typen.
- Voortgang, herhaling van foute woorden, niveaus met ontgrendeling, examens.
- Duolingo-achtige elementen: voortgang, trofeeën, dagdoel, streak.
- Dagelijkse herinneringsmelding (tijdstip instelbaar).
- Nieuwe woordenlijsten later kunnen toevoegen door de app een lijst te geven.

**Buiten scope**
- Hedging (te moeilijk automatisch na te kijken; oefent Rob met AI).
- Levens buiten de examens.
- Punten (XP): niet gevraagd.
- Passief, "correct the sentences", verwarrende woordparen, uitspraak: niet gevraagd. Latere cursusonderdelen kunnen mogelijk later bijkomen (de cursus is nog niet volledig gezien).

## 4. Inhoud (huidige leerstof)
Zeven Word-lijsten met telkens een tabel *Word and Type* · *Explanation/Definition* · *Dutch Translation*:

| Lijst | Ca. woorden |
|---|---|
| Applying for a job | 170 |
| Business writing | 179 |
| Academic writing | 128 |
| Moral foreign language effect | 55 |
| Trolley problem | 23 |
| Ethical dilemmas | 17 |
| Stanford class | 16 |

Totaal ca. 590 woorden (exact aantal te valideren bij het inlezen).

Bijzonderheden: *Applying for a job* heeft dubbel weergegeven kolommen (samengevoegde cellen); *Business writing* en *Academic writing* bestaan uit meerdere tabellen. Engelse woorden staan met lidwoord of "to" en een type, bv. `to pursue (v)`, `a trolley (n)`. Nederlandse vertalingen kunnen meerdere varianten bevatten, bv. `consignatiezending, verzending, depotgoederen`.

Grammarbron: cursus, hoofdstuk "Language work: Grammar", p. 243–248 (conditionals). De cursus bevat te weinig oefenzinnen, dus extra oefeningen in dezelfde stijl worden bijgemaakt (zie § 14).

## 5. Routes en niveaus

### Vocabulary
- **Niveau 1: Matchen.** **Niveau 2: Typen.** Niveau 2 gaat pas open na het examen van niveau 1.
- Alle lijsten van een niveau staan open; Rob kiest zelf welke lijst hij speelt.
- In het begin oefent hij **lijst per lijst**. De **gemixte oefening** (alle lijsten door elkaar) gaat pas open als alle lijsten van dat niveau minstens één keer gespeeld zijn.
- Een oefensessie = alle woorden van de gekozen lijst.
- Rob kan ook **meerdere lijsten combineren** tot één oefensessie, omdat sommige lijsten (16–17 woorden) kort zijn. Een sessie bevat dan alle woorden van de gekozen lijsten samen.

### Grammar
- **Eén niveau**, met de 4 if-types als vrij te kiezen onderdelen (zoals de lijsten) en een **gemixte oefening** die pas opengaat nadat alle 4 types gespeeld zijn. De mix is het moeilijkste deel.
- Vocabulary en Grammar zijn twee aparte routes.

## 6. Oefenvormen

**Matchen**: 6 Engelse en 6 Nederlandse woorden tegelijk op het scherm; Engels verbinden met Nederlands, ook omgekeerd. Via een knop kan de Engelse uitleg (Explanation/Definition) opgevraagd worden.

**Typen**: antwoord volledig intypen, in beide richtingen.

**Zinnen aanvullen (Grammar)**: ontbrekend woord of werkwoordsvorm zelf typen.

Tijdens de oefening kleurt een antwoord meteen **groen (juist)** of **rood (fout)**.

## 7. Antwoordcontrole (typen)
- **Meerdere vertalingen:** één juiste vertaling is genoeg.
- **"to" / lidwoord (NL→EN):** antwoord met of zonder "to" (of "a") is volledig juist. Zonder "to" volgt enkel de melding dat het "to …" moet zijn om 100% correct te zijn.
- **Typo's:** een letter vervangen door een letter die op het toetsenbord naast de juiste ligt (voorbeeld: z i.p.v. a) is toegestaan: telt als juist, met melding dat er een typo in zit. Letters omwisselen (ao i.p.v. oa) is fout. *(Interpretatie "naburige toets" te bevestigen bij ontwerp.)*
- **Accenten:** niet vereist; enkel een melding.
- **Hoofdletters:** enkel vereist waar het echt moet (bv. dagen). Weglaten = **fout**.
- **Bij elk fout antwoord wordt altijd de juiste oplossing getoond**, samen met de Engelse uitleg van het woord.

## 8. Voortgang en herhaling
- Woorden die fout waren komen **vaker** terug; woorden die altijd juist zijn komen **minder vaak** voor.
- Voortgang wordt per woord bewaard (online).

## 9. Niveaus, ontgrendeling en examen
- Volgende niveaus zijn **vergrendeld** tot de voorwaarde gehaald is.
- Een niveau is **gehaald bij 70%**, **gemiddeld over het hele niveau** (alle woorden van alle lijsten samen).
- Bij 70% krijgt Rob de **optie** het **examen** te doen. Slagen voor het examen ontgrendelt het volgende niveau.
- Rob kan een vergrendeld niveau toch aanklikken; hij krijgt dan een waarschuwing ("Ben je zeker? Je bent mogelijk nog niet klaar voor dit niveau").
- **Examen:** test het niveau in dezelfde oefenvormen als het niveau zelf.
  - Vocabulary: **12 vragen, 3 levens**.
  - Grammar: **21 vragen, 3 levens** (meer vragen bij gelijk aantal levens, want fouten maken is hier minder aanvaardbaar zodra je het echt voelt).
  - Geslaagd zolang de levens niet op zijn. *(Impliciet aangenomen; te bevestigen.)*
- Levens bestaan enkel in de examens.

## 10. Duolingo-elementen
- **Voortgang** zichtbaar per route, niveau en lijst.
- **Dagdoel:** 10 minuten oefenen, geeft een trofee.
- **Streak:** een dag telt zodra **één oefening** gemaakt is.
- **Trofeeën:**
  - dagdoel gehaald;
  - **streak:** een opbouwende reeks met telkens een langere streak;
  - gehaald niveau;
  - **examen** gehaald (één per niveau);
  - **100%**, met een teller van hoeveel keer 100% behaald is.
- Geen levens (buiten examens), geen XP.

## 11. Overzichten en kleuren
- **Overzicht van de lijsten:** rood, groen of **goud (100%)** per lijst.
- **Overzicht van de woorden:** per woord rood of groen, afhankelijk van hoe vaak het goed of fout was.
- **Tijdens de oefening:** meteen groen of rood.
- Uiterlijk: **witte achtergrond**; inspiratie uit de duidelijke aanpak van Duolingo, maar geen kopie.

## 12. Meldingen
Dagelijkse herinnering op een **zelf in te stellen uur**.

## 13. Nieuwe lijsten toevoegen
Rob geeft de app een lijst (Word-bestand met dezelfde tabelopbouw) en de app voegt die toe. De technische weg (in de app inlezen, via de repo of via de database) mag de ontwikkelaar kiezen. Ook latere grammaticaonderdelen moeten zo toegevoegd kunnen worden.

## 14. Overgelaten aan de ontwikkelaar / nog te bepalen in het ontwerp
- Drempels van de **streak-trofeeën** (Rob: "maak iets opbouwend").
- Aantal en inhoud van de **bijgemaakte oefenzinnen** per if-type, in dezelfde stijl als de cursus.
- Exacte **herhalingsregel** voor foute/juiste woorden (het gedrag staat in § 8).
- Precieze berekening van het **70%-gemiddelde** over een niveau.
- Technische keuze voor het inlezen van nieuwe lijsten (§ 13).
- Samenspel van **zelf gecombineerde lijsten** met de **gemixte oefening** (die pas opengaat nadat alle lijsten gespeeld zijn): aangenomen dat zelf combineren altijd mogelijk is en dat het spelen van een combinatie telt als het spelen van de betrokken lijsten. Te bevestigen.
- Bevestigen van de twee interpretaties in § 7 en § 9.

## 15. Technische richting (genoteerd, nog niet uitgewerkt)
Kotlin + Jetpack Compose, Gradle, Android Studio; Google-login en online opslag via Firebase, zoals de maaltijdplanner-app. Repository: `RobPolusPXL/communicationskillsapp`.

# v1.6.0 — The night is yours

## Art direction

The party references established the mood: adults around a table, a bar and arcade behind them, warm light, expressive illustration and humor driven by the situation. The visual hierarchy is **people → action → friends' reaction → interface**. The ten challenges still need one-handed readability, with the atmosphere of a night out rather than a children's game catalog.

Twelve original portraits represent fictional party characters: a winegrower, creative director, DJ, antiques dealer, diva, chef, photographer and others. No personality is assigned by ethnicity or gender. A portrait may be chosen by several players; optional photo import remains possible. Each game uses a distinct fragment of the night scene and a relevant visual cue: signage, curtain, vinyl, coasters, gallery, cards, rhythm, beam or wire. The result keeps the artwork legible and the player's face prominent.

The historical design discussion drew inspiration from recognizable roles and character anticipation in other games. It did not use third-party illustrations or claim that a strategy game's audience proves virality for a party game.

## Changes across all ten games

| Game | Action around the table | v1.6.0 reveal |
| --- | --- | --- |
| Trivia | Friends answer privately; the actor can follow the room or choose alone. | Correct answer and the friends who got it. A disputed octopus question was replaced. |
| Poses | Bar-scene role cards played solo, seated or with a willing partner. | Jury votes and named supporters; passing without points or sips is allowed. |
| Music Quiz | Original motifs become fictional jingles; everyone submits a choice. | Jingle title and friends with sharp ears. No third-party catalog. |
| Reflex | Every friend places a target on the course. | Targets hit and the friends who built the course. |
| Roulette | Friends protect coasters; the actor chooses a risk. | Chosen coaster, outcome and its protectors. |
| Drawing | Prompts feel situated in the party; friends guess privately. | Subject and the people who recognized the drawing. |
| Memory | Friends compose a symbol chain. | Progress and the first attributed contributions. |
| Rhythm | The room composes a measure to repeat. | Measure and its composers. |
| Bluff | True or invented bar alibis defended before the jury. | Truth and believers; a penalty-free pass is possible. |
| Last Wire | The bomb travels between players before the final cut. | Named relay route and chosen wire, retained after resume. |

In-app help explains each action in FR/EN. Menu language can be selected on the home screen or under **Settings → Party** and persists on the device. A round still follows the active player's profile language; these controls are independent.

The original audio was reorganized into shorter, more widely spaced phrases. Background music softens during the jingle, rhythm and bluff challenges. Tap and verdict sounds are less insistent, with game-specific accents. Music, style, volume, SFX and haptics remain separate settings.

## Reproduction and verification

```sh
APERO_SIM_PARTIES=10000 APERO_PEOPLE_PARTIES=10000 \
./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease \
  --offline --rerun-tasks
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
```

| Check | Historical result |
| --- | --- |
| JUnit | 48 tests passed, including language choice, shared portraits, passing, locked answers, relay persistence and FR/EN reveal stories. |
| **Simulated** multi-device rooms | 10,000 parties, 40,000 rounds, 160,000 private FR/EN snapshots, 80,000 forbidden actions rejected and 400 restores. |
| **Simulated** clock and interruptions | 10,000 parties of 18 rounds, or 180,000 rounds, spanning 2–6 players, three modes and modeled absences. |
| **Hypothetical** virtual players | 10,000 parties of 24 rounds, or 240,000 rounds, with assumed tastes, relationships, moods and fatigue. |
| Android 16, one phone | Two profiles, ten games, alternating actors, stakes, friend actions, history and screenshots via `tools/smoke_v120.py`. |
| Two emulators on simulated local Wi-Fi | Android 16 hosted and Android 8 joined with address/PIN. The lobby and first result synchronized; a remote Trivia answer reached the host in the next round. The link used the emulator's local port forwarding. |

In the virtual-player model, mean round duration depended strongly on mode and device layout: roughly **19.5 s** with a phone per player for a “mixed” group versus **34.6 s** on one shared phone, and **51.3 s** for the modeled shared “distracted” scenario. These numbers arise from test assumptions. They identify where to observe real handoffs; they are neither human timing measurements nor real network latency.

## What the release did not demonstrate

A simulation cannot prove that an anecdote is funny, a portrait is appealing, the audio works in a real bar or the games will spread. The device journeys above used emulators. This release did not measure Wi-Fi, Bluetooth or Internet latency on physical phones. External music services open in their own apps; Apéro Royale does not control their catalogs. The default Internet relay is a public test service.

A useful next human test is to observe several tables without explaining each scene first. Record improvised rules, hesitation, phrases repeated the next day and sessions where nobody laughs. Those observations can test the social promise more honestly than a simulated “fun score.”

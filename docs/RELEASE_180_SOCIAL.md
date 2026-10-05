# v1.8.0 — Trust the table

A round should create a story between friends, not merely return a score. This release gives Trivia and Music Quiz three clear choices: answer independently, follow the room's lead, or trust a named friend. The friend has already answered, but their choice remains hidden until the reveal. When that answer is right, **both players earn 25 bonus points**. Misplaced trust adds no separate drinking penalty; the round's ordinary stake remains the only possible virtual-sip consequence.

Royal Bluff now designates a friend to ask a spoken follow-up in the active player's language. The jury still votes individually. All ten games retain their illustrated scenes, group actions and FR/EN help.

## A shareable memory, by choice

The result screen places **Share** beside the leaderboard. The app renders a 1080 × 1920 PNG card with the game's scene, the active player's nickname, a short reveal, and the score or virtual sips. The result and card use a numbered game badge with the adult illustration. Neither portraits nor imported photos are included. The card stays in the local app cache unless a player chooses a destination in Android's share chooser; there is no automatic upload.

<p align="center">
<img src="screenshots/games/trivia-result.png" alt="A reveal about two friends trusting each other" width="260"> <img src="screenshots/round-card.png" alt="The locally generated vertical round card" width="260">
</p>

## Consistency across devices

The host keeps individual Trivia and Music Quiz answers private during play. Guest snapshots include the number of replies, the name of an eligible friend and the room's leading option, but not each person's answer. The **trust a friend** command goes to the host, which locks in that friend's actual answer and awards points. The final choice becomes visible with the result. Contributions for other challenges remain available where their gameplay requires them.

## Reproducible stress test

The complete verification used:

```sh
APERO_SIM_PARTIES=10000 APERO_PEOPLE_PARTIES=10000 \
APERO_ORGANISM_PARTIES=10000 APERO_SOCIAL_ROUNDS=10000 \
./gradlew :app:testDebugUnitTest :app:assembleRelease :app:lintVitalRelease \
  --offline --rerun-tasks
```

| Model | Executed | Invariant or assumption exercised |
| --- | ---: | --- |
| Virtual clock and device layouts | 10,000 parties, 180,000 rounds | Turn order, deadlines, missing actions, resume and game repetition. |
| Stateful virtual players | 10,000 parties, 240,000 rounds | Choices shaped by assumed preferences, relationships and fatigue. |
| Relative player trajectories | 10,000 parties, 240,000 rounds | Modeled changes compared with each virtual player's own baseline. |
| v1.8.0 social trust | 10,000 Trivia/Music Quiz rounds, 40,000 private views | 2–6 FR/EN players, masked answers, public room lead, lock-in, scores and resume. |

In the focused synthetic run, 3,334 actors chose to trust a friend and 2,000 rounds were won. Those shares follow the test's deterministic choices: **they are not measurements of enjoyment, virality or the feature's effectiveness**. The test covers both languages and all five table sizes. The first three models complement one another; they do not represent 30,000 distinct human groups.

On an Android 16 emulator, the debug build played through all ten challenges with two alternating profiles, wagers, friend actions and saved history. A real round generated the PNG card, which was previewed in Android's share chooser. The signed APK installed as an update from v1.7.0 on an Android 8 emulator and as a fresh install on an Android 16 emulator; the main activity started on both. The scripted smoke test reads SQLite through `run-as`, so it applies to debug builds rather than the signed production build.

## Evidence boundary

Virtual-player behavior, relationships and timing are assumptions. This release did not add tests with human groups, connected physical phones over Wi-Fi/Bluetooth/Internet, or active external music services. The card is an optional souvenir; the app collects no biometric or physiological data. Sharing an image to another app depends on a player's choice and that app's behavior.

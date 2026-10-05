# Release notes

The newest signed Android package is always linked from the [README](README.md). Historical packages below remain available for reproducibility; install **v1.8.0** for the current experience.

## v1.8.0 — Trust the table

**[Download the signed APK](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.8.0.apk)** · Android 8.0+ · `com.aperoroyale` · versionCode `20`

SHA-256: `b7e8cf73c69f06ccbdd1f34075a80ae5b36866680d02906e1e581965c5c577d4`
Signing certificate SHA-256: `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120`

- **A choice with a face.** In Trivia and Music Quiz, the active player can answer alone, follow the room's lead or trust a named friend. That friend's answer stays private until the reveal. A correct duo earns 25 extra points each.
- **A better Bluff.** A designated friend asks a spoken follow-up in the active player's language before the jury votes.
- **A story to take away.** Every result offers a locally rendered 9:16 share card with the illustrated game scene, nickname, verdict and score. Android's share chooser opens only on request. Imported profile photos never appear on the card.
- **A more adult reveal.** The result and share card use the game's numbered badge instead of the small pixel sprite.

**Verification:** 60 passing Java tests; three synthetic models covering 30,000 modeled sessions and 660,000 rounds; a focused 10,000-round social test with 40,000 private snapshots. A debug build completed all ten games with two alternating players on an Android 16 emulator. The signed APK was installed as an update over 1.7.0 on an Android 8 emulator and as a fresh install on an Android 16 emulator; it launched on both. APK Signature Scheme v2 and the package metadata were verified.

Read the [design and test report](docs/RELEASE_180_SOCIAL.md), [round reveal](docs/screenshots/games/trivia-result.png) and [share card](docs/screenshots/round-card.png). The models test logic and assumed timing; they do not measure real-world fun or network latency on physical phones.

## v1.7.0 — Room to breathe

**[Signed APK](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.7.0.apk)** · versionCode `19`

- Jury and drawing votes gained a seven-second grace period after a majority responds. Missing answers are no longer treated as rejection, and a local handoff pauses the countdown.
- Drawing gained 50 seconds and Music Quiz 28 seconds at their maximum; two-player Turbo takes a smaller time reduction. Early finishes still advance immediately.
- Optional FR/EN conversation prompts now appear around results and handoffs, with a setting to turn them off.
- A [paired synthetic comparison](docs/RELEASE_170_RYTHME.md) replayed 30,000 modeled parties before and after the timing changes. The test reported fewer expired rounds, while some measures of longest silence rose slightly.

The original release verification reported 56 passing tests and signed APK launch on Android 8 and 16 emulators. The detailed historical report remains in the repository.

## v1.6.0 — The night is yours

**[Signed APK](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.6.0.apk)** · versionCode `18`

- Introduced 12 original adult portraits, ten illustrated bar and arcade scenes, and a brass, plum and petrol visual palette. Multiple players can choose the same portrait; optional photo import remains available.
- Menu language can be selected on the home screen or in Settings and persists separately from each player's turn language.
- All ten games gained clearer instructions and named social reveals. Poses and Bluff gained a penalty-free pass.
- Original music and SFX were spaced out, with quieter moments during listening and speech. The [design report](docs/RELEASE_160_DESIGN.md) records the direction and its test boundaries.

The original verification reported 48 passing tests, ten games played with two alternating profiles on one Android 16 emulator, and a limited two-emulator Wi-Fi check. No physical-phone latency claim was made.

## v1.5.0 — The table takes shape

**[Signed APK](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.5.0.apk)** · versionCode `17`

- Reworked home, lobby and settings layout with slimmer controls and clearer hierarchy. The lobby added a more legible two-player table and direct Vote, Free and Turbo selection.
- A late guest can join an active room as a spectator, play hidden waiting games and face a private admission vote at the next break. Rejection adds a virtual sip and permits a later retry.
- The [interface and stress report](docs/UI_150_ET_STRESS.md) documents the changes. The original release reported 43 passing tests and signed update checks on Android emulators.

## Earlier milestones

| Version | Signed package | Milestone |
| --- | --- | --- |
| 1.4.9 | [APK](releases/AperoRoyale-v1.4.9.apk) | Stateful virtual-player model with preferences, relationships, mood and fatigue; Music Quiz and Memory timing adjustments. |
| 1.4.8 | [APK](releases/AperoRoyale-v1.4.8.apk) | Virtual-clock stress model, explicit skip for absent players and handoff-aware timers. |
| 1.4.7 | [APK](releases/AperoRoyale-v1.4.7.apk) | Synthetic party-lab findings, vote timeout and finite secret-rule selection. |
| 1.4.6 | [APK](releases/AperoRoyale-v1.4.6.apk) | Persistent profiles, pause/cancel/return navigation and a dedicated music dock. |
| 1.4.5 | [APK](releases/AperoRoyale-v1.4.5.apk) | Hidden waiting-room games and a discovery-triggered room rule. |
| 1.4.4 | [APK](releases/AperoRoyale-v1.4.4.apk) | A role for friends in all ten challenges, with private actions and crew scores. |
| 1.4.3 | [APK](releases/AperoRoyale-v1.4.3.apk) | Stronger bomb relay, more round variants and a fuller-room rotation. |
| 1.4.2 | [APK](releases/AperoRoyale-v1.4.2.apk) | Persistent shuffled content decks and timing diagnostics. |
| 1.4.1 | [APK](releases/AperoRoyale-v1.4.1.apk) | Secret-rule and jury timeout fixes. |
| 1.4.0 | [APK](releases/AperoRoyale-v1.4.0.apk) | Midnight visual direction and external music-service launchers. Spotify catalog quiz integration was removed; Music Quiz uses original offline motifs. |
| 1.3.0 | [APK](releases/AperoRoyale-v1.3.0.apk) | More consequential predictions, stronger Bluff and bomb rules, and host-checked Rhythm. |
| 1.2.1 | [APK](releases/AperoRoyale-v1.2.1.apk) | Private handoff and resume timer fixes. |
| 1.2.0 | [APK](releases/AperoRoyale-v1.2.0.apk) | Explicit local turns, Turbo mode and per-game night scenes. |

The [documentation index](docs/README.md) links the design notes, simulation methods, data files and verification limits behind these milestones. Historical results describe the build and model available at the time; they are not measurements of human enjoyment or proof of viral growth.

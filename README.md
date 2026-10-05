<div align="center">

# APÉRO ROYALE

### THE NIGHT IS YOURS.

**Ten games. Two to six friends. One table full of stories.**

An illustrated Android party arcade built for people in the same room. Pass one phone around, or let everyone play and vote from their own. Every player can use French or English.

[**DOWNLOAD THE SIGNED APK · v1.8.0**](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.8.0.apk) · [Release notes](RELEASE.md) · [Privacy](PRIVACY.md) · [License](LICENSE)

Android 8.0+ · Offline local play · No account · No ads

<img src="docs/screenshots/home.png" alt="English home screen, set in an illustrated late-night arcade bar" width="335"> <img src="docs/screenshots/round-card.png" alt="A shareable round card with adult game artwork" width="335">

</div>

## The pitch

Someone takes the spotlight. Everyone else gets a move: vote, set a trap, make a prediction, build a pattern, guess a drawing or judge an alibi. The active player stakes **one to three virtual sips**, plays a short challenge, and hands the turn on. The reveal names the friends who helped, doubted or caused the chaos.

The art direction is a midnight bar and arcade: warm brass, deep plum, illustrated adult characters and a distinct scene for each game. The controls stay easy to hit; the atmosphere does the heavy lifting.

> The drinking rule is optional. Replace any virtual sip with water, a non-alcoholic drink, a dare, or nothing. Pose and Bluff also offer a penalty-free pass.

## Start a night in three moves

1. **Set the table.** Add 2–6 players. Pick a name, FR/EN language, one of 12 adult portraits, or an optional photo. Portraits can be shared; profiles survive across parties.
2. **Pick the pace.** In **Vote**, the table chooses the next game. In **Free**, browse all ten. In **Turbo**, the game deals faster surprise rounds.
3. **Play together.** On one phone, private actions use clear handoff screens. On several phones, players answer from their own devices. The active player's language follows their profile; menu language is a separate setting.

<p align="center">
<img src="docs/screenshots/avatar-choice.png" alt="Adult portrait selection" width="205"> <img src="docs/screenshots/lobby-two.png" alt="Two-player lobby" width="205"> <img src="docs/screenshots/handoff.png" alt="Private turn handoff" width="205">
</p>

## Ten games, ten reasons to talk

Every game has an illustrated scene, a FR/EN tutorial and a social reveal. The table participates in all ten, including reflex and rhythm challenges.

| Game | What the room does |
| --- | --- |
| [**Trivia**](docs/screenshots/games/trivia.png) | Friends answer in secret. The active player can go solo, follow the room, or trust a named friend; a correct duo earns bonus points. |
| [**Absurd Poses**](docs/screenshots/games/poses.png) | Act out a bar-scene prompt alone, seated, or with a willing accomplice. The jury votes. |
| [**Music Quiz**](docs/screenshots/games/blind-test.png) | Hear an original fictional jingle, cast private guesses and decide whether to trust a friend's ear. |
| [**Neon Reflex**](docs/screenshots/games/reflex.png) | Friends place targets; the active player races through the course they built. |
| [**Royal Roulette**](docs/screenshots/games/roulette.png) | Each friend protects a coaster. The active player picks a risk. |
| [**Cursed Drawing**](docs/screenshots/games/drawing.png) | Draw a party-themed prompt, then watch everyone guess privately. |
| [**Flash Memory**](docs/screenshots/games/memory.png) | Friends assemble a symbol chain for the active player to repeat. |
| [**Rhythm or Nothing**](docs/screenshots/games/rhythm.png) | The room sets the measure; the active player hits the beats. |
| [**Royal Bluff**](docs/screenshots/games/bluff.png) | Tell a true or invented alibi. A friend asks a follow-up and the jury decides. |
| [**The Last Wire**](docs/screenshots/games/bomb.png) | Pass the bomb between players before somebody chooses the final wire. |

The jury and drawing gallery leave a seven-second grace period after a majority responds. Missing players are not counted as “no” votes.

<p align="center">
<img src="docs/screenshots/games/trivia-result.png" alt="A round reveal names the friends who trusted each other" width="260"> <img src="docs/screenshots/games/drawing-result.png" alt="Cursed Drawing gallery reveal" width="260"> <img src="docs/screenshots/games/bomb-result.png" alt="The Last Wire route reveal" width="260">
</p>

### A round worth sharing

The result tells a short story with the players' names. **Share** renders a 1080 × 1920 illustrated card on the device and opens Android's share chooser. It includes the game art, nickname, verdict and score; imported photos never appear on the card. Nothing is posted automatically.

## One phone, many phones

| Setup | How it works |
| --- | --- |
| **One phone** | Create everyone locally and pass the device whenever a private action appears. The next turn goes to the next player. |
| **Same Wi-Fi** | A host opens a room; friends join with its address and six-digit PIN. Local TCP traffic is not encrypted, so use a trusted network. |
| **Nearby Bluetooth** | Pair Android devices, then host or join with the room PIN. |
| **Across the Internet** | Share a 12-character room code. Room payloads are encrypted over a TLS MQTT relay. The default relay is a third-party public test service, with no uptime guarantee. |

The host saves the game and historical leaderboard. A player can rejoin an ongoing party, watch the current challenge and play hidden waiting-room games. At the next break, existing players vote to admit them. A declined guest receives a virtual sip and can try again. The lobby has a six-player cap.

The pause menu can resume, return to the lobby or cancel the current challenge without awarding points or sips. An absent player can skip a private action so the table can keep moving.

<p align="center">
<img src="docs/screenshots/wifi-peer-action.png" alt="A remote friend's answer reaches the host" width="240"> <img src="docs/screenshots/late-join-spectator-en.png" alt="A late guest watches and plays a hidden game" width="240"> <img src="docs/screenshots/late-join-vote.png" alt="The table votes on admission" width="240">
</p>

## Soundtrack, secrets and control

The original soundtrack offers Chill and Arcade styles, separate music and SFX volume, and optional haptics. Music steps aside during listening, speaking and verdict moments. A compact radio menu can save a playlist link and open **Spotify, Deezer, Apple Music or Amazon Music** in its own app. Apéro Royale does **not** control those services or use their catalogs in the Music Quiz; the quiz plays original offline motifs.

Waiting screens hide small optional games. Discoveries can unlock a room-wide social rule; the table votes on disputed infractions. The app never listens to conversations or requests microphone access. Players can turn optional conversation prompts on or off in Settings.

<p align="center">
<img src="docs/screenshots/settings-party.png" alt="Party and language settings" width="205"> <img src="docs/screenshots/settings-music.png" alt="Music and effects settings" width="205"> <img src="docs/screenshots/radio-dock.png" alt="Radio Apéro launcher" width="205">
</p>

## Built and tested in the open

Apéro Royale is a native Java Android app. `GameEngine` handles turns and scoring; `GameStore` persists profiles, history and the active party in SQLite; `ArcadeView` and `GameSprites` draw the game; `ArcadeAudio` generates its original audio. The signing key is not in this repository.

The v1.8.0 verification ran **60 passing tests**: three complementary synthetic party models covered 30,000 modeled sessions and 660,000 rounds; a focused test covered another 10,000 Trivia/Music Quiz rounds and 40,000 private player snapshots. A debug build completed all ten games with two alternating players on an Android 16 emulator. The signed APK was installed and launched on Android 8 and 16 emulators.

These tests check rules, state and assumed timing. They do not prove that a real group will find a game funny or that Wi-Fi, Bluetooth and Internet latency will be low on physical phones. Read the [v1.8.0 test report](docs/RELEASE_180_SOCIAL.md) for the exact scope.

```sh
# JDK 17 and Android SDK 36
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease

# Requires a debug build and an emulator with the expected display layout
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py

# Longer focused social test
APERO_SOCIAL_ROUNDS=10000 ./gradlew :app:testDebugUnitTest \
  --tests com.aperoroyale.SocialFeatureStressTest
```

## Read more

[Release notes](RELEASE.md) · [Design and simulation reports](docs/README.md) · [Privacy and network model](PRIVACY.md) · [Third-party licenses](THIRD_PARTY_NOTICES.md) · [MIT license](LICENSE)

No account, advertising, analytics or biometrics. Profiles and leaderboards live on the host device; optional photos are downsized locally. See [Privacy](PRIVACY.md) before using network rooms or sharing a round card.

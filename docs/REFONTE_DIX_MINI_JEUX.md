# Ten games people want to play again

**Historical design notebook, written across versions 1.3.0–1.4.4.** The proposals below are prototypes and acceptance criteria, **not a list of shipped features**. See [release notes](../RELEASE.md) for the current build and the [1.4.7 experience lab](LABO_EXPERIENCE_2026.md) for modeled pacing.

The design hypothesis is simple: every round gives **everyone a decision**, one person a spotlight moment, the table a memorable reveal and a one-tap path to a rematch. That is a direction to test with people, not proof of virality.

## What the 1.4.x builds delivered

By 1.4.4 every challenge asked each player to do something game-specific. Friends answered Trivia and Sound, placed Reflex targets, protected Roulette cups, built the Memory opening, chose Rhythm beats and guessed Drawing. Poses and Bluff used a jury; Bomb used a relay. A shared phone waited for the next person's private confirmation, while multiple phones sent choices to the host. The [500-party model](SIMULATION_EXPERIENCE_500.md) found zero actor-only rounds in its 7,990-round sample, but six-player Vote mode on one phone remained slow and no real-group enjoyment was established by that model.

Earlier 1.4.3 work had added shuffled, no-repeat decks and mechanical variants:

| Game | Historical content or mechanic | Still needed in this design proposal |
| --- | --- | --- |
| Trivia | 17 no-repeat cards | Answers and alliances from everyone; reviewed explanations. |
| Poses | 18 no-repeat cards and direct jury | Friend-selected variation and mobility choices. |
| Sound | 6 original motifs × 4 tones | More distinct sound content and every friend's response. |
| Reflex | 6 targets/radii variants | A real duel and device latency measurement. |
| Roulette | 6 trap layouts and full reveal | Friends' clues, decoys and verifiable outcome. |
| Drawing | 17 no-repeat prompts; 30 s draw, 12 s guess | Friend-written false titles and a room vote. |
| Memory | 6 sequence structures | Readable construction or sabotage by friends. |
| Rhythm | 6 four-beat phrases on an eight-beat grid | Call and response with audio calibration. |
| Bluff | 17 no-repeat prompts; locked truth and direct jury | Short questions and an objectively judged fiction mode. |
| Bomb | Next-holder choice and a riskier late fuse | Richer decisions and reconnection tests. |

The old Spotify quiz integration was removed in 1.4.0. Spotify, Deezer, Apple Music and Amazon Music became optional external listening shortcuts. The sound challenge relies on original audio, not service tracks.

## Design briefs for the next prototypes

### 01 · Trivia: “Follow me if you dare”

Everyone locks a plausible answer. Before reveal, the featured player chooses whether to keep theirs or follow a friend without seeing that friend's choice. Reveal the alliance chain, then the sourced answer and a short explanation. On one phone, private answers pass between people; on several phones, they can be parallel. **Test:** every person answers, the follow choice is clear in under ten seconds, and host and guests score identically.

### 02 · Poses: “Disaster casting”

The room chooses seated, standing or no-contact prompts. A friend selects a comic variation; the performer acts for 10–15 seconds, then the table titles the pose or applauds. At two players, both get a short turn. A pass without a drinking penalty covers unsuitable space or mobility. **Test:** no unwanted physical contact, clear instructions and two real actions in a two-player round.

### 03 · Sound: “The noise studio”

In a shared room, a player imitates a party sound without requiring a microphone and friends guess the scene. Online, each phone synthesizes the same original seeded rhythm or timbre; friends identify or reproduce it locally. Silent play uses a visual pulse and haptics. **Test:** six consecutive rounds without dominant repeats, local audibility for the answering device and no dependency on the host speaker or streaming API.

### 04 · Reflex: “Trap duel”

A friend chooses a readable trap; the runner selects one counter. At two players, each runs the same seeded course using local time. At more players, compare short local attempts rather than packet arrival times. A client-generated trace is useful for a friendly room but cannot prove cheating resistance among strangers. **Test:** equal seeds produce equal targets; loss or reordering of packets causes no phantom hit; simulated 50/150/300 ms latency does not determine the winner.

### 05 · Roulette: “The traitor's cup”

Friends secretly place clue, protection or decoy tokens. The featured player sees token counts, then chooses a cup or proposes a swap. Reveal tokens before traps. Fix the random outcome before anyone chooses and disclose a seed afterward for audit. **Test:** every player has a decision, loss distribution matches the rules, no early secret reaches a guest and pure chance cannot stack endless penalties.

### 06 · Drawing: “False titles”

The artist draws for about 25 seconds. Others submit short fake titles and then choose the real prompt; a missing submission gets a game-provided decoy. Credit artists for correct guesses and decoy authors when they fool someone. A two-player version supplies game decoys; the guesser can name the art after reveal. A local gallery attributes work only when people opt into sharing. **Test:** everyone guesses, a blank drawing does not auto-win and export needs no broad photo permission.

### 07 · Memory: “The impossible chain”

Each holder repeats a short symbol chain and adds one symbol. The table owns one collective rescue. Hide the chain during physical handoff and start the answer timer after the demonstration. **Test:** both players alternate at two, all six participate before a long wait, save/resume preserves the phase, and color is never the only cue.

### 08 · Rhythm: “Answer the beat”

One player writes a four-hit phrase; another answers it. Score relative intervals recorded with a monotonic local clock. Sound and wave animation share that schedule, with optional calibration; a visual and haptic route remains playable muted. **Test:** the same performance scores similarly at varied network delay, and slow audio hardware does not make a round impossible.

### 09 · Bluff: “One-minute interrogation”

Offer two clearly named formats. In **Alibi**, the game privately assigns a consistent true-or-invented fiction card, so the outcome can be judged objectively. In **Personal anecdote**, the table plays for the story, without claiming a vote verifies personal truth. The judge gets one question at two players; a six-player table chooses at most two questions. Avoid intimate or humiliating prompts by default. **Test:** secret locked before voting, ties explained and the two formats understood.

### 10 · Bomb: “The wrong wire”

At each handoff, the holder chooses between two tools or wires. One helps; the other creates a comic constraint. The table gets one shared clue. Pause time for a physical phone pass and resume when the next holder confirms; the host validates remote ownership. **Test:** each person makes a decision, reconnection preserves the fuse and handoff time does not punish the team.

## Art and sound signatures

The prototype screenshots had distinct backdrops but similar title, frame, meter and action placement. Each new scene should center its **game object** and reveal:

| Game | Visual object and motion | Sound cue | Reveal |
| --- | --- | --- | --- |
| Trivia | Quiz lectern; portraits align or split | Choice-lock notes and answer chord | Reversed alliances |
| Poses | Articulated duo and stage curtain | Short roll, optional applause | Group poster |
| Sound | Cassette console and drawn waveform | Original noise bank and intentional silence | Hidden scene |
| Reflex | Expressive target and legible trap | Brief varied hits | Side-by-side times |
| Roulette | Six flipping cups and visible tokens | Rising clicks, hard stop | Traps then protection |
| Drawing | Canvas and title cards | Soft stroke and gallery chime | Attributed titles |
| Memory | Colored, shaped relic chain | One note per symbol | Sequence replay |
| Rhythm | Two pulse curves | Timestamped beat | Timing gap |
| Bluff | Alibi dossier and question cards | Suspense tick | Turned secret |
| Bomb | Distinct wires and visible fuse | Tick that leaves space for voices | Final choice close-up |

Sprites need rest, anticipation, action, success and failure states. A five-second example and skip option in each tutorial should be tested with real players.

## Shared implementation contract

1. **Rounds:** unique ID, explicit brief/commit/act/reveal/result phases, action owner and deadline. Pause only timers that must wait for a physical handoff.
2. **Network:** semantic commands carrying round ID, player ID, sequence, action and payload; host acknowledgments and deduplication; role-specific reconnect snapshots; a player session token separate from the room code. Compare local Reflex and Rhythm measurements, not packet delivery time.
3. **Content:** stable IDs, versions, categories, difficulty, accessibility tags, duration and source for factual cards. Draw without repeats until a deck is exhausted. Review FR and EN as playable copy.
4. **Scoring:** record actor, helper, trap, decoy, jury and team contributions separately. Save one result per round even after duplicate messages. Distinguish synced room ranking from local history.
5. **Sharing:** preview and deliberate Android Sharesheet export; remove names and photos by default. No automatic publication or broad media permission. See [Android sharing](https://developer.android.com/social-and-messaging/guides/media-sharing).
6. **Drinks:** sips are virtual counters. Offer water, a challenge, a pass and a per-player cap; never claim to measure blood alcohol. See the [WHO alcohol information](https://www.who.int/europe/news/item/04-01-2023-no-level-of-alcohol-consumption-is-safe-for-our-health/).

This notebook is a roadmap for prototypes and observed playtests. Screenshots, simulations and successful builds alone cannot establish that a joke lands at a real table.

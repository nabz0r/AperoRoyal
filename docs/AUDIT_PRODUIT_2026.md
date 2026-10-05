# Product audit and roadmap — Apéro Royale

**Historical baseline:** 4 October 2026, Android 1.2.1 at cf4c9f9. This audit reviewed code, documentation and emulator screenshots. It did not include a real party or physical-device network measurements. The later [1.4.7 experience lab](LABO_EXPERIENCE_2026.md) revisited pacing across 3,000 modeled rooms and fixed a vote timeout that could stall a room. Read the findings below in their original version context.

## Product judgment

The baseline already offered ten challenges, FR/EN profiles, pass-and-play, wagers, votes, juries, local saves and scores, plus Wi-Fi, Bluetooth and Internet transport. Its main opportunity was to turn each round into a story shared by the table: a quick choice, meaningful participation, a reveal worth talking about and an immediate next round.

At the time of the audit, several challenges still had one performer and several spectators. Network sync reproduced that structure on more phones without always changing the social play. Visual scenes differed, but the action layout and small sprites felt similar. Three short music patterns repeated, and rhythm input did not share the audio clock.

## Findings at the audited baseline

| Priority | Evidence in 1.2.1 | Effect | Proposed response |
| --- | --- | --- | --- |
| P0 | The round stake centered on the active player; others mostly gained a prediction bonus or penalty. | Friends waited through solo play. | Give every player a consequential action or role. |
| P0 | Rhythm taps used an index and overall deadline while the visual phase and audio steps used different periods. | Timing could be inconsistent. | Use one round clock, local calibration and a host-validated window. |
| P0 | Network snapshots contained targets, cups, votes, predictions and sequences for every client. | A modified guest client could inspect secrets early. | Send role-specific snapshots and reveal secrets at the correct phase. |
| P1 | Vote, transition, handoff, wager, prediction, action and result formed a long sequence. | Momentum fell before the game began. | Merge setup steps and shorten the replay path. |
| P1 | The offline music quiz named six unfamiliar original motifs. | It tested invented labels more than group music culture. | Make players create or interpret sounds; keep streaming links independent. |
| P1 | Historical stats keyed players by nickname on one device. | Names could collide and leaderboards diverge. | Use stable profile IDs and label local versus room rankings. |
| P1 | A 400×820 custom canvas exposed limited accessibility semantics. | Small labels and TalkBack support were weak. | Expose controls semantically and verify target size, scaling and contrast. |
| P2 | Three 32-step audio scenes and no identified focus handling in that module. | Repetition and possible conflict with other music apps. | Add longer, quieter scene layers and audio focus behavior. |
| P2 | Prompt arrays had 17 trivia, 18 pose, 17 drawing, 17 bluff and six melody entries with no repeat history. | Cards came back too soon. | Version content packs and draw without repeats until exhaustion. |

Several of these items were addressed in later releases. Use [release notes](../RELEASE.md) for shipped behavior; this table records the original diagnosis.

## Ten-game design direction

One player can take the spotlight while everyone makes a choice. On one phone, private choices pass from hand to hand behind a brief privacy screen. On several phones, they can happen in parallel. A timer begins when people are ready, never during the physical handoff.

| Game | Social move proposed in this audit |
| --- | --- |
| Trivia | Everyone answers; the featured player can trust their own answer or follow a friend before reveal. |
| Poses | A willing duo performs an absurd pose; others add a light variation or judge it. Offer a seated, no-contact option. |
| Sound | One player imitates or creates a party sound and others interpret it; recording stays optional. |
| Reflex | Short attack-and-defense duel; compare locally measured runs rather than network arrival times. |
| Roulette | Friends hide clues, decoys or protections before the featured player picks a cup. |
| Drawing | Everyone suggests a title; the reveal becomes a gallery and vote. |
| Memory | Players extend a shared chain, with one collective rescue. |
| Rhythm | Call and response with measured timing differences, after clock and audio calibration. |
| Bluff | A secret true-or-invented card, short questions and a vote, without forced personal disclosure. |
| Bomb | Each handoff offers a real choice and a readable pause on one phone. |

The more detailed [ten-game design notebook](REFONTE_DIX_MINI_JEUX.md) is a historical proposal, not a claim that every variant shipped.

## Experience and production priorities

**First minute.** Two names, two portraits, one-phone selection and a first challenge should be possible in under 60 seconds. Keep advanced options available without placing them before the first round. Each game's introduction should show a short playable example and allow a skip.

**Visual and sound.** Make the player, game object and next action obvious. Larger expressive sprites and distinct game compositions matter more than more decorative frames. Music should default to quiet, with mute, chill and arcade controls reachable mid-party. Sound cues for handoff, wager and reveal should share the rhythm game's clock where timing matters.

**Accessibility.** Verify ordinary text against a 4.5:1 contrast target, touch targets at least 48 dp, scaling on small and large screens, and TalkBack for essential menus and actions.

**Network proof.** Commands should carry round ID, player ID, sequence, action and payload. The host should acknowledge and deduplicate them, reject stale turns, keep secrets in role-specific snapshots and restore a disconnected player's role. Test two and six physical devices over supported transports, app sleep, late join, simultaneous actions and host exit. A public MQTT relay is an experimental dependency, not an availability guarantee.

**Content and identity.** Keep versioned FR/EN cards, no-repeat draws, stable profile IDs, a room ranking distinct from local historical stats and optional memory cards that players explicitly choose to share.

## Acceptance targets, not measured performance

First round under 60 seconds on one phone; at least one meaningful action per player per round; nobody idle for over 45 seconds; Express rounds 30–75 seconds; over 95% completion in observed parties; no duplicate results after reconnection; no early secret disclosure. Network targets: Wi-Fi join p95 under 10 seconds, action acknowledgments p95 under 300 ms on Wi-Fi and under 800 ms on the test Internet connection. Verify music interruption and screen-reader navigation.

A panel of 6–10 groups of 2–6 people should settle whether the interactions are actually clear and funny. Track voluntary replays, explanations needed, spontaneous reactions and times when people put down their phones. These were design targets, not evidence of an already perfect game.

Streaming service buttons were made optional listening shortcuts from 1.4.0 onward. Virtual sip counts must not be described as blood-alcohol measurements; water, a challenge or a pass should remain viable table choices.

## Sources reviewed for the historical audit

- [Jackbox audience interaction and tutorials](https://www.jackboxgames.com/blog/party-pack-11-free-content-update); [Hear Say sound creation](https://www.jackboxgames.com/blog/introducing-the-fourth-game-in-party-pack-11-hear-say)
- [Android accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility), [low-latency audio](https://developer.android.com/games/sdk/oboe/low-latency-audio), [audio focus](https://developer.android.com/media/optimize/audio-focus)
- [Spotify development changes](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide) and [playback reference](https://developer.spotify.com/documentation/web-api/reference/start-a-users-playback)
- [Godot client-action validation](https://docs.godotengine.org/en/4.7/tutorials/networking/high_level_multiplayer.html), [Photon lag compensation](https://doc.photonengine.com/fusion/v2/manual/advanced/lag-compensation), [WHO alcohol fact sheet](https://www.who.int/news-room/fact-sheets/detail/alcohol)

This document cannot establish real-world enjoyment or physical-device reliability. Its numerical targets still require measured sessions.

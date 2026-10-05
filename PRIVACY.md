# Privacy and network model

Apéro Royale has no account, advertising, analytics or telemetry. It does not run its own player-data server. The game works offline when everyone shares one phone.

## What the host stores

The host device stores player nicknames, languages, selected avatars or optional imported photos, scores, virtual sips, predictions, room-rule votes, game results and the active party in a local SQLite database. Imported photos are downsized and encoded on the device before storage. The all-time and per-game leaderboards are calculated from local history. Uninstalling the app normally removes its data under Android's app-data rules.

## What other phones receive

Joined devices receive the room state needed to play, including nicknames, avatars or optional photos, scores and the current challenge. The host checks player actions and remains authoritative for the result.

- **Wi-Fi:** the host exchanges snapshots and actions over local TCP. This traffic is **not encrypted**; use a trusted network.
- **Bluetooth:** paired Android devices exchange the same state and actions over RFCOMM. Security depends on pairing and the device implementation.
- **Internet:** devices use a TLS MQTT relay. Room payloads, including optional photos and nicknames, are additionally encrypted with AES-GCM using a key derived from the 12-character room code. The relay can see connection metadata and the room topic, but not correctly encrypted payloads without the code. The default `broker.emqx.io` relay is a public third-party test service; a private TLS MQTT relay URL can be entered. Share the code only with participants.

During multi-device parties, the host stores hidden-game progress in the active session. Each player receives their own progress; other players' progress and puzzle seeds are removed from their snapshots. Finished discoveries and any resulting room rule become visible to the group.

In Trivia and Music Quiz, individual answers stay hidden in guest snapshots during the round. The host sends the current room lead and the identity of a friend who has replied. A decision to trust that friend is resolved on the host. Final answers and scores are revealed with the result.

For late admission, a guest's nickname and language go to the host. Individual admission ballots are hidden in guest snapshots; the count and outcome are shared. A declined guest's virtual sip enters the host's local history and leaderboard. The guest's waiting-room arcade runs only on that guest's phone; its progress is not sent to the room.

## Sharing a result

When a player taps **Share**, the app draws a 9:16 PNG card in its local cache. The card includes nicknames, game art and the resolved score or virtual sips, but **no imported profile photos**. Android's share chooser then lets the player select a destination. There is no automatic posting. Once a player sends the card to another app, that app's privacy practices apply.

## Music and microphone

The music menu can open Spotify, Deezer, Apple Music or Amazon Music through an HTTPS link. An optional playlist link and selected service are kept in local app preferences. Apéro Royale does not sign in to music accounts, read their libraries, control external playback, transmit credentials or send playlist links to party peers. Music services play in their own apps or sites under their own terms. The game creates its original music, quiz motifs and SFX locally.

The app does **not** request microphone access. Players report and vote on social-rule infractions themselves; no conversation is monitored.

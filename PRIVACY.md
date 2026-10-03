# Privacy / Confidentialité

## English

Apéro Royale stores each player's nickname, language, selected avatar or optional imported photo, scores, virtual sips, predictions, room-rule votes, game results and the active party in a local SQLite database on the host device. An imported photo is downscaled and encoded locally before storage. The all-time and per-game leaderboards are calculated from that device's local history. Uninstalling the app normally removes its local data under Android's app-data rules.

The app has no Apéro Royale account, ads, analytics or telemetry. It does not operate its own player-data server.

- **One phone:** party data stays on that device.
- **Wi-Fi room:** the host sends game snapshots and receives player actions over local TCP. This traffic is **not encrypted**. Use a trusted Wi-Fi network. Other joined phones receive nicknames, avatars/photos, scores and game state.
- **Bluetooth room:** paired phones exchange the same room state and actions over RFCOMM. The available security depends on the Android Bluetooth pairing and device implementation.
- **Internet room:** devices use a TLS MQTT relay. Room messages, including optional photos and nicknames, are additionally encrypted with AES-GCM using a key derived from the 12-character room code. The relay sees connection metadata and the room topic but cannot read correctly encrypted payloads without the room code. The default `broker.emqx.io` relay is a public test service operated by a third party; users can enter a private TLS MQTT relay URL. Share the room code only with participants.

The game does not request microphone access. Social-rule infractions are reported and judged by players through an in-room vote.

Spotify is optional. If enabled, the app opens Spotify authorization in the browser using OAuth PKCE and sends authorized API requests directly to Spotify to read the selected playlist and control playback. Spotify processes those requests under its own privacy policy. The Spotify Client ID and playlist ID are stored locally. Access and refresh tokens remain in app memory and are discarded when the process ends. The app does not download Spotify audio or send Spotify tokens to peers.

## Français

Apéro Royale conserve localement dans SQLite les pseudos, langues, sprites ou photos facultatives, scores, gorgées virtuelles, pronostics, votes sur les règles, résultats et la partie en cours. Une photo importée est réduite sur l'appareil avant son enregistrement. Les classements historique et par jeu sont calculés depuis l'historique local du téléphone hôte. La désinstallation supprime normalement ces données selon les règles Android.

L'application ne possède ni compte Apéro Royale, ni publicité, ni analytique, ni télémétrie. En Wi-Fi, les données du salon circulent **sans chiffrement** sur le réseau local. En Bluetooth, elles passent entre appareils associés. Sur Internet, les messages passent par un relais MQTT TLS et sont chiffrés en AES-GCM à partir du code de salle ; le relais public proposé par défaut est `broker.emqx.io`, un service de test tiers. Le code doit rester entre participants. Les autres téléphones de la salle reçoivent les pseudos, avatars ou photos, scores et état de la partie. Le jeu ne demande pas l'accès au microphone : les infractions aux règles sociales sont signalées et soumises au vote des joueurs.

Spotify est facultatif. Ses requêtes sont envoyées directement à Spotify après autorisation OAuth PKCE. Le Client ID et la playlist sont enregistrés localement ; les jetons restent uniquement en mémoire pendant la session et ne sont pas envoyés aux autres joueurs. L'application ne télécharge pas l'audio Spotify.

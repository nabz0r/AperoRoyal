# Privacy / Confidentialité

## English

Apéro Royale stores each player's nickname, language, selected avatar or optional imported photo, scores, virtual sips, predictions, room-rule votes, game results and the active party in a local SQLite database on the host device. An imported photo is downscaled and encoded locally before storage. The all-time and per-game leaderboards are calculated from that device's local history. Uninstalling the app normally removes its local data under Android's app-data rules.

The app has no Apéro Royale account, ads, analytics or telemetry. It does not operate its own player-data server.

- **One phone:** party data stays on that device.
- **Wi-Fi room:** the host sends game snapshots and receives player actions over local TCP. This traffic is **not encrypted**. Use a trusted Wi-Fi network. Other joined phones receive nicknames, avatars/photos, scores and game state.
- **Bluetooth room:** paired phones exchange the same room state and actions over RFCOMM. The available security depends on the Android Bluetooth pairing and device implementation.
- **Internet room:** devices use a TLS MQTT relay. Room messages, including optional photos and nicknames, are additionally encrypted with AES-GCM using a key derived from the 12-character room code. The relay sees connection metadata and the room topic but cannot read correctly encrypted payloads without the room code. The default `broker.emqx.io` relay is a public test service operated by a third party; users can enter a private TLS MQTT relay URL. Share the room code only with participants.

The game does not request microphone access. Social-rule infractions are reported and judged by players through an in-room vote.

The music menu can open Spotify, Deezer, Apple Music or Amazon Music through an HTTPS link. An optional playlist link and the selected service are stored in local app preferences. Apéro Royale does not connect to a music account, read its library, control external playback, transmit music credentials or send the playlist link to party peers. The chosen music app or website processes playback under its own terms and privacy policy. Original game music and sound effects are generated locally.

## Français

Apéro Royale conserve localement dans SQLite les pseudos, langues, sprites ou photos facultatives, scores, gorgées virtuelles, pronostics, votes sur les règles, résultats et la partie en cours. Une photo importée est réduite sur l'appareil avant son enregistrement. Les classements historique et par jeu sont calculés depuis l'historique local du téléphone hôte. La désinstallation supprime normalement ces données selon les règles Android.

L'application ne possède ni compte Apéro Royale, ni publicité, ni analytique, ni télémétrie. En Wi-Fi, les données du salon circulent **sans chiffrement** sur le réseau local. En Bluetooth, elles passent entre appareils associés. Sur Internet, les messages passent par un relais MQTT TLS et sont chiffrés en AES-GCM à partir du code de salle ; le relais public proposé par défaut est `broker.emqx.io`, un service de test tiers. Le code doit rester entre participants. Les autres téléphones de la salle reçoivent les pseudos, avatars ou photos, scores et état de la partie. Le jeu ne demande pas l'accès au microphone : les infractions aux règles sociales sont signalées et soumises au vote des joueurs.

Le menu musical peut ouvrir Spotify, Deezer, Apple Music ou Amazon Music par un lien HTTPS. La plateforme choisie et un éventuel lien de playlist sont conservés dans les préférences locales. Apéro Royale ne se connecte pas à un compte musical, ne lit pas sa bibliothèque, ne contrôle pas la lecture externe et n'envoie ni identifiants ni playlist aux autres joueurs. La lecture est gérée par l'application ou le site choisi, selon ses propres conditions et règles de confidentialité. La musique originale et les effets du jeu sont produits localement.

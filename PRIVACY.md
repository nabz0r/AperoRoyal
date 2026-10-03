# Privacy / Confidentialité

Apéro Royale stores nicknames, chosen languages, avatars, game results and lifetime stats in a local SQLite database on the Android device. It does not run its own analytics, advertising service, cloud account or telemetry.

When a player uses a same-Wi-Fi room, nicknames, languages, scores and game state are transmitted over the local network to the host and other joined phones. The room uses a six-digit PIN but LAN traffic is not encrypted. Use a trusted private Wi-Fi network.

Spotify is optional. If enabled, Apéro Royale opens Spotify authorization in the browser using OAuth PKCE and sends authorized API requests directly to Spotify to read the selected playlist and control playback. Spotify may process data under its own privacy policy. The Spotify Client ID and playlist ID are saved on device. Access and refresh tokens are kept only in app memory and discarded when the app process ends. Apéro Royale does not download Spotify audio or transmit tokens to its LAN peers.

Uninstalling the app removes its local database under Android's normal app-data rules. No personal data is collected by the project operator.

---

Apéro Royale conserve les pseudos, langues, avatars, résultats et statistiques dans une base SQLite sur l'appareil. L'application n'utilise ni publicité, ni télémétrie, ni compte cloud. En mode Wi-Fi local, l'état de la partie est transmis aux téléphones du salon, sans chiffrement ; utilisez un réseau privé de confiance. Spotify est facultatif et ses jetons restent uniquement en mémoire pendant la session. Désinstaller l'application supprime normalement ses données locales.

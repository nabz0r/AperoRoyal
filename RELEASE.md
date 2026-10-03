# Apéro Royale 1.1.0 — Arcade sociale

- [APK Android signé](releases/AperoRoyale-v1.1.0.apk) · `com.aperoroyale` · versionCode `3` · Android 8.0+ (API 26)
- SHA-256 : `bfa425f671aa445b64cab594b0d0efed617a25cc347902c01e26027d4cda95e5`
- Certificat de signature SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` (même clé que 1.0.1)

## Nouveautés

Direction artistique retravaillée autour d’une fête urbaine rétro originale, six portraits illustrés, scènes animées et sprites propres aux dix mini-jeux. La musique procédurale a été reconstruite avec un groove continu et des effets plus doux ; elle reste coupée par défaut. Le menu permet de régler séparément musique, volume, effets et vibrations.

Le joueur actif mise 1 à 3 gorgées virtuelles ; **tous les autres pronostiquent** le résultat pour marquer des points. Les poses et les bluffs passent devant un jury au lieu d’un bouton d’auto-validation. La roulette révèle son gobelet, la mémoire monte en difficulté et les jeux de rythme/réflexe utilisent des commandes réseau adaptées à la latence. Un classement par défi rejoint les classements de soirée et historiques.

Onze secrets sont cachés dans la partie, dont le chat pixel. Le premier secret permet de choisir parmi dix règles applicables à toute la salle. Les infractions sociales sont signalées par les joueurs et jugées par vote sur leurs téléphones. Le jeu n’utilise pas le microphone.

Le README et les guides expliquent le parcours, et les dix écrans de jeu sont capturés dans [`docs/screenshots/games`](docs/screenshots/games).

## Vérifications effectuées

- `./gradlew test assembleDebug assembleRelease` et `lintVitalRelease` : réussis. Le projet n’a pas encore de tests unitaires Gradle (`NO-SOURCE`).
- `tools/smoke_v110.py` sur émulateur Android 16 : les dix mini-jeux terminés, alternance de deux joueurs sur un même téléphone, dix mises, dix pronostics, enregistrement SQLite et captures des dix écrans.
- Deux émulateurs Android 16 et Android 8.0 : un invité rejoint le salon Wi-Fi via redirection TCP, vote depuis son téléphone, mise sur son propre tour, termine le défi de rythme à distance et participe au vote d’une règle ; la pénalité et l’historique sont synchronisés sur l’hôte.
- Salon Internet via le relais public MQTT TLS : le second émulateur rejoint avec le code à 12 caractères, reçoit le lobby, vote depuis son téléphone et la transition est synchronisée.
- Import de photo via le sélecteur Android sur émulateur : la photo de test devient le portrait du profil et reste enregistrée dans la session SQLite après réduction locale.
- L’APK signé s’installe sur l’émulateur Android 8.0 au-dessus de la version 1.0.1, démarre sans erreur fatale observée et annonce la version 1.1.0.
- `apksigner verify` : certificat valide ; `aapt` : versionCode 3 et API minimale 26 ; SHA-256 calculé sur l’APK livré.

## Limites vérifiées honnêtement

L’intégration Bluetooth RFCOMM exige deux téléphones Android appairés et n’a pas été validée sur émulateur. Le salon Internet a été vérifié jusqu’au vote et à la transition ; un tour complet à distance via le relais public n’a pas été rejoué dans ce cycle. Spotify demande un compte et un appareil actif et n’a pas été revalidé. L’import a été testé avec une image de démonstration, pas une galerie personnelle réelle. Le relais Internet par défaut est un [service public de test EMQX](https://www.emqx.com/en/mqtt/public-mqtt5-broker) sans garantie de disponibilité. L’hôte doit garder l’application ouverte pendant une partie à plusieurs téléphones.

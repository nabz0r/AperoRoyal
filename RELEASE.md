# Apéro Royale 1.2.0 — Le tour passe enfin

- [APK Android signé](releases/AperoRoyale-v1.2.0.apk) · `com.aperoroyale` · versionCode `4` · Android 8.0+ (API 26)
- SHA-256 de l’APK : `037f250b157d30595c8d6dc773844ce3a8210fd8c5aa39118552c3dbaf0cba5f`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que les versions précédentes

## Ce qui change

**Le tour de chacun est explicite.** L’écran de passage annonce le joueur et le défi, puis attend son « Je suis prêt » avant la mise. À deux sur un téléphone, le premier vote part désormais du joueur actif ; après chaque manche, l’ordre s’inverse. Les pronostics, jurys et dessins à deviner ont leur propre relais privé. Le second joueur intervient donc pendant chaque manche au lieu d’attendre passivement la suivante.

**Mode Turbo.** Le lobby propose maintenant Vote, Libre et Turbo. Turbo choisit un défi surprise sans attendre le scrutin et raccourcit les chronos de jeu ; les mises et pronostics restent présents.

**Décors et son.** Dix décors rétro nocturnes originaux habillent les mini-jeux et l’écran de passage, sans masquer les commandes. Le morceau procédural est moins répétitif, démarre à volume réduit et marque le changement de joueur avec un court signal. Musique, volume, style, SFX et vibrations restent réglables.

**Dessin en réseau.** Le devineur distant choisit sa réponse depuis son propre téléphone ; l’hôte vérifie cette action avant d’attribuer le résultat. Le devineur local reçoit un écran de passage avant de voir les réponses.

Le [README](README.md) présente le nouveau parcours et les captures des dix jeux ; [l’écran de passage](docs/screenshots/handoff.png) montre le moment « à toi de jouer ».

## Vérifications

- `./gradlew test assembleDebug assembleRelease lintVitalRelease` : réussite. Les tâches unitaires Gradle indiquent encore `NO-SOURCE` ; les parcours réels sont couverts par les scripts ci-dessous.
- `tools/smoke_v120.py` sur émulateur Android 16 : dix jeux terminés, deux profils alternés, dix mises et pronostics, relais du dessin, historique SQLite et dix nouvelles captures.
- `tools/smoke_turns.py` : deux manches Vote complètes, premier votant et joueur actif alternés.
- `tools/smoke_turbo.py` : deux manches Turbo, sélection directe et alternance des joueurs.
- Deux émulateurs Android 16 et Android 8.0 en Wi-Fi via redirection locale : le second téléphone rejoint la salle, vote, reçoit son tour, valide lui-même « Je suis prêt » et place sa mise. Le jeu de dessin a aussi été terminé avec l’artiste sur l’hôte et le devineur sur l’autre téléphone ; le résultat a été enregistré sur l’hôte.
- L’APK signé s’installe en mise à jour sur Android 8.0. `aapt` annonce versionCode 4 / minSdk 26 ; `apksigner verify` valide la signature et le certificat ci-dessus.

## Limites actuelles

Bluetooth RFCOMM demande deux téléphones appairés et n’a pas été revalidé sur les émulateurs. Le salon Internet dépend d’un relais MQTT TLS public de test ; la version précédente a été vérifiée jusqu’au vote et à la transition, mais cette version n’a pas refait un tour complet sur ce relais. Spotify demande une configuration et un appareil compatible ; aucun compte Spotify réel n’a été utilisé dans cette validation. Le jeu en réseau garde l’hôte ouvert pendant la partie.

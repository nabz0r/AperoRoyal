# Apéro Royale 1.0.1

- APK Android signé : [`releases/AperoRoyale-v1.0.1.apk`](releases/AperoRoyale-v1.0.1.apk)
- Package : `com.aperoroyale` ; versionCode `2` ; Android 8.0+ (API 26)
- SHA-256 de l’APK : `52d7d0654f20c699b3d021c644c140e021af1620b5a499eab7f0acea79fda39e`
- Certificat de signature SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` (identique à 1.0.0, mise à jour en place possible)

## Ce qui change

Nouvelle direction arcade pixel, illustrations et avatars originaux, interface de vote et de résultat repensée. Deux à six joueurs peuvent se passer un téléphone ou rejoindre une même salle en Wi-Fi, Bluetooth appairé ou Internet avec code. Le groupe vote pour le prochain défi ; le mode libre donne accès aux dix jeux. Chaque tour offre une mise de une à trois gorgées virtuelles, un drop aléatoire et un classement persistant. Chaque défi possède son guide FR/EN. Le chat pixel déclenche une règle de salle après vingt touches. La musique démarre coupée et possède des réglages séparés pour son style, son volume, les effets et les vibrations.

## Vérifications effectuées

- `./gradlew test assembleDebug assembleRelease` et `lintVitalRelease` : réussis. Le module ne contient actuellement aucun test unitaire Gradle ; `test` indique `NO-SOURCE`.
- Parcours automatisé sur émulateur Android 16 : dix mini-jeux, rotation de deux joueurs sur un téléphone, mises, mode libre et dix entrées d’historique SQLite.
- Deux émulateurs Android 16 et Android 8.0 : salle Internet via relais TLS public, arrivée du second joueur, synchronisation du lobby, votes depuis les deux appareils, tour du joueur invité, mise, action de réflexe à distance et résultat synchronisé.
- Mise à jour installée de l’APK 1.0.0 à 1.0.1 sur émulateur Android 8.0 : signature acceptée, partie et classement conservés.
- `apksigner verify` : signature valide ; `aapt` : version 1.0.1 et API minimale 26.

## Limites de validation

Le Bluetooth RFCOMM demande deux téléphones Android appairés et n’a pas pu être testé sur émulateur. L’import de photo n’a pas été testé avec une galerie réelle. Spotify demande un Client ID développeur, un compte autorisé et un appareil Premium actif : son flux n’a pas été testé dans cette livraison. Le relais Internet par défaut est un [service public de test EMQX](https://www.emqx.com/en/mqtt/public-mqtt5-broker) sans garantie de disponibilité ; un relais MQTT TLS privé peut être renseigné dans les menus. L’hôte doit garder l’application ouverte pendant une partie à plusieurs téléphones.

# APÉRO ROYALE 👑

**La borne d'arcade de la soirée, dans ta poche.** Dix défis courts, 2 à 6 joueurs, des paris en gorgées, un vote pour le prochain jeu et un chat pixel qui cache une règle secrète. Fonctionne hors ligne sur un téléphone ; les amis peuvent aussi rejoindre depuis leurs téléphones en Wi-Fi, Bluetooth ou Internet.

![Écran d'accueil Apéro Royale](docs/screenshots/home.png)

> [Télécharger l'APK Android 1.0.1](releases/AperoRoyale-v1.0.1.apk) · Android 8.0 ou plus récent · [Notes de version](RELEASE.md)

## La soirée en 30 secondes

1. Crée une partie, ajoute les joueurs et choisis pour chacun **FR ou EN**, un sprite ou une photo personnelle. Chaque joueur a son propre pseudo, score et compteur de gorgées.
2. Choisis **Vote arcade** : tout le monde vote parmi trois défis proposés. Ou choisis **Jeu libre** pour lancer n'importe lequel des dix mini-jeux.
3. Le téléphone passe au joueur du tour. Celui-ci mise **1, 2 ou 3 gorgées virtuelles** : victoire = points bonus ; défaite = la mise rejoint sa jauge. Les « drops » aléatoires changent le tour : Turbo, Bouclier ou Temps bonus.
4. La partie affiche les résultats, le classement de la soirée et les statistiques historiques. L'état et l'historique restent dans SQLite sur le téléphone hôte ; la partie reprend après fermeture de l'application.

Les gorgées sont un compteur de jeu. Chacun choisit librement sa boisson ; eau et sans alcool fonctionnent tout aussi bien.

![Vote pour le prochain jeu](docs/screenshots/vote.png)

## 10 défis qui changent le rythme

| Jeu | Le défi à relever |
| --- | --- |
| **Culture G** | Quatre réponses, seize secondes. Une erreur et la mise est perdue. |
| **Positions à la con** | Prends une pose absurde ; le groupe juge la performance. |
| **Blind Test** | Reconnais une mélodie originale hors ligne ou un titre de ta playlist Spotify configurée. |
| **Réflexe néon** | Frappe dix cibles mouvantes avant la fin du chrono. |
| **Roulette Royale** | Choisis parmi six gobelets ; une plus grosse mise ajoute des pièges. |
| **Dessin maudit** | Dessine un mot sans parler, puis passe la main au devineur. |
| **Mémoire flash** | Observe une séquence puis reproduis-la sous pression. |
| **Rythme ou rien** | Tape quatre fois au centre du beat. |
| **Bluff royal** | Vends ton histoire au groupe sans te faire démasquer. |
| **Bombe à bulles** | Huit touches en relais, chacun à son tour avant l'explosion. |

**Guides intégrés :** un écran de règles et d'astuces pour chacun des dix jeux est accessible depuis l'accueil. Après un tour, le chat pixel attend la sélection suivante. Le premier joueur qui le touche **20 fois** choisit une règle valable pour toute la partie : double XP, aucune gorgée, ou ordre des tours inversé.

![Mini-jeu Réflexe néon](docs/screenshots/game.png)

## Jouer ensemble

| Configuration | Mise en route | Ce qui est partagé |
| --- | --- | --- |
| **Un seul téléphone** | Ajoute 2 à 6 joueurs dans le lobby. | Votes secrets avec écran « passe le téléphone », tours, paris et résultats. |
| **Même Wi-Fi** | L'hôte touche **Héberger → Wi-Fi**. Les invités saisissent l'IP locale et le PIN à six chiffres. | Le téléphone hôte arbitre la partie et synchronise les écrans par TCP (port 43867). |
| **Bluetooth** | Associe d'abord les téléphones dans les réglages Android. L'hôte choisit **Bluetooth**, les invités sélectionnent le téléphone associé et saisissent le PIN. | Salle de proximité via Bluetooth RFCOMM. |
| **Internet** | L'hôte choisit **Internet**, puis communique le code à 12 caractères. Les invités choisissent **Rejoindre → Internet**. | Salle synchronisée via un relais MQTT TLS ; les messages sont chiffrés de bout en bout avec une clé dérivée du code. |

Un salon peut accueillir **six joueurs au total** : plusieurs joueurs sur le téléphone hôte et des invités sur leurs propres appareils. Le vote et les actions du joueur actif sont transmis au téléphone hôte. Les invités peuvent choisir leur langue et personnaliser leur avatar ; l'interface du vote suit la langue de l'appareil joueur et les défis suivent celle du joueur du tour.

Le relais Internet proposé par défaut, [`ssl://broker.emqx.io:8883`](https://www.emqx.com/en/mqtt/public-mqtt5-broker), est un **service public de test**, sans garantie de disponibilité. L'application retente automatiquement une connexion initiale coupée. Le champ « TLS relay URL » permet d'utiliser un serveur MQTT TLS privé compatible. Le code de salle sert aussi de secret de chiffrement : partage-le uniquement avec les participants. Le mode Wi-Fi local n'est pas chiffré ; utilise un réseau de confiance. Bluetooth dépend du support et de l'association des appareils Android.

## Une ambiance que tu contrôles

- **Musique coupée par défaut.** Deux styles de chiptune synthétisée, *Chill* et *Arcade*, et trois niveaux de volume dans le menu de soirée.
- **SFX et vibrations** réglables séparément. La musique baisse pendant le Blind Test ; les effets restent audibles.
- **Spotify facultatif.** L'application utilise OAuth PKCE et Spotify Web API pour lire une playlist et piloter un appareil Spotify actif. Il faut un Client ID, un compte Premium et un appareil de lecture actif pour la lecture Web API. Sans Spotify, six mélodies originales sont incluses hors ligne.
- **Photo de profil facultative.** Une image choisie dans la galerie est réduite localement avant d'être partagée avec les autres téléphones de la salle.

Pour Spotify, renseigne le **Client ID** et la playlist dans **Musique / Spotify**, puis enregistre `http://127.0.0.1:43868/callback` comme URI de redirection dans le Spotify Developer Dashboard. Aucun client secret n'est intégré à l'APK ; les jetons restent en mémoire pendant la session.

## Build et tests

Projet Android natif Java, JDK 17, Android SDK 36 et Gradle 8.14.3. Le projet ne requiert pas de serveur pour le jeu local.

```sh
./gradlew test assembleDebug
```

Pour créer un APK release installable, prépare une clé PKCS12 privée et un fichier `signing.properties` local, ignoré par Git :

```properties
storeFile=/absolute/path/release.p12
storePassword=your-password
keyAlias=apero
keyPassword=your-password
```

Puis lance `./gradlew assembleRelease`. Le résultat signé est `app/build/outputs/apk/release/app-release.apk`. Garde la même clé pour les mises à jour Android. Le script `tools/smoke_v101.py` parcourt les dix jeux sur un émulateur avec un APK debug installé ; voir [RELEASE.md](RELEASE.md) pour les vérifications de la version publiée.

## Architecture et confidentialité

`GameEngine` détient la rotation, les votes, les paris et les règles. `GameStore` sauvegarde la session, les résultats et les classements dans SQLite. `ArcadeView` dessine toute l'interface arcade et les sprites. `PartyNetwork`, `BluetoothPartyNetwork` et `InternetPartyNetwork` transportent les commandes et instantanés de l'hôte. `ArcadeAudio` synthétise musique et effets ; `SpotifyBridge` gère l'intégration facultative.

Pas de compte Apéro Royale, de publicité ni de télémétrie. [Détails sur les données et les salons](PRIVACY.md). Code sous [licence MIT](LICENSE). Dépendance Internet : Eclipse Paho MQTT Java (EPL 2.0 / EDL 1.0), voir [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

---

**English:** Apéro Royale is a 2–6 player retro party game for Android 8+. Play pass-and-play offline or join friends over Wi-Fi, paired Bluetooth or an Internet room code. Vote on the next challenge, wager virtual sips, customize avatars, explore ten mini-games and unlock the pixel cat's room-wide rule. Music starts off and can be configured independently from sound effects. The ten in-app tutorials and gameplay switch between French and English for each player.

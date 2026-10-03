# APÉRO ROYALE 👑

**La soirée devient une borne d’arcade.** Un jeu d’apéro Android à partager entre 2 et 6 amis, sur **un seul téléphone** ou sur **plusieurs téléphones**. Votez pour la prochaine épreuve, pariez 1 à 3 gorgées virtuelles, pronostiquez la réussite des autres et faites vivre les règles secrètes de la salle.

![Accueil Apéro Royale](docs/screenshots/home.png)

[**Télécharger l’APK signé 1.1.0**](releases/AperoRoyale-v1.1.0.apk) · Android 8.0+ · [Détails de la release](RELEASE.md) · [Confidentialité](PRIVACY.md)

## La boucle de jeu

1. **Créez la salle.** Chaque personne prend un pseudo, une langue FR/EN et un portrait exclusif parmi six sprites. Une vraie photo peut aussi être importée depuis l’appareil.
2. **La salle choisit.** Trois jeux apparaissent ; chaque joueur vote depuis son téléphone. Le mode libre permet à l’hôte de lancer directement n’importe lequel des dix jeux.
3. **Le joueur actif mise.** Il choisit 1, 2 ou 3 gorgées virtuelles. Les amis pronostiquent sa victoire ou sa défaite : **+35 points** pour un bon prono, **+1 gorgée virtuelle** pour un mauvais.
4. **Tout le monde joue.** Défis de jury, dessin à deviner, bombe en relais, votes de règles et pronostics impliquent le groupe. Le joueur actif change à chaque tour.
5. **On recommence.** Points, gorgées, séries et classements sont sauvegardés sur le téléphone hôte. La partie se reprend après fermeture de l’application.

L’eau, les boissons sans alcool et les défis sans consommation ont toute leur place. Les gorgées affichées sont des **compteurs de jeu** ; chacun décide librement de ce qu’il boit.

![Vote du prochain jeu](docs/screenshots/vote.png)

## Dix mini-jeux, dix scènes et sprites dédiés

Chaque jeu possède sa propre scène animée, son sprite pixel et un guide intégré FR/EN. Le menu **Découvrir les 10 défis** explique les règles avant la partie.

| # | Mini-jeu | Moment de soirée |
| --- | --- | --- |
| 01 | **Culture G** | Un QCM tordu ; répondre vite rapporte un bonus. |
| 02 | **Positions à la con** | Une pose absurde à défendre devant le jury des amis. |
| 03 | **Blind Test** | Reconnaître un air original hors ligne ou un titre Spotify configuré. |
| 04 | **Réflexe néon** | Dix cibles à attraper avant la fin du chrono, même sur un téléphone invité. |
| 05 | **Roulette Royale** | Six gobelets, des pièges révélés après votre choix et une mise qui augmente le risque. |
| 06 | **Dessin maudit** | Dessiner un concept impossible puis passer au devineur. |
| 07 | **Mémoire flash** | Rejouer une séquence de plus en plus longue. |
| 08 | **Rythme ou rien** | Quatre frappes au bon moment sur un beat partagé. |
| 09 | **Bluff royal** | Vendre une histoire loufoque ; le jury tranche. |
| 10 | **Bombe à bulles** | Huit touches en relais, joueur après joueur, avant l’explosion. |

<table>
<tr><td><img src="docs/screenshots/games/trivia.png" alt="Culture G" width="180"></td><td><img src="docs/screenshots/games/poses.png" alt="Positions à la con" width="180"></td><td><img src="docs/screenshots/games/blind-test.png" alt="Blind Test" width="180"></td><td><img src="docs/screenshots/games/reflex.png" alt="Réflexe néon" width="180"></td><td><img src="docs/screenshots/games/roulette.png" alt="Roulette Royale" width="180"></td></tr>
<tr><td>Culture G</td><td>Positions</td><td>Blind Test</td><td>Réflexe</td><td>Roulette</td></tr>
<tr><td><img src="docs/screenshots/games/drawing.png" alt="Dessin maudit" width="180"></td><td><img src="docs/screenshots/games/memory.png" alt="Mémoire flash" width="180"></td><td><img src="docs/screenshots/games/rhythm.png" alt="Rythme ou rien" width="180"></td><td><img src="docs/screenshots/games/bluff.png" alt="Bluff royal" width="180"></td><td><img src="docs/screenshots/games/bomb.png" alt="Bombe à bulles" width="180"></td></tr>
<tr><td>Dessin</td><td>Mémoire</td><td>Rythme</td><td>Bluff</td><td>Bombe</td></tr>
</table>

## Les secrets de la salle 🐈

Le chat pixel revient pendant le choix du prochain jeu. Le premier à le toucher **20 fois** déclenche un secret. Dix autres secrets récompensent des exploits cachés dans les mini-jeux. Celui qui ouvre le premier secret choisit une **règle valable pour toute la salle**, synchronisée sur tous les téléphones : mot « oui » interdit, pseudos tabous, questions seulement, toast obligatoire, interdiction de pointer du doigt, et d’autres surprises. Si quelqu’un enfreint une règle sociale, un joueur le signale et **la salle vote** avant d’appliquer une gorgée virtuelle. Le jeu n’active pas le microphone pour surveiller les conversations.

## Un téléphone ou plusieurs

| Connexion | Mise en place | Usage |
| --- | --- | --- |
| **Un téléphone** | Ajoutez 2 à 6 joueurs dans le lobby. | Chacun prend son tour ; votes, paris, jurys et défis en relais passent d’une main à l’autre. |
| **Wi-Fi** | L’hôte ouvre une salle Wi-Fi ; les amis entrent son IP et le PIN à six chiffres. | Actions et votes synchronisés sur le réseau local, port TCP 43867. |
| **Bluetooth** | Associez les appareils dans Android, puis hébergez/rejoignez avec le PIN. | Salle de proximité via RFCOMM sur appareils compatibles. |
| **Internet** | L’hôte partage le code de salle à 12 caractères. | Synchronisation via relais MQTT TLS avec messages chiffrés en AES-GCM. |

L’hôte garde la partie ouverte et arbitre les actions. Les invités peuvent voter, pronostiquer, jouer leur tour et participer aux jurys depuis leur appareil. Les portraits restent uniques dans la salle ; les photos facultatives sont réduites localement avant partage. L’interface suit le français ou l’anglais du joueur concerné.

Le relais Internet par défaut, [`ssl://broker.emqx.io:8883`](https://www.emqx.com/en/mqtt/public-mqtt5-broker), est un **service public de test**. Son accès et sa disponibilité ne sont pas garantis ; le menu accepte un autre relais MQTT TLS compatible. Le Wi-Fi local ne chiffre pas ses messages : utilisez un réseau de confiance. Le Bluetooth dépend de l’appairage et du support Android.

## Une ambiance que vous contrôlez

La musique commence **désactivée**. Le menu sépare musique, style *Chill / Arcade*, volume, effets sonores et vibrations. La bande son originale est synthétisée dans l’application, sans téléchargement ; elle baisse pendant le Blind Test. L’ambiance a été reconstruite en 1.1.0 avec un groove continu et des effets plus doux.

**Spotify est facultatif.** Configurez votre Client ID et une playlist dans **Musique / Spotify**, puis ajoutez `http://127.0.0.1:43868/callback` aux URI de redirection de votre application Spotify Developer. L’intégration OAuth PKCE utilise la Web API et demande un compte Premium et un appareil Spotify actif pour contrôler la lecture. Sans Spotify, le Blind Test utilise six mélodies originales hors ligne.

## Pour développer

Android natif Java, JDK 17, Android SDK 36 et Gradle 8.14.3. Aucun serveur n’est requis pour le jeu local.

```sh
./gradlew test assembleDebug
ADB_SERIAL=emulator-5554 python3 tools/smoke_v110.py
```

Pour créer une mise à jour signée, utilisez votre propre clé PKCS12 et placez ses paramètres dans `signing.properties` à la racine ; ce fichier reste hors Git :

```properties
storeFile=/absolute/path/release.p12
storePassword=your-password
keyAlias=apero
keyPassword=your-password
```

`./gradlew assembleRelease` produit `app/build/outputs/apk/release/app-release.apk`. Conservez la même clé pour permettre les mises à jour Android.

`GameEngine` arbitre les tours, votes, pronostics, règles et scores. `GameStore` conserve session, historique, statistiques par joueur et par jeu dans SQLite. `ArcadeView` et `GameSprites` dessinent les écrans et scènes. `PartyNetwork`, `BluetoothPartyNetwork` et `InternetPartyNetwork` transportent les commandes et instantanés. `ArcadeAudio` produit musique et effets ; `SpotifyBridge` gère l’option Spotify.

Illustrations originales dans [`art/source`](art/source), ressources compressées sous `app/src/main/res/drawable-nodpi`. Licence [MIT](LICENSE). Pas de compte Apéro Royale, de publicité ni de télémétrie. [Notes sur les données](PRIVACY.md) et [licences tierces](THIRD_PARTY_NOTICES.md).

---

**English:** Apéro Royale 1.1.0 is a French/English Android party arcade for 2–6 friends. Play on one phone or join a room over Wi-Fi, paired Bluetooth or an Internet code. Everyone votes on the next game and predicts the active player’s result; social challenges use a jury. Ten games have dedicated pixel sprites and guides. Eleven hidden achievements reveal a room-wide rule, enforced by group vote. Music starts muted, and optional photos and Spotify can be configured in the menus. The signed APK is linked above.

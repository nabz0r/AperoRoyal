# APÉRO ROYALE 👑

**La soirée devient une borne d’arcade.** Un jeu d’apéro Android pour 2 à 6 amis, sur **un seul téléphone** ou **plusieurs téléphones**. Un joueur prend la lumière à chaque manche ; tous les autres votent, pronostiquent, jugent, dessinent ou désamorcent avec lui.

![Accueil Apéro Royale 1.4.1](docs/screenshots/home.png)

[**Télécharger l’APK signé 1.4.1**](releases/AperoRoyale-v1.4.1.apk) · Android 8.0+ · [Détails de la release](RELEASE.md) · [Confidentialité](PRIVACY.md)

## La boucle de jeu

1. **Créez la salle.** Chaque personne prend un pseudo, une langue FR/EN et un portrait exclusif parmi six sprites. Une vraie photo peut aussi être importée depuis l’appareil.
2. **La salle choisit.** En mode Vote, chacun choisit parmi trois défis. En mode Libre, le groupe ouvre le catalogue ; en mode **Turbo**, le jeu enchaîne des défis surprises plus courts.
3. **Le téléphone passe.** L’écran annonce le numéro de tour, le pseudo et le défi. Le chrono reste arrêté jusqu’à ce que le joueur concerné touche **Je suis prêt** sur le téléphone partagé ou le sien.
4. **Le joueur actif mise.** Il choisit 1, 2 ou 3 gorgées virtuelles. Les amis choisissent de **le couvrir** ou **le défier**. Une couverture réussie rapporte 35 points, peut ajouter deux secondes aux jeux chronométrés et retire une gorgée virtuelle en cas d’échec. Un défi réussi rapporte 50 points au pronostiqueur et augmente de 25 points le gain du joueur actif s’il gagne. Un mauvais choix ajoute une gorgée virtuelle. Sur un téléphone, pronostics et jurys ont un passage privé : le chrono attend que la personne suivante touche **C’est moi**.
5. **Tout le monde joue.** Les poses passent devant le jury ; le bluff oppose la vérité secrète du conteur aux votes des amis ; le dessin passe au devineur ; la bombe demande deux actions par joueur avant de circuler. Le joueur actif **alterne à chaque manche**, et le premier votant suit l’ordre des tours.
6. **On recommence.** Points, gorgées, séries et classements sont sauvegardés sur le téléphone hôte. La partie se reprend après fermeture de l’application.

**Confort 1.4.0.** L’accueil, la palette, l’icône et les réglages adoptent une direction nocturne plus adulte. La source musicale se choisit dans une liste claire ; un bouton dans l’en-tête ouvre l’application choisie pendant la partie. La bande originale du jeu se tait lorsqu’une source externe ou le silence est sélectionné. Effets et vibrations restent séparés. Les écrans s’adaptent aussi aux téléphones 16:9 ; les chronos locaux reprennent après un passage dans les réglages ou en arrière-plan.

**Fiabilité 1.4.1.** [500 soirées simulées](docs/SIMULATION_500_PARTIES.md) ont traversé les dix jeux et les trois modes, avec 7 990 manches et 40 190 restaurations d’état. Deux anomalies ont été corrigées : le choix de règle secrète en Turbo et le verdict du jury à l’expiration du chrono dans le moteur.

L’eau, les boissons sans alcool et les défis sans consommation ont toute leur place. Les gorgées affichées sont des **compteurs de jeu** ; chacun décide librement de ce qu’il boit.

![Vote du prochain jeu](docs/screenshots/vote.png)

![Passage au joueur suivant avant le défi](docs/screenshots/handoff.png)

## Dix mini-jeux, dix scènes et sprites dédiés

Chaque jeu possède désormais un **décor nocturne dédié**, un sprite pixel animé et un guide intégré FR/EN. Les commandes restent lisibles sur ces scènes. Le menu **Découvrir les 10 défis** explique les règles avant la partie.

| # | Mini-jeu | Moment de soirée |
| --- | --- | --- |
| 01 | **Culture G** | Un QCM tordu ; répondre vite rapporte un bonus. |
| 02 | **Positions à la con** | Une pose absurde à défendre devant le jury des amis. |
| 03 | **Blind Test** | Reconnaître un motif sonore original hors ligne (montée, descente, alternance, échos…). |
| 04 | **Réflexe néon** | Dix cibles à attraper avant la fin du chrono, même sur un téléphone invité. |
| 05 | **Roulette Royale** | Six gobelets, des pièges révélés après votre choix et une mise qui augmente le risque. |
| 06 | **Dessin maudit** | Dessiner un concept impossible puis passer au devineur. |
| 07 | **Mémoire flash** | Rejouer une séquence de plus en plus longue. |
| 08 | **Rythme ou rien** | Quatre frappes validées sur l’horloge de la manche par l’hôte. |
| 09 | **Bluff royal** | Raconter une anecdote vraie ou inventée ; le jury devine, le conteur marque s’il trompe la majorité. |
| 10 | **Bombe à bulles** | Deux touches par joueur en relais, jusqu’à huit touches minimum, avant l’explosion. |

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

L’hôte garde la partie ouverte et arbitre les actions. Les invités peuvent voter, pronostiquer, jouer leur tour et participer aux jurys depuis leur appareil. Les réponses, pièges et votes avant révélation sont retirés des instantanés envoyés à chaque rôle. Les portraits restent uniques dans la salle ; les photos facultatives sont réduites localement avant partage. L’interface suit le français ou l’anglais du joueur concerné. Le classement historique est enregistré sur l’hôte ; les profils de même pseudo créés lors de soirées différentes partagent encore leurs statistiques historiques.

Le relais Internet par défaut, [`ssl://broker.emqx.io:8883`](https://www.emqx.com/en/mqtt/public-mqtt5-broker), est un **service public de test**. Son accès et sa disponibilité ne sont pas garantis ; le menu accepte un autre relais MQTT TLS compatible. Le code de salle est partagé entre invités : il ne protège pas les messages d’un participant malveillant qui connaît ce code. Le Wi-Fi local ne chiffre pas ses messages : utilisez un réseau de confiance. Le Bluetooth dépend de l’appairage et du support Android.

## Une ambiance que vous contrôlez

La bande originale démarre discrètement à **18 %** pour une nouvelle installation, avec quatre variations de phrase, un timbre par jeu, une basse douce et un signal court lors du passage de tour. Le menu **Musique** permet de choisir bande originale, Spotify, Deezer, Apple Music, Amazon Music ou silence. Pour les services externes, collez facultativement une URL HTTPS de playlist puis touchez **Ouvrir l’application**. Le raccourci ♫ dans l’en-tête permet de rouvrir le service pendant la partie. La lecture et ses commandes restent dans l’application musicale choisie ; aucun compte ou abonnement n’est fourni par le jeu. Le menu distingue aussi style *Chill / Arcade*, volume de la bande originale, effets et vibrations. Les réglages sont mémorisés localement.

La version 1.4.0 retire l’ancien contrôle OAuth et le quiz fondé sur la Web API Spotify : la [Developer Policy Spotify](https://developer.spotify.com/policy) interdit les jeux et quiz construits avec sa plateforme sans autorisation applicable. Les quatre services musicaux sont des **raccourcis d’écoute indépendants** : Apéro Royale n’inspecte pas leurs titres et ne pilote pas leur lecture. Le défi sonore reste jouable hors ligne avec six motifs originaux. La [spécification de refonte des dix jeux](docs/REFONTE_DIX_MINI_JEUX.md) détaille les prochains prototypes collectifs, encore à réaliser.

![Réglages de la musique](docs/screenshots/settings-music.png)

![Choix de la plateforme musicale](docs/screenshots/settings-sources.png)

![Source musicale externe](docs/screenshots/settings-provider.png)

## Pour développer

Android natif Java, JDK 17, Android SDK 36 et Gradle 8.14.3. Aucun serveur n’est requis pour le jeu local.

```sh
./gradlew test assembleDebug
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turns.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turbo.py
```

Pour créer une mise à jour signée, utilisez votre propre clé PKCS12 et placez ses paramètres dans `signing.properties` à la racine ; ce fichier reste hors Git :

```properties
storeFile=/absolute/path/release.p12
storePassword=your-password
keyAlias=apero
keyPassword=your-password
```

`./gradlew assembleRelease` produit `app/build/outputs/apk/release/app-release.apk`. Conservez la même clé pour permettre les mises à jour Android.

`GameEngine` arbitre les tours, votes, pronostics, règles et scores. `GameStore` conserve session, historique, statistiques par joueur et par jeu dans SQLite. `ArcadeView` et `GameSprites` dessinent les écrans et scènes. `PartyNetwork`, `BluetoothPartyNetwork` et `InternetPartyNetwork` transportent les commandes et instantanés. `ArcadeAudio` produit musique et effets ; `MusicLinks` valide les liens des services musicaux externes.

Illustrations originales dans [`art/source`](art/source), décors des mini-jeux sous [`app/src/main/res/drawable-nodpi`](app/src/main/res/drawable-nodpi) et captures réelles d’émulateur sous [`docs/screenshots`](docs/screenshots). Licence [MIT](LICENSE). Pas de compte Apéro Royale, de publicité ni de télémétrie. [Notes sur les données](PRIVACY.md) et [licences tierces](THIRD_PARTY_NOTICES.md).

**Pour guider la prochaine version :** [diagnostic et refonte détaillée des dix mini-jeux](docs/REFONTE_DIX_MINI_JEUX.md), puis [audit produit initial](docs/AUDIT_PRODUIT_2026.md). Ces documents distinguent les mécaniques déjà livrées des hypothèses à tester avec de vrais groupes.

---

**English:** Apéro Royale 1.4.1 is a French/English Android party arcade for 2–6 friends. Play on one phone or join a room over Wi-Fi, paired Bluetooth or an Internet code. Every round hands control to the next player; private prediction and jury clocks pause until the next person is ready. Local rounds resume after reopening or backgrounding the app. Choose group Vote, Free Play or Turbo rounds. Friends back or challenge the actor, judge social games and guess drawings on their own phone or through a private pass screen. Ten games have distinct scenes, pixel sprites and guides. Eleven hidden achievements can reveal a room-wide rule. The mature new visual direction adds an illustrated lounge home and a redesigned music menu. Choose original music, silence or a shortcut to Spotify, Deezer, Apple Music or Amazon Music. External playback stays in the chosen music app; the sound game remains original and offline. See the [ten-game redesign plan](docs/REFONTE_DIX_MINI_JEUX.md). The signed APK is linked above.

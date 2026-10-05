# APÉRO ROYALE 👑

### Dix jeux. Une table. Personne ne reste spectateur.

Un jeu de soirée Android pour **2 à 6 amis**, en français ou en anglais. Un seul téléphone suffit : on se le passe à chaque action privée. Chacun peut aussi jouer depuis son téléphone et voter, piéger, dessiner, deviner ou juger en direct.

[**Télécharger l’APK Android signé · 1.5.0**](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.5.0.apk) · Android 8.0+ · [Notes de version](RELEASE.md) · [Confidentialité](PRIVACY.md)

![Accueil Apéro Royale](docs/screenshots/home.png)

## Une soirée en trois gestes

1. **Posez les noms sur la table.** Ajoutez 2 à 6 profils avec pseudo, langue et avatar original ou photo. Ils sont retrouvés à la prochaine ouverture.
2. **Choisissez le rythme.** **Vote** : chacun choisit le prochain jeu. **Libre** : la table ouvre le catalogue. **Turbo** : le jeu tire des manches surprises plus courtes.
3. **Passez le téléphone ou jouez chacun sur le vôtre.** Avant le défi, chacun a une action. Le joueur actif mise 1 à 3 gorgées virtuelles, relève le défi, puis le rôle change au tour suivant.

La règle de base est simple : **tu perds le défi, tu prends la gorgée que tu as misée**. Les points et le classement ajoutent du piquant. Les compteurs sont virtuels ; eau, sans alcool et défis sans consommation ont toute leur place.

<table>
<tr><td><img src="docs/screenshots/lobby-four.png" alt="Salon à quatre" width="240"></td><td><img src="docs/screenshots/vote.png" alt="Vote du prochain jeu" width="240"></td><td><img src="docs/screenshots/handoff.png" alt="Passage du téléphone" width="240"></td></tr>
<tr><td>La table</td><td>Le choix collectif</td><td>À toi de jouer</td></tr>
</table>

## Dix défis, dix ambiances

Chaque défi a son décor, son sprite et un guide FR/EN dans l’app. Les commandes de la version 1.5.0 sont plus fines ; la zone tactile reste confortable.

| Défi | Ce qui se passe autour de la table |
| --- | --- |
| **Culture G** | Tout le monde répond en secret ; l’acteur tente sa réponse ou suit la tendance. |
| **Positions à la con** | Une pose absurde à défendre devant le jury. |
| **Blind Test** | Un motif musical original à reconnaître ; chacun écoute et vote. |
| **Réflexe néon** | Les amis placent les cibles que l’acteur devra toucher. |
| **Roulette Royale** | Chacun protège un verre ; l’acteur choisit son risque. |
| **Dessin maudit** | Un dessin, puis les devinettes privées des autres. |
| **Mémoire flash** | Les amis composent la chaîne que l’acteur devra rejouer. |
| **Rythme ou rien** | La salle construit la mesure ; l’acteur tape les temps justes. |
| **Bluff royal** | Histoire vraie ou inventée ? Le jury tranche. |
| **Bombe à bulles** | Une bombe passe de main en main avant le choix du fil. |

<table>
<tr><td><img src="docs/screenshots/games/trivia.png" alt="Culture G" width="180"></td><td><img src="docs/screenshots/games/poses.png" alt="Positions" width="180"></td><td><img src="docs/screenshots/games/blind-test.png" alt="Blind Test" width="180"></td><td><img src="docs/screenshots/games/reflex.png" alt="Réflexe" width="180"></td><td><img src="docs/screenshots/games/roulette.png" alt="Roulette" width="180"></td></tr>
<tr><td>Culture G</td><td>Positions</td><td>Blind Test</td><td>Réflexe</td><td>Roulette</td></tr>
<tr><td><img src="docs/screenshots/games/drawing.png" alt="Dessin" width="180"></td><td><img src="docs/screenshots/games/memory.png" alt="Mémoire" width="180"></td><td><img src="docs/screenshots/games/rhythm.png" alt="Rythme" width="180"></td><td><img src="docs/screenshots/games/bluff.png" alt="Bluff" width="180"></td><td><img src="docs/screenshots/games/bomb.png" alt="Bombe" width="180"></td></tr>
<tr><td>Dessin</td><td>Mémoire</td><td>Rythme</td><td>Bluff</td><td>Bombe</td></tr>
</table>

## Un téléphone ou plusieurs

| Vous êtes… | Mise en place |
| --- | --- |
| **Autour d’un seul téléphone** | Créez les profils, puis passez l’appareil quand « C’est moi » apparaît. Les choix privés restent cachés jusqu’à la révélation. |
| **Sur le même Wi-Fi** | L’hôte ouvre une salle Wi-Fi. Les amis rejoignent avec son adresse et le PIN à six chiffres. |
| **À proximité en Bluetooth** | Appairez les téléphones Android, puis hébergez ou rejoignez la salle avec le PIN. |
| **Connectés par Internet** | L’hôte partage un code de salle à douze caractères. Le relais MQTT TLS par défaut est un service public de test ; sa disponibilité n’est pas garantie. |

L’hôte conserve la partie et le classement. Les invités votent et agissent depuis leur écran ; l’interface suit leur langue. Une partie peut être reprise après fermeture. Le menu de pause permet de revenir au salon, de reprendre, ou d’annuler le défi courant sans points ni gorgée. Un ami absent peut passer son action privée pour que la soirée continue.

### Un ami débarque en pleine partie

Il rejoint la salle avec le même code et son pseudo, même si une manche a commencé. Son téléphone montre le défi en cours et lui ouvre une petite **arcade secrète** : chat pixel à attraper, code de pattes, puis miroir. À la fin de la manche, chaque joueur déjà présent vote **oui** ou **non** pour lui faire une place au tour suivant. La majorité des votes exprimés l’accueille ; une égalité ou aucun vote le refuse. Les votes individuels restent cachés, et le scrutin avance après douze secondes sans réponse.

En cas de refus, le nouvel arrivant prend **une gorgée virtuelle inscrite au classement** et peut demander un nouveau vote au tour suivant. Ses secrets restent jouables pendant l’attente. Une seule demande d’entrée peut attendre à la fois et la table garde sa limite de six joueurs. Pour une soirée sans alcool, la gorgée peut naturellement être remplacée par un défi ou une boisson sans alcool.

<table>
<tr><td><img src="docs/screenshots/late-join-spectator-en.png" alt="Invité en tribune et arcade secrète" width="230"></td><td><img src="docs/screenshots/late-join-vote.png" alt="Vote pour accueillir un ami" width="230"></td><td><img src="docs/screenshots/late-join-rejected-en.png" alt="Refus, gorgée virtuelle et nouvelle tentative" width="230"></td></tr>
<tr><td>Regarder et jouer</td><td>Décider ensemble</td><td>Retenter sa chance</td></tr>
</table>

## Une radio qui vous laisse choisir

Bande originale discrète, style Chill ou Arcade, volume, effets et vibrations se règlent séparément. Vous pouvez aussi choisir **Spotify, Deezer, Apple Music ou Amazon Music**, mémoriser un lien de playlist et ouvrir votre application musicale depuis la radio ♫. Le jeu ne pilote pas ces services et n’utilise pas leurs catalogues pour le Blind Test : celui-ci joue des motifs originaux, même hors ligne.

<table>
<tr><td><img src="docs/screenshots/settings-music.png" alt="Réglages audio" width="230"></td><td><img src="docs/screenshots/settings-sources.png" alt="Sources musicales" width="230"></td><td><img src="docs/screenshots/radio-dock.png" alt="Radio Apéro" width="230"></td></tr>
</table>

## Des secrets à découvrir

Sur plusieurs appareils, les écrans d’attente cachent de petits jeux. Une découverte faite par un joueur déjà dans la partie peut donner le droit de proposer une règle valable pour toute la salle au tour suivant. L’arcade de l’invité en attente reste sur son téléphone et n’influence pas le vote d’entrée. Si une règle sociale est contestée, la salle vote. Aucun microphone ne surveille les conversations.

<details><summary>Voir un secret en image</summary>

![Un jeu secret](docs/screenshots/secret-paw-code.png)

</details>

## Confiance et fabrication

Apéro Royale fonctionne sans compte, publicité ni télémétrie. Les profils, parties et classements sont stockés localement sur le téléphone hôte. Les photos facultatives sont réduites avant partage. [Données et réseau](PRIVACY.md) · [Licence MIT](LICENSE) · [Licences tierces](THIRD_PARTY_NOTICES.md).

Projet Android natif Java, JDK 17, SDK 36, Gradle 8.14.3. Les sources principales sont dans app/src/main/java/com/aperoroyale. GameEngine gère les tours, GameStore conserve la partie dans SQLite, ArcadeView et GameSprites dessinent les scènes, ArcadeAudio joue la musique et les effets. La clé de signature n’est pas dans le dépôt.

~~~sh
./gradlew testDebugUnitTest assembleDebug
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
~~~

[Refonte visuelle et stress tests 1.5.0](docs/UI_150_ET_STRESS.md) · [Analyse des dix jeux](docs/REFONTE_DIX_MINI_JEUX.md) · [Modèle de joueurs virtuels](docs/LABO_GENS_VIRTUELS_2026.md) · [Historique des versions](RELEASE.md). Les simulations vérifient des règles et des délais supposés ; elles ne prouvent pas l’amusement ou la latence sur de vrais téléphones.

---

**English:** Apéro Royale 1.5.0 is a French/English Android party arcade for 2–6 friends, on one phone or several. Everyone acts in all ten challenges. A late friend can spectate, play secret games, and face an admission vote at the next round break; rejection adds one virtual sip and allows a retry. Profiles and the leaderboard persist. The signed APK is linked at the top; music services open in their own apps, while the Blind Test uses original offline motifs.

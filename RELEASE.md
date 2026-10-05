# Apéro Royale 1.4.9 — Les joueurs virtuels

- [Télécharger l'APK Android signé 1.4.9](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.4.9.apk) · `com.aperoroyale` · versionCode `16` · Android 8.0+ (API 26)
- SHA-256 de l'APK : `80ea3f06465d0c6399f765f3bc73292aae77e8758af055c21ed5cc58661bb031`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.8

## Nouveautés

- [Laboratoire de joueurs virtuels](docs/LABO_GENS_VIRTUELS_2026.md) : agents avec goûts, aptitudes, relations, confiance, humeur, fatigue et mémoire des défis. Ils votent, misent, contribuent, jugent et peuvent manquer une action ; les effets d'une manche modifient leurs choix ultérieurs. Trois graines couvrent 90 000 soirées de 24 manches par variante, soit 4,32 millions de manches pour la comparaison avant/après.
- Blind Test : 23 secondes au lieu de 18 ; Mémoire : 26 au lieu de 22, avant les modificateurs existants. Dans **ce modèle**, les fins forcées passent de 30,0–30,5 % à 14,4–14,6 % pour Blind Test et de 20,9–21,3 % à 12,1–12,4 % pour Mémoire. Les deux réglages restent à essayer avec de vrais amis.
- Données de cohortes avant/après, hypothèses et exporteur reproductible publiés. Les agents ne mesurent ni le plaisir, ni les conversations, ni le Wi-Fi/Bluetooth/Internet réel.

## Vérifications

- `testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite ; 39 tests, zéro échec. Six longues exécutions de 30 000 soirées ont passé sans état bloqué.
- APK signée v2 vérifiée ; `aapt` confirme versionCode 16 et minSdk 26.
- APK installée et lancée sur émulateurs Android 8.0 et Android 16 ; l'activité principale reste au premier plan. Les parcours UI détaillés de la version précédente ne sont pas rejoués ici, car les changements visibles portent sur deux chronos.

**Limites.** Le modèle est une expérience de conception, sans calibration sur des personnes réelles. Ses pourcentages ne sont pas des prédictions de réussite, d'amusement ou de fidélisation.

---

# Apéro Royale 1.4.8 — L'horloge de soirée

- [Télécharger l'APK Android signé 1.4.8](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.4.8.apk) · `com.aperoroyale` · versionCode `15` · Android 8.0+ (API 26)
- SHA-256 de l'APK : `454ac14fb6b71d7d4848e0e7f4204273083cb54fb9a557ae5848b4b9ec0d30f1`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.7

## Nouveautés

- [Laboratoire extrême à horloge virtuelle](docs/LABO_EXTREME_2026.md) : trois graines, 100 000 soirées de 18 manches chacune, soit 5,4 millions de manches. Les vrais délais et transitions du moteur sont exercés avec des profils persistants, des interruptions et des réponses manquantes corrélées, sur un, deux ou plusieurs téléphones modélisés. Les [180 cohortes](docs/data/extreme-100k-cohorts.csv), les [dix jeux](docs/data/extreme-100k-games.csv) et les hypothèses sont publiés. Ce ne sont pas des mesures de plaisir ou de réseau réel.
- Un joueur absent ne bloque plus l'écran privé d'un téléphone partagé. [« Absent ? Passer son tour »](docs/screenshots/handoff-skip.png) enregistre une abstention sans inventer sa réponse ni ses points ; le défi continue avec ceux qui participent.
- La fenêtre de contribution de groupe repart après chaque ami : huit secondes en Turbo, douze dans les autres modes. Le modèle signale nettement moins de contributions coupées, avec une hausse assumée de la durée de manche.
- Positions, Dessin et Bluff donnent 40 secondes à la création avant le verdict ou la phase suivante. Le simulateur tient compte du chrono suspendu lors du passage physique de la bombe.

## Vérifications

- `testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite, dont tests de l'horloge virtuelle, abstentions, score, reprise et dix défis. Trois exécutions longues de 100 000 soirées ont passé sans erreur d'état.
- Android 16 sur émulateur : vote et contribution sautés par le nouveau bouton, vote absent résolu par délai, profils et reprise, pause, annulation, radio. Scripts : `tools/smoke_skip_handoff.py`, `tools/smoke_vote_timeout.py`, `tools/smoke_release_146.py`.
- APK signée v2 vérifiée ; `aapt` confirme versionCode 15 et minSdk 26. Mise à jour installée et lancée sur émulateur Android 8.0.

**Limites.** Les comportements, durées et délais réseau du laboratoire sont des hypothèses. Aucun groupe humain ni plusieurs téléphones physiques connectés n'ont servi à calibrer ce modèle. Les taux de réussite et d'échéance simulés ne prédisent donc ni l'amusement ni la fidélisation.

---

# Apéro Royale 1.4.7 — Le laboratoire de soirée

- [Télécharger l'APK Android signé 1.4.7](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.4.7.apk) · `com.aperoroyale` · versionCode `14` · Android 8.0+ (API 26)
- SHA-256 de l'APK : `327f774ca63ba399821f6e6c795ac875cbffebac06de5fcb8db9bae71a395408`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.6

## Nouveautés

- [Laboratoire de soirées synthétiques](docs/LABO_EXPERIENCE_2026.md) : 3 000 groupes et 36 000 manches, 2–6 joueurs FR/EN, un ou plusieurs téléphones, trois modes, profils rapides ou hésitants, passages de téléphone, pauses et trois scénarios de délai réseau. Les résultats séparent la préparation, l'attente sans saisie et les six jeux où les amis préparent le défi puis regardent l'acteur. Les hypothèses et la méthode sont publiées ; **aucun score de plaisir ou de viralité n'est revendiqué**.
- Le mode Vote ne reste plus figé si une personne ne répond pas. Après 18 secondes pour le premier vote, puis 12 secondes après chaque voix, les joueurs absents s'abstiennent ; seules les voix réellement reçues départagent les jeux. Le compteur apparaît à l'écran. Le passage du téléphone et les pauses locales suspendent le délai.
- Le salon indique désormais que Turbo accélère les manches sur un téléphone partagé à partir de quatre joueurs, tout en laissant le choix du mode au groupe. [Capture du salon à quatre](docs/screenshots/lobby-four.png).
- Le choix d'une règle secrète sans réponse reçoit son premier choix proposé après 15 secondes en Turbo ; en Vote, le même filet de sécurité s'applique à l'expiration du scrutin. L'accroche de l'accueil ne promet plus « zéro temps mort », ce que nos simulations ne démontrent pas.

## Vérifications

- `testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. Le nouveau laboratoire couvre 30 cohortes et les dix mini-jeux ; le stress test ajoute 15 000 scrutins avec absences hypothétiques. Des tests unitaires vérifient abstention, restauration et règle secrète sans réponse.
- Émulateur Android 16 : parcours de la version 1.4.6 repassé avec deux profils, pause de plus de 12 secondes, annulation, retour au salon, redémarrage et radio. Nouveau parcours : un vote absent, puis deux votes absents, débloquent le tour après les délais annoncés.
- APK signée v2 vérifiée ; `aapt` confirme versionCode 14 et minSdk 26. Installation en mise à jour de 1.4.6 sur émulateur Android 8.0, lancement et ouverture de l'accueil conservant le portrait local.

**Limites.** Les durées du laboratoire sont des hypothèses et les délais réseau ne proviennent pas de sockets réels. La participation sociale, l'envie de rejouer, la qualité audio et la latence sur téléphones physiques restent à étudier. Le mode Vote à six sur un téléphone demeure lent dans tous nos scénarios ; cette release rend ses blocages finis, elle ne refond pas encore le déroulement des dix jeux.

---

# Apéro Royale 1.4.6 — La table se retrouve

- [Télécharger l'APK Android signé 1.4.6](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.4.6.apk) · `com.aperoroyale` · versionCode `13` · Android 8.0+ (API 26)
- SHA-256 de l'APK : `6949aed727751ae7bc24558fbfe3ce29220da775f9251c3e3b12a29cde3fdf7b`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.5

## Nouveautés

- Les profils locaux persistent entre les soirées : pseudo, langue, sprite et photo. L'accueil reconnaît l'équipe, un profil peut être corrigé ou retiré, et le salon peut être vidé sans supprimer le classement. Le renommage transporte les statistiques et l'historique vers le nouveau pseudo.
- ☰ ouvre un vrai menu pendant une partie sur un téléphone : pause, annulation du défi courant sans résultat, retour au salon ou accueil avec reprise ultérieure. Le chrono reste suspendu pendant le choix ; le résultat d'une manche déjà terminée est conservé.
- Accueil recomposé autour d'une scène panoramique, d'une action principale, d'une carte de l'équipe et de quatre accès compacts. Les écrans de choix musical deviennent une grille lisible.
- La Radio Apéro dispose d'un panneau sombre dédié : sélection directe d'une plateforme et ouverture de sa playlist, bande originale ou silence, commandes Android play/pause/suivant pour le lecteur actif. Aucun compte de streaming n'est lu par le jeu.
- [Décisions de conception, références Play Store et limites](docs/RELEASE_146_DESIGN.md) ; captures de [l'accueil](docs/screenshots/home.png), de [la radio](docs/screenshots/radio-dock.png) et des [sources](docs/screenshots/settings-sources.png).

## Vérifications

- `testDebugUnitTest assembleRelease lintVitalRelease --offline` : réussite, y compris les tests de règles et simulations existants.
- Parcours sur émulateur Android 16 : deux profils retrouvés après nouvelle soirée et fermeture ; renommage avec score et historique préservés ; pause de 13 secondes pendant une préparation de 12 secondes ; annulation sans ligne d'historique ; retour au salon ; changement de groupe ; sélection musicale persistée. Script : `tools/smoke_release_146.py`.
- Mise à niveau de l'APK signée 1.4.5 vers 1.4.6 sur émulateur Android 8.0 : installation, lancement, création et réutilisation d'un profil après redémarrage. `apksigner` valide la signature ; `aapt` confirme versionCode 13 et minSdk 26.

**Limites vérifiables.** Aucun compte Spotify, Deezer, Apple Music ou Amazon Music réel, aucun lecteur externe actif et aucun groupe de téléphones physiques n'ont été testés pour cette release. Les commandes Android peuvent être ignorées si aucune session média n'est active ; les plateformes jouent dans leur propre application. Les simulations de manche ne mesurent ni plaisir ni viralité.

---

# Apéro Royale 1.4.5 — Les secrets de la ruelle

- [APK Android signé 1.4.5](https://raw.githubusercontent.com/nabz0r/AperoRoyal/main/releases/AperoRoyale-v1.4.5.apk) · `com.aperoroyale` · versionCode `12` · Android 8.0+ (API 26)
- SHA-256 : `c871f4d36b3f41546b89e21ca2618f59e95844c5852d253efb387196dac55ade`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.4

## Changements

- Les invités découvrent trois micro-jeux secrets dans leurs écrans d’attente : poursuite de chat, code de pattes et chat miroir. Un indice discret remplace l’ancien chat de menu à 20 tapotements. Les secrets ne figurent pas dans le catalogue des dix défis.
- La progression privée suit le joueur pendant la manche, le résultat et le choix suivant. Une seule découverte par tour évite le spam. L’hôte valide les gestes ; les autres appareils ne reçoivent ni la graine ni la progression de ce joueur.
- Le premier joueur qui trouve un type de secret peut choisir ou renouveler une règle de salle au **tour suivant**. La règle de la manche en cours continue donc à arbitrer ses points et gorgées. Le trophée déclenche un écran et un effet sonore sur le téléphone gagnant.
- Nouvel artwork pixel nocturne dédié aux secrets, boutons larges et décor de ruelle. Le lobby, le catalogue, le résultat et l’attente suivent mieux la langue personnelle du joueur invité ; les noms longs du catalogue ne chevauchent plus les sprites.
- [Audit de 10 000 soirées multi-appareils](docs/SIMULATION_10000_MULTI.md) et nouvelles captures d’émulateur dans le README.

## Vérifications

- `testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. La simulation ajoute 10 000 salles, 40 000 manches, 160 000 états privés FR/EN, 35 945 secrets terminés et 400 sauvegardes restaurées. Les tests existants de 500 soirées de robustesse et 500 soirées de rythme continuent de passer.
- Émulateur Android 16 : dix mini-jeux terminés en mode Libre avec deux profils sur le même téléphone, alternance des tours, actions des amis, paris et historique SQLite.
- Deux émulateurs Android 16 et Android 8 en Wi‑Fi local simulé : salon FR/EN rejoint, secret découvert sur le téléphone EN, code joué après le résultat, trophée reçu, règle choisie sur l’invité et synchronisée sur l’hôte. [Captures du secret](docs/screenshots/secret-paw-code.png) et [du choix de règle](docs/screenshots/secret-room-rule-en.png).
- APK signée vérifiée en signature v2 ; `aapt` confirme versionCode 12 et minSdk 26. Installation en mise à jour depuis 1.4.4 sur Android 8 vérifiée avant publication.

**Limites.** Les 10 000 salles sont simulées dans le moteur, pas sur 10 000 connexions réelles. Aucune mesure de latence Bluetooth/Internet ni test de plaisir sur de vrais groupes n’est revendiqué. Le salon Internet par défaut dépend d’un relais public de test.

---

# Apéro Royale 1.4.4 — Toute la salle joue

- [APK Android signé 1.4.4](releases/AperoRoyale-v1.4.4.apk) · `com.aperoroyale` · versionCode `11` · Android 8.0+ (API 26)
- SHA-256 : `572e250370086f510add6809256a4393b057d37f41fe300e452b0d33153d5029`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.3

## Changements

- Les six jeux qui laissaient les amis spectateurs ont chacun une action propre : réponses et tendance de salle pour Culture G et Blind Test, placement des premières cibles pour Réflexe, protection des gobelets pour Roulette, construction de la chaîne Mémoire, vote des temps pour Rythme. Les choix se font en privé sur un téléphone partagé, ou sur chaque appareil invité.
- Tous les non-artistes devinent désormais le dessin ; l'artiste gagne si au moins la moitié des réponses reçues sont correctes. Les passages locaux suspendent le chrono et sa durée croît avec la taille de la galerie.
- Les contributions des amis rapportent des points et figurent dans l'historique et le classement. L'hôte masque les réponses avant la révélation, arbitre les commandes réseau et diffuse les résultats. Dans une salle à plusieurs appareils, la musique d'ambiance joue sur l'hôte et les invités conservent leurs effets locaux, pour éviter des boucles décalées.
- Guides FR/EN, captures des dix jeux et documentation du rythme actualisés. Les scènes et sprites dédiés de chaque jeu restent en place ; les nouveaux écrans collectifs utilisent leur décor propre.

## Vérifications

- `testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. 500 soirées de robustesse, 7 990 manches et 40 041 restaurations ; 500 autres soirées modélisent la participation et le rythme, avec **0 manche solo** quand chacun répond. Le modèle estime 77,3 s par manche en Vote à six sur un téléphone, 49,4 s en Turbo et 35,5 s en Vote sur appareils séparés.
- Émulateur Android 16 : dix jeux terminés avec deux profils locaux, alternance des tours, actions collectives, mises et historique ; reprise, arrière-plan, délais de dessin, jury et Turbo vérifiés. Une manche de dessin à trois joueurs a enregistré la réponse de chaque non-artiste avant le verdict.
- Deux émulateurs Android 16 et Android 8.0 via Wi-Fi local simulé : invité anglophone dessinant sur son appareil, hôte devinant, résultat reçu sur les deux ; choix de symbole Mémoire envoyé par l'invité et retrouvé en tête de la séquence arbitrée par l'hôte.
- APK signée vérifiée par `apksigner` avec certificat identique à 1.4.3 ; `aapt` confirme versionCode 11 / minSdk 26. Mise à niveau de l'APK signée 1.4.3 vers 1.4.4 installée et lancée sur Android 8.0.

**Limites mesurées.** Une simulation ne démontre ni plaisir ni viralité. À six sur un téléphone, le vote à chaque manche reste lent dans le modèle. Les services Bluetooth et Internet sont implémentés mais cette release n'inclut pas de mesure de latence de bout en bout sur téléphones physiques.

---

# Apéro Royale 1.4.3 — Le relais prend vie

- [APK Android signé 1.4.3](releases/AperoRoyale-v1.4.3.apk) · `com.aperoroyale` · versionCode `10` · Android 8.0+ (API 26)
- SHA-256 : `b2452689c4eeef6b2b8c9355aad35b729920aa3a78ad388921120a727d6ceae9`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.2

## Changements

- Après deux défis où toute la salle n'a pas agi, Vote et Turbo proposent une manche de Poses, Bluff ou Bombe. Ces trois défis démarrent directement après la mise : le jury ou le relais remplace le prono générique.
- La bombe impose le passage par chaque ami, laisse le porteur choisir le suivant et varie de une à trois touches par porteur. Une fois tout le monde passé, le porteur peut couper un fil rouge ou bleu : réussite anticipée et +100 points pour l'acteur, ou défaite. Le passage d'un téléphone à l'autre suspend son chrono. Les commandes Wi-Fi de passage et de coupe sont traitées par l'hôte.
- Réflexe varie le nombre et la taille des cibles ; Mémoire propose six formes de séquence ; Rythme six phrases de quatre frappes ; Roulette six dispositions de pièges révélées après le choix. Les six motifs sonores originaux du Blind Test sont joués dans quatre tonalités, soit 24 variantes de tirage.
- [L'audit de rythme actualisé](docs/SIMULATION_EXPERIENCE_500.md) compare l'état 1.4.2 et 1.4.3. À six sur un téléphone en Vote, la moyenne **modélisée** passe de 87,3 à 81,0 s ; la plus longue série sans action propre au défi passe de cinq à deux manches. Les six jeux principalement solo restent à revoir.

## Vérifications

- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. Le test de robustesse traverse 500 soirées, 7 990 manches et 39 726 restaurations d'état ; l'audit de rythme traverse 500 soirées et 7 990 manches supplémentaires avec hypothèses de temps explicites. Des tests ciblés couvrent les variantes, l'ordre du relais, les deux issues du fil et leur sauvegarde.
- Émulateur Android 16 : les dix mini-jeux terminés à deux profils locaux, chacun son tour ; passages privés, Turbo et règle secrète vérifiés.
- Deux émulateurs reliés en Wi-Fi : l'invité reçoit la bombe sur son appareil, agit, coupe un fil et l'hôte synchronise le résultat.
- APK signée vérifiée par `apksigner` en signature v2 ; `aapt` confirme versionCode 10 et minSdk 26. Mise à niveau de l'APK signée 1.4.2 vers 1.4.3 installée et lancée sur émulateur Android 8.0.

**Limites.** Aucune simulation ne prouve qu'un jeu est viral. Les durées ne sont pas mesurées sur des humains ; à six sur un téléphone, Vote reste long, et six jeux gardent un cœur solo. Pas de mesure de latence Bluetooth/Internet ni de session sur vrais téléphones pour cette version.

---

# Apéro Royale 1.4.2 — Le rythme sous la loupe

- [APK Android signé 1.4.2](releases/AperoRoyale-v1.4.2.apk) · `com.aperoroyale` · versionCode `9` · Android 8.0+ (API 26)
- SHA-256 : `9bc4da3199785da590d1d8ddb49aa5b91227bf77d77cef3f262c156eb61ba02a`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.1

## Changements

- [Audit de 500 soirées supplémentaires](docs/SIMULATION_EXPERIENCE_500.md) : 7 990 manches avec temps modélisé, part de préparation, répétition des jeux et cartes, et participation propre à chaque défi. Le scénario à six sur un téléphone en mode Vote ressort à 87,3 s modélisées par manche ; six défis sur dix restent essentiellement solo. Ces constats sont des risques de conception, non une validation du plaisir.
- QCM, poses, motifs sonores, dessins et amorces de bluff passent dans des paquets mélangés persistants : aucune carte ne revient avant épuisement du paquet, y compris après reprise de la partie.
- Roulette : 10 s pour choisir un gobelet, puis révélation complète. Dessin : 30 s pour dessiner, 12 s pour deviner ; le relais sur téléphone partagé suspend le délai du devineur.

## Vérifications

- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. Les deux simulations totalisent 1 000 soirées et 15 980 manches ; la nouvelle simulation sert au diagnostic de rythme avec hypothèses publiées.
- Parcours sur émulateur Android 16 : les dix mini-jeux terminés à deux profils locaux avec passage de téléphone, mises, pronostics et historique.
- Test de passage privé sur Android 16 : pronostic Turbo et jury suspendus pendant le relais, reprise après fermeture et arrière-plan. Le script a été adapté à l'emplacement actuel du bouton « Reprendre ».
- Test de rythme sur Android 16 : après plus de 12 s de passage privé, le devineur garde son nouveau chrono ; la roulette sans choix expire avec une défaite enregistrée.
- APK release installée et lancée sur émulateur Android 8.0. `apksigner verify` valide la signature v2 ; `aapt` confirme versionCode 9 / minSdk 26.

**Limites.** Les durées du rapport sont des estimations, pas des observations humaines ou des mesures de latence Bluetooth/Wi-Fi/Internet. Le plaisir entre amis, l'attente ressentie et les mécaniques collectives à refaire nécessitent des tests avec de vrais groupes. Les dix refontes de gameplay décrites dans la documentation ne sont pas livrées dans 1.4.2.

---

# Apéro Royale 1.4.1 — Une soirée qui continue

- [APK Android signé 1.4.1](releases/AperoRoyale-v1.4.1.apk) · `com.aperoroyale` · versionCode `8` · Android 8.0+ (API 26)
- SHA-256 : `a02808a53e268c7f75635272f07ad3849508e8d08bad878d15aef82437927fc5`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.0

## Correctifs

- En Turbo, une règle secrète débloquée dispose maintenant d'un écran de choix avant le défi suivant, y compris lorsque la partie est reprise ou synchronisée.
- À l'expiration du chrono d'un jury, le moteur applique les votes exprimés pour les défis de pose et de bluff.

## Vérifications

- [Simulation reproductible de 500 soirées](docs/SIMULATION_500_PARTIES.md) : 7 990 manches de deux à six joueurs sur les dix jeux et les trois modes, 40 190 restaurations JSON, 498 choix de règle secrète dont 166 en Turbo, et 436 signalements soumis au vote. Les deux anomalies ont été détectées puis couvertes par des tests ciblés.
- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. `apksigner verify` valide la signature v2 et `aapt` confirme versionCode 8 / minSdk 26.
- Émulateur Android 16 : les dix jeux terminés à deux profils avec passage de tour, mises, pronostics et historique ; deux défis Turbo terminés ; secret Turbo débloqué puis règle choisie à l'écran après une manche.
- Émulateur Android 8.0 : APK signé installé, activité lancée et processus encore actif après affichage de l'accueil.

**Limites.** La simulation porte sur le moteur et ses instantanés ; elle ne représente pas 500 soirées humaines, connexions réseau réelles, interactions audio ou sessions sur téléphones physiques. Les limites de l'intégration musicale et du relais Internet de 1.4.0 restent applicables.

---

# Apéro Royale 1.4.0 — La nuit vous appartient

- [APK Android signé 1.4.0](releases/AperoRoyale-v1.4.0.apk) · `com.aperoroyale` · versionCode `7` · Android 8.0+ (API 26)
- SHA-256 : `3525d3465b5828ca91d6d0ca4f8915d79de0b511bcd806ac77c0aaa898cc1f20`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.3.0

## Changements

- **Nouvelle direction visuelle.** Accueil illustré comme un salon d'arcade nocturne, palette charbon/laiton, composants moins arrondis et icône redessinée. Les dix scènes de jeu conservent leurs mécaniques et adoptent la nouvelle palette ; leurs captures ont été régénérées.
- **Musique à votre goût.** Le menu de réglages permet de choisir bande originale, Spotify, Deezer, Apple Music, Amazon Music ou silence. Un lien HTTPS de playlist peut être mémorisé pour chaque plateforme ; le bouton d'ouverture et le raccourci ♫ en jeu lancent l'application musicale ou le navigateur. Musique du jeu, effets et vibrations sont indépendants. Le volume initial de la bande originale passe à 18 % sur une nouvelle installation.
- **Blind Test autonome.** L'ancien accès OAuth et les appels Web API Spotify ont été retirés du jeu. Le mini-jeu reste fondé sur ses motifs sonores originaux et jouable hors ligne. La [politique développeur Spotify](https://developer.spotify.com/policy) interdit de construire un jeu ou quiz avec sa plateforme sans autorisation applicable.
- **Plan de la suite.** [Audit et refonte des dix jeux](docs/REFONTE_DIX_MINI_JEUX.md) : rôle de chaque joueur, latence, contenu, rejouabilité, moments à partager et critères de test. Ces refontes collectives sont des spécifications, pas des mécaniques déjà livrées.

## Vérifications de 1.4.0

- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite, dont tests des liens HTTPS et fournisseurs.
- Émulateur Android 16 : écran d'accueil, réglages, choix de Deezer, lien externe ouvert dans Chrome. Le parcours automatisé a terminé les dix mini-jeux avec deux profils, alternance des acteurs, mises, pronostics, historique et dix captures actualisées.
- Émulateur Android 8.0 : APK signé installé et activité lancée. `apksigner verify` valide la signature v2 ; `aapt` confirme versionCode 7 et minSdk 26.

**Limites de validation.** Aucune session sur de vrais téléphones avec comptes Spotify, Deezer, Apple Music ou Amazon Music, ni test complet Bluetooth ou Internet sur cette version. Le bouton musical ouvre un service externe : Apéro Royale n'en pilote pas lecture, pause ou volume. Les dix refontes de gameplay restent à développer et à éprouver auprès de groupes réels. Le relais Internet public, l'identité réseau et le classement historique par pseudo gardent les limites décrites ci-dessous.

---

# Apéro Royale 1.3.0 — La salle joue avec toi

- [APK Android signé 1.3.0](releases/AperoRoyale-v1.3.0.apk) · `com.aperoroyale` · versionCode `6` · Android 8.0+ (API 26)
- SHA-256 : `690edb2c48925e8d8034ae14ba035510e7521aaf525f738a7f4a28d26cca79a6`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.2.1

## Changements

- **Pronostics qui agissent sur la manche.** Couvrir ajoute jusqu'à six secondes aux jeux chronométrés compatibles et retire une gorgée virtuelle sur une défaite. Défier augmente le gain du joueur actif s'il l'emporte et rapporte davantage à un pronostiqueur qui avait annoncé sa défaite. Le passage privé reste actif sur un seul téléphone.
- **Bluff et bombe plus collectifs.** Le conteur choisit secrètement si son anecdote est vraie ou inventée ; le jury vote et peut être trompé. La bombe demande deux actions par joueur avant chaque passage.
- **Rythme vérifié par l'hôte.** Les touches sont évaluées sur la même période de 600 ms que l'affichage, avec une marge réseau bornée. La séquence ne peut plus être validée avec quatre touches simplement croissantes.
- **Secrets mieux isolés.** Les instantanés invités masquent réponses, gobelet perdant, vérité du bluff et détail des votes avant révélation. Les actions restent arbitrées par l'hôte.
- **Son et visuel.** Six motifs auditifs distincts remplacent les mélodies anonymes du Blind Test hors ligne ; la musique de fond a quatre phrases et des variations par jeu. La Radio Apéro prend la place de la musique synthétique lorsqu'elle joue. Les sprites réagissent davantage dans les dix scènes, dont les captures sont actualisées.
- **Navigation.** Les actions principales du Canvas exposent désormais des boutons et cibles nommés à l'accessibilité Android ; les barres système se recachent après les boîtes de dialogue.
- **Contenu.** Une carte ne peut plus revenir immédiatement dans le même mini-jeu pendant la partie.

## Vérifications de 1.3.0

- `./gradlew testDebugUnitTest assembleRelease lintVitalRelease --offline` : réussite. Les tests unitaires couvrent l'horloge du rythme, le soutien/défi, la vérité du bluff, la bombe et le non-retour immédiat d'une carte.
- `tools/smoke_v120.py` sur émulateur Android 16 : les dix jeux sont terminés avec deux profils, alternance des tours, mises, pronostics et historique ; dix captures de jeu régénérées.
- Deux émulateurs Android 16 et Android 8.0 reliés en Wi-Fi local simulé : vote, passage, mise, pronostic, dessin depuis le téléphone invité, réponse sur l'hôte et résultat synchronisé.
- L'arborescence Android de la version signée expose les boutons nommés de l'accueil. L'APK 1.3.0 signé s'installe et démarre sur l'émulateur Android 8.0 ; `aapt` confirme versionCode 6 / minSdk 26 et `apksigner verify` valide la signature.

**Limites de validation.** Aucun essai de 1.3.0 n'a été réalisé sur des téléphones physiques appairés en Bluetooth, via le relais Internet public ou avec un compte Spotify réel. Le relais Internet dépend d'un service public de test ; le code partagé n'est pas une identité cryptographique par joueur. Le classement historique reste sur l'hôte et utilise encore le pseudo comme clé. Les commandes accessibles ont été détectées par Android, sans session complète avec TalkBack ni test utilisateur en soirée. Les mesures de latence et l'appréciation du plaisir de jeu restent à faire avec de vrais groupes.

---

# Apéro Royale 1.2.1 — Un tour sans chrono perdu

- [APK Android signé 1.2.1](releases/AperoRoyale-v1.2.1.apk) · `com.aperoroyale` · versionCode `5` · Android 8.0+ (API 26)
- SHA-256 : `6061e22e774cd8868833726b529acc766fc485b6d58ab1ffd033337962624c96`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.2.0

## Changements

- **Passage de téléphone serein.** Le chrono des pronostics, du jury et des votes de règle attend maintenant la confirmation du joueur suivant. L’écran de passage l’indique clairement.
- **Reprise fiable.** Une partie rouverte restaure le passage privé et redonne un chrono utilisable. Les manches locales ne se terminent pas pendant que l’application est en arrière-plan ; les réglages préservent aussi les chronos des pronostics et votes de règle.
- **Formats courts et langues.** L’interface garde au moins 820 unités de hauteur virtuelle et centre son contenu sur les écrans 16:9. Les consignes privées suivent la langue du joueur qui prend le téléphone.
- **Musique plus discrète.** La musique procédurale se tait lorsque l’application est en arrière-plan. Les menus hors défi sont rafraîchis moins souvent pour économiser les ressources.

## Vérifications de 1.2.1

- `./gradlew test assembleDebug assembleRelease lintVitalRelease` : réussite ; Gradle indique `NO-SOURCE` pour les tests unitaires Java, donc les parcours ci-dessous portent la vérification fonctionnelle.
- `tools/smoke_handoff.py` sur émulateur Android 16 : pronostic Turbo toujours ouvert après 6 secondes de passage, jury toujours ouvert après 21 secondes, reprise après fermeture, puis après passage en arrière-plan.
- `tools/smoke_turbo.py` : deux défis consécutifs et alternance des joueurs ; `tools/smoke_v120.py` : dix jeux terminés à deux joueurs avec mises, pronostics et historique.
- Affichage et navigation vérifiés sur émulateur 1080 × 1920 ; format 1080 × 2340 utilisé pour les parcours complets.
- APK release signé installé en mise à jour et lancé sur émulateur Android 8.0 ; `aapt` confirme versionCode 5 / minSdk 26 et `apksigner verify` valide la signature.

Les limites réseau, Bluetooth et Spotify décrites ci-dessous restent applicables ; aucun nouvel essai sur téléphones physiques ou comptes Spotify réels n’a été effectué pour 1.2.1.

---

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

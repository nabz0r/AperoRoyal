# Audit produit et feuille de route — Apéro Royale

**Date :** 4 octobre 2026  
**Base examinée :** `main` à `cf4c9f9`, version Android 1.2.1  
**Méthode :** lecture du code, du README, des captures d'émulateur et des sources primaires ci-dessous. Aucun test avec de vrais joueurs ni mesure réseau sur appareils physiques n'a été réalisé pour cet audit.

## Verdict

Apéro Royale possède déjà une base jouable : 10 défis, profils FR/EN, passage de téléphone, paris, votes, jurys, sauvegarde locale, scores et transport Wi-Fi/Bluetooth/Internet. La priorité n'est pas d'ajouter un onzième défi. Il faut faire de chaque manche **une histoire vécue par tout le groupe** : choix rapide, participation simultanée, révélation drôle, conséquence claire, puis revanche immédiate.

Aujourd'hui, dans la plupart des défis, une personne exécute l'action et les autres pronostiquent. Le mode multijoueur transporte cet état entre appareils, mais ne transforme pas assez le gameplay. Les décors sont distincts ; l'interface de jeu reprend presque toujours la même composition et de petits sprites. Le son est réglable, mais trois motifs de huit secondes se répètent, et le défi de rythme n'est pas calé sur le moteur audio. Ces écarts expliquent mieux le manque d'envie de rejouer qu'un simple manque d'effets.

## Ce qui fonctionne déjà

- Un joueur actif différent à chaque manche, avec écran privé avant son tour ; votes et jurys peuvent aussi passer de main en main sur un seul téléphone.
- Les dix jeux possèdent un décor, une icône animée et un guide en deux langues.
- Mode Turbo, choix libre ou vote collectif ; profils avec avatar exclusif ou photo importée.
- Salle en Wi-Fi, Bluetooth ou Internet ; l'hôte arbitre les commandes et conserve la session et l'historique SQLite.
- Paris de 1 à 3 gorgées virtuelles, pronostics, règles de salle, secrets et chat pixelisé. Le microphone n'est pas nécessaire pour appliquer une règle sociale : le groupe signale et vote.

## Frictions observées dans le code

| Priorité | Constat vérifiable | Effet en soirée | Décision recommandée |
| --- | --- | --- | --- |
| P0 | `GameEngine.finish()` attribue l'enjeu central au joueur actif ; les autres gagnent surtout 35 points ou une pénalité via le prono. | Une partie du groupe attend la fin d'un jeu solo. | Chaque jeu doit offrir à **chaque joueur** une action, un choix ou un rôle qui change le résultat, au-delà du prono. |
| P0 | `GameEngine.rhythmTap()` valide un indice croissant et le délai global, sans contrôler la proximité d'un temps musical. `ArcadeView` utilise une phase visuelle de 600 ms indépendante des pas audio de 250 ms. | La validation est incohérente entre appareils et peut récompenser des frappes hors rythme. | Une horloge de manche unique, fenêtre de frappe calculée par l'hôte, calibration locale ; retirer ce jeu du compétitif en ligne avant correction. |
| P0 | `GameEngine.networkJson()` envoie `target`, `loserCup`, votes, pronostics et séquence à tous les clients. | Un client peut lire la réponse ou des choix cachés avant la révélation. | Instantanés par rôle avec seulement les champs visibles ; révéler l'élément secret au bon moment. |
| P1 | La boucle Vote/Libre → transition → passage → pari → prono → jeu → résultat impose plusieurs écrans successifs. | La tension retombe avant l'action. | Fusionner pari et prono dans une préparation collective courte ; une seule touche pour lancer/rejouer. |
| P1 | Le Blind Test hors ligne demande de nommer six mélodies originales inconnues. | Il teste la mémorisation d'étiquettes inventées, pas la culture musicale du groupe. | Faire deviner un son produit par un joueur, ou une catégorie/émotion/époque ; garder Spotify comme option. |
| P1 | `GameStore` utilise le pseudo comme clé de statistiques et garde le classement sur un seul appareil. | Deux personnes homonymes de soirées différentes fusionnent ; les classements divergent entre téléphones. | Identifiant de profil stable, nom affiché séparé ; classement de salle synchronisé et classement historique explicitement local. |
| P1 | `ArcadeView` dessine tout dans un Canvas virtuel 400×820, sans arborescence d'actions accessible ; certains labels peuvent descendre à 10 unités virtuelles. | Lecture, taille de texte, TalkBack et contrôles hors toucher sont difficiles. | Exposer des éléments accessibles ou migrer les menus vers des vues natives ; vérifier 48 dp, contraste et mise à l'échelle. |
| P2 | `ArcadeAudio` propose trois scènes de 32 pas × 250 ms ; aucune ambiance dédiée à chaque jeu, ni focus audio identifié dans ce module. | Répétition et concurrence possible avec la musique d'autres applications. | Composer des boucles plus longues et des couches par jeu, avec priorité aux signaux de jeu et gestion du focus audio. |
| P2 | Les contenus sont intégrés en tableaux dans `GameEngine` : 17 QCM, 18 poses, 17 dessins, 17 bluffs et 6 mélodies. Le tirage de prompt n'a pas d'historique anti-répétition. | Les mêmes cartes reviennent vite dans une soirée. | Packs de contenu versionnés FR/EN, identifiants stables, tirage sans répétition sur la session. |

## Refonte des dix jeux

Le principe commun : **tous prennent une décision ; l'un est sous les projecteurs**. Sur un téléphone, les actions privées passent de main en main avec un écran court. Sur plusieurs téléphones, elles se font en parallèle. Les chronos commencent lorsque les participants sont prêts, pas pendant le passage de l'appareil.

| Jeu | Version actuelle | Version cible et ressort social |
| --- | --- | --- |
| Culture G | QCM chronométré pour l'acteur. | Question simultanée : chacun répond ; l'acteur choisit avant la révélation s'il suit sa réponse ou celle d'un ami. Mauvaise réponse partagée, rivalité immédiate. Prévoir un mode solo de tour pour un seul téléphone. |
| Positions à la con | L'acteur fait une pose, le jury tranche. | Duo tiré au sort : l'acteur et un complice réalisent une pose compatible avec l'espace disponible ; les autres votent sur la réussite ou choisissent un handicap léger. Option sans effort physique. |
| Blind Test | Reconnaître un nom de mélodie originale ou un titre Spotify. | « Bruit de soirée » : un joueur imite/crée un son ou fredonne, les autres devinent parmi des réponses construites à partir d'une carte. Aucun micro requis sur un téléphone ; l'enregistrement local facultatif devient un mode supplémentaire. |
| Réflexe néon | Dix cibles pour l'acteur. | Face-à-face de 8 secondes avec alternance attaque/parade. Sur Internet, remplacer le temps absolu de toucher par une séquence locale signée et comparer des résultats après coup ; ne pas donner un avantage structurel au ping. |
| Roulette Royale | Une personne choisit un gobelet. | Chaque joueur cache un gobelet ou un « sauvetage » ; l'acteur choisit et peut proposer un échange. Révélation simultanée : le bluff et la discussion comptent davantage que le hasard pur. |
| Dessin maudit | Un dessinateur ; le joueur suivant répond à un QCM. | Tous les autres proposent une réponse libre ou choisissent parmi des leurres créés par le groupe ; galerie et vote du dessin le plus absurde en fin de manche. |
| Mémoire flash | Une personne répète une suite de couleurs. | Chaîne coopérative : chaque joueur ajoute un symbole à retenir ; le groupe peut dépenser une aide commune. Le perdant est celui qui casse la chaîne, avec une fin brève et drôle. |
| Rythme ou rien | Quatre frappes de l'acteur. | Call-and-response : l'acteur invente une phrase de quatre temps, chacun la reproduit ; le score montre les écarts. Ne lancer cette version qu'après correction de l'horloge et calibration. |
| Bluff royal | L'acteur joue un prompt ; jury oui/non. | Carte secrète « vrai ou inventé » : l'acteur raconte une anecdote, chaque autre joueur interroge puis vote ; l'acteur marque s'il trompe la majorité. Prévoir des prompts qui n'exigent pas de révéler une information personnelle. |
| Bombe à bulles | Huit touches en relais. | Relais à choix : chaque passeur choisit entre accélérer le chrono ou transférer un handicap amusant. Sur un seul téléphone, une passe vaut une mini-phase lisible, pas une transmission physique pour un simple tap. |

## Expérience, image et son

**Direction artistique.** Conserver le mélange nuit, néon et pixel, mais définir des composants mesurables : silhouette forte des personnages, sprite de grande taille dans l'action, états animé/repos/réaction, palette par famille de jeu, typographie des commandes et du résultat. Les captures actuelles montrent des scènes distinctes derrière une structure d'écran presque constante. La refonte doit toucher la **hiérarchie et les réactions** : visage du joueur, objet de jeu et action suivante visibles immédiatement ; moins de cadres décoratifs. Utiliser des références d'arcade sans emprunter l'identité visuelle ou les personnages d'autres licences.

**Première minute.** Deux pseudos, deux portraits, choix « même téléphone » et premier défi en moins de 60 secondes. Mettre les réglages secondaires et la connexion à un service musical après la première manche. Pour chaque jeu, un tutoriel de dix secondes avec exemple jouable ; bouton passer pour les habitués. Les mises à jour de Jackbox montrent l'intérêt des actions pour le public et des tutoriels qu'on peut ignorer, mais il faut valider ces principes avec les joueurs d'Apéro Royale.

**Rythme de soirée.** Trois formats : Express 10 min, Classique 20 min, Libre. Après la révélation : scores, gorgées virtuelles, un moment à commenter, puis « manche suivante » sans tunnel de menus. Le classement doit montrer aussi les réussites drôles (meilleur bluff, meilleur dessin, sauvetage), pour éviter qu'un mauvais départ rende la suite inutile.

**Audio.** Garder la musique discrète par défaut ; offrir muet, chill et arcade depuis la partie. Écrire une vraie banque de motifs/stems par famille de jeux et des jingles courts pour passage, pari, révélation et fin de manche. Les indices de rythme doivent partager **la même horloge** que le son. Le moteur actuel à 22,05 kHz et paquets de 250 ms mérite un profilage sur appareils ; Android documente les chemins audio à faible latence, sans garantie uniforme par téléphone. Gérer explicitement la perte de focus audio et le mixage avec Spotify.

**Confort.** Vérifier sur petits et grands écrans le contraste ≥ 4,5:1 du texte courant, les cibles tactiles ≥ 48 dp, le texte adaptable et une navigation TalkBack pour les menus et actions essentielles. Les commandes de jeu peuvent rester personnalisées, mais elles doivent exposer leur sens aux technologies d'assistance.

## Multijoueur : preuve attendue avant de promettre « fluide »

1. Passer des coordonnées tactiles transmises par réseau à des commandes sémantiques (`vote`, `choisir_gobelet`, `répondre`) avec identifiant de manche, séquence et accusé de réception. L'hôte refuse les doublons et les commandes d'une ancienne manche.
2. Séparer état public, état privé de l'acteur et secret de l'hôte. Tester qu'un client invité ne reçoit jamais une réponse avant sa révélation.
3. Mesurer le délai aller-retour, montrer « reconnexion » et restaurer la place du joueur. Un code de salle reste un accès de salle ; prévoir une identité de connexion indépendante pour empêcher l'usurpation d'un pseudo après partage du code.
4. Tester les chemins réels : 2 et 6 appareils en Wi-Fi, Bluetooth compatible et Internet ; entrée tardive, perte d'un client, veille, retour d'application, deux actions simultanées, hôte qui quitte. Le relais MQTT public actuel est adapté à l'expérimentation, pas à une garantie de disponibilité.
5. Garder les mini-jeux de réflexe locaux ou les comparer avec une méthode qui ne confond pas adresse et latence réseau. L'hôte reste arbitre, mais la validation doit se faire sur un temps partagé fiable.

## Ordre de réalisation proposé

**Lot 1 — confiance, une à deux itérations.** Corriger rythme et fuite d'informations ; supprimer les doubles scores en cas de retransmission ; instrumentation des étapes et des latences ; tests automatisés de l'état caché, de la rotation et de la reprise. Aucun nouveau décor ne compense une manche injuste.

**Lot 2 — la soirée devient collective.** Refaire d'abord Culture G, Dessin, Bluff et Blind Test avec une action pour chacun ; simplifier la préparation de manche ; tester à deux, quatre et six personnes sur un et plusieurs appareils. Revoir les six autres à partir des observations de ces sessions.

**Lot 3 — identité visuelle et sonore.** Prototype de deux jeux avec sprites plus expressifs, interface propre au gameplay et composition sonore dédiée ; choix entre variantes par tests de compréhension et d'envie de rejouer. Étendre ensuite le système aux huit autres.

**Lot 4 — contenu et social.** Packs FR/EN sans répétition, classement de salle et profils stables, réactions rapides pendant les temps morts, règles secrètes mieux mises en scène. Le chat textuel en salle Internet peut venir après la stabilité de la partie : autour d'une table, les joueurs se parlent déjà.

## Critères d'acceptation à mesurer

Ce sont des **objectifs de conception**, non des performances constatées : première manche en moins de 60 s sur un téléphone ; chaque joueur fait au moins une action significative par manche ; aucun participant sans interaction plus de 45 s ; une manche Express tient en 30–75 s ; taux de manches terminées > 95 % pendant un test de soirée ; reprise sans score doublé ; aucune réponse secrète reçue trop tôt ; connexion Wi-Fi p95 < 10 s et accusé d'action p95 < 300 ms, Internet p95 < 800 ms sur le réseau de test ; son coupé/repris correctement quand une autre application prend le focus ; parcours des menus au lecteur d'écran.

Un panel de 6 à 10 groupes de 2–6 joueurs doit servir à trancher l'ergonomie et le plaisir. Mesurer aussi l'envie de relancer une manche, le nombre de rires/réactions spontanées, les explications nécessaires et les moments où quelqu'un repose son téléphone. Ces observations valent mieux qu'une promesse « AAA » ou « parfaite ».

## Spotify et consommation

Spotify reste un **bonus facultatif**, pas la condition du défi musical : les règles de développement et de quota ont changé en 2026 ; le contrôle de lecture requiert notamment Premium et un appareil actif. Une expérience hors ligne amusante doit fonctionner immédiatement avec six amis.

Conserver le ton apéro et les mises, mais offrir par profil un plafond de gorgées virtuelles et une substitution « eau / défi / passe ». Afficher des compteurs de jeu, sans prétendre mesurer l'alcoolémie. L'OMS rappelle qu'il n'existe pas de consommation d'alcool sans risque ; ces options élargissent aussi le groupe qui peut participer sans briser la soirée.

## Sources externes, consultées le 4 octobre 2026

- [Jackbox Party Pack 11 : mise à jour des interactions du public, tutoriels facultatifs, contenu et audio](https://www.jackboxgames.com/blog/party-pack-11-free-content-update)
- [Jackbox Hear Say : sons créés par les joueurs et vote du groupe](https://www.jackboxgames.com/blog/introducing-the-fourth-game-in-party-pack-11-hear-say)
- [Android : accessibilité mobile, lisibilité, TalkBack et cibles tactiles](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility)
- [Android : audio de jeu à faible latence](https://developer.android.com/games/sdk/oboe/low-latency-audio)
- [Android : gestion du focus audio](https://developer.android.com/media/optimize/audio-focus)
- [Spotify : changements du mode développement en février 2026](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide)
- [Spotify : conditions du contrôle de lecture](https://developer.spotify.com/documentation/web-api/reference/start-a-users-playback)
- [Godot : validation des actions clientes en multijoueur](https://docs.godotengine.org/en/4.7/tutorials/networking/high_level_multiplayer.html)
- [Photon : compensation de la latence pour les jeux rapides](https://doc.photonengine.com/fusion/v2/manual/advanced/lag-compensation)
- [OMS : effets et risques de l'alcool](https://www.who.int/news-room/fact-sheets/detail/alcohol)

## Limites de cet audit

Les captures proviennent de l'émulateur du dépôt. Le ressenti artistique, la fatigue musicale, l'ergonomie en soirée et la stabilité Wi-Fi/Bluetooth/Internet n'ont pas été retestés sur de vrais groupes pour ce document. Le code prouve l'existence des chemins, pas leur fiabilité en conditions réelles. Les objectifs chiffrés ci-dessus restent à vérifier par télémétrie locale de test et sessions observées, sans suivre les utilisateurs de la version publique.

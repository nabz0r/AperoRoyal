# Les dix défis qui donnent envie de relancer une manche

**Diagnostic et spécification de jeu, 4 octobre 2026.** Analyse initiale sur Apéro Royale 1.3.0 (`6e6893b`), mise à jour pour 1.4.0. Les refontes collectives décrites ci-dessous restent à prototyper ; la suppression du quiz Spotify, le menu musical multi-services et la nouvelle direction de l'accueil sont déjà livrés dans 1.4.0. Aucun test avec un groupe réel n'a encore mesuré le plaisir, la compréhension ou le partage.

## L'objectif produit

Une manche réussie doit produire quatre choses : **une décision de chacun**, **un moment qui bascule**, **une révélation que l'on commente**, puis **une revanche accessible en une touche**. C'est notre hypothèse de conception, pas une promesse de viralité. Les exemples publics de Jackbox donnent aux spectateurs des actions, des créations à juger et parfois une galerie ; Nintendo alterne des manches courtes, des rôles et des règles de groupe. Nous adaptons ces principes à 2–6 amis autour d'un seul téléphone ou de plusieurs téléphones, sans reprendre leurs personnages ou leurs règles à l'identique. [Jackbox, mise à jour Party Pack 11](https://www.jackboxgames.com/blog/party-pack-11-free-content-update), [Nintendo, présentation de Jamboree](https://www.nintendo.com/en-ca/whatsnew/super-mario-party-jamboree-heres-a-quick-overview-of-the-game/), [Jackbox, galerie Civic Doodle](https://www.jackboxgames.com/blog/custom-civic-doodle-t-shirts-and-postcards-available).

**Musique et plateformes.** L'ancien code 1.3.0 demandait à la Web API Spotify des titres et déclenchait leur lecture pendant un quiz. La [Developer Policy Spotify](https://developer.spotify.com/policy) interdit explicitement les jeux et quiz construits avec sa plateforme ; [ses conseils de conformité](https://developer.spotify.com/compliance-tips) citent le « name that tune ». La version 1.4.0 retire cette intégration et propose des raccourcis indépendants vers Spotify, Deezer, Apple Music et Amazon Music. Le joueur choisit sa plateforme et peut enregistrer un lien de playlist HTTPS ; son application musicale gère ensuite le compte et la lecture. L'architecture de `MusicLinks` permet d'ajouter d'autres destinations validées, mais un contrôle intégré de la lecture exige des contrats et SDK adaptés à chaque fournisseur ainsi que des tests sur appareils et comptes réels. [MusicKit Android](https://developer.apple.com/musickit/) documente une voie technique pour Apple Music ; [Amazon Music Web API](https://www.developer.amazon.com/docs/music/landing_home.html) était en bêta fermée lors de cette recherche. Le jeu musical repose sur des sons originaux ou dûment licenciés, sans analyser les chansons de services externes.

**Cadence cible, à vérifier en soirée :** 5–10 s pour comprendre la règle, 20–45 s pour agir, 3–6 s de révélation, puis « revanche » ou « suivant ». Sur un téléphone partagé, une action privée à six joueurs ne doit pas imposer six longs formulaires : choix courts, relais clair, chrono suspendu pendant le passage. Sur plusieurs téléphones, les choix peuvent être simultanés. Le jeu doit toujours rester jouable sans micro, caméra, Spotify ou connexion Internet.

**Boucle de découverte envisagée.** Une personne termine une manche et choisit d'exporter son moment préféré ; son ami reçoit une image lisible **et un lien de salle** ; le lien ouvre la salle si l'application est installée, ou une page qui explique le jeu et son installation ; l'ami rejoint avant la prochaine manche et produit à son tour un souvenir. Il faudra un domaine contrôlé, des [Android App Links vérifiés](https://developer.android.com/training/app-links/about), un code de salle temporaire et un état « salon en cours » compréhensible. Un lien vers le seul APK ne suffit pas à ce parcours. Valider aussi les [règles actuelles de vérification des développeurs Android](https://developer.android.com/developer-verification) avant d'organiser une diffusion large. Mesurer séparément les exports **choisis**, ouvertures du lien, installations, arrivées en salle et premières manches terminées ; ne pas appeler « viralité » un simple nombre de captures produites.

## 01 · Culture G — « Suis-moi si tu oses »

**Aujourd'hui.** Dix-sept QCM FR/EN, quatre réponses, un acteur et 16 secondes ; une erreur clôt la manche. Les autres ont déjà choisi de le couvrir ou de le défier, mais n'interviennent plus dans la question. Les questions visibles mêlent faits évidents et formulations à vérifier (« neuf cerveaux », « pluie de diamants ») ; les leurres ont souvent une vraisemblance inégale. Voir [contenu](../app/src/main/java/com/aperoroyale/GameEngine.java#L876), [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1051) et [score](../app/src/main/java/com/aperoroyale/GameEngine.java#L485).

**Refonte.** Afficher une question et trois réponses plausibles, avec une micro-explication sourcée à la révélation. Chaque ami verrouille une réponse. L'acteur peut conserver la sienne ou « suivre » une personne sans voir son choix : il crée ainsi une alliance à risque. À deux sur un téléphone, l'acteur verrouille d'abord, passe l'appareil, puis l'autre choisit ; le choix « suivre » est décidé avant la révélation. Sur plusieurs appareils, tout le monde répond en parallèle. Un bon choix rapporte aux répondants ; l'acteur obtient un bonus seulement si son pari social réussit. Éviter qu'une mauvaise réponse impose une pénalité à toute l'équipe sans accord.

**Moment racontable.** Révélation animée des alliances : « 4 personnes ont suivi Nova… et Nova a suivi Pixel ». Carte partageable avec la question, les choix agrégés et la réponse, sans pseudos par défaut. **À tester :** compréhension du choix « suivre » en moins de 10 s ; chaque personne répond ; aucune question ambiguë après revue éditoriale FR/EN ; mêmes réponses et scores sur hôte et invités.

## 02 · Positions à la con — « Casting catastrophe »

**Aujourd'hui.** Dix-huit défis de pose, puis un jury « validé / raté ». Le joueur lance lui-même le jury ; celui-ci vote pendant 20 secondes. Une partie des cartes implique contact, déplacement ou équilibre, alors qu'aucune préférence d'accessibilité ni d'espace n'est demandée. À deux, un seul ami juge et décide seul de l'enjeu. Voir [cartes](../app/src/main/java/com/aperoroyale/GameEngine.java#L930), [pose/jury](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1064) et [verdict](../app/src/main/java/com/aperoroyale/GameEngine.java#L476).

**Refonte.** Au début, la salle règle « assis », « debout » ou « sans contact ». Une carte combine personnage, situation et contrainte comique : le réalisateur parmi les amis choisit une variation, l'acteur la joue pendant 10–15 s, puis les autres attribuent un titre ou un vote d'applaudissement. À deux, remplacer le jury binaire par un duel de deux poses courtes : chaque joueur joue et l'application donne le même temps aux deux ; le groupe choisit le moment préféré sans sanction automatique pour le perdant. Un bouton **passer sans boire** remplace les défis incompatibles avec l'espace ou la mobilité.

**Moment racontable.** Affiche générée avec les avatars, le titre choisi par le groupe et la consigne ; photo réelle seulement si les personnes concernées acceptent avant l'export. **À tester :** aucune carte n'exige contact non choisi ; deux joueurs ont une action chacun ; consigne comprise sans démonstration ; temps debout/restreint validé dans un vrai salon.

## 03 · Blind Test — « Studio des bruits »

**Aujourd'hui.** Six motifs synthétiques hors ligne correspondent à six étiquettes (« montée », « descente »…). Le mode Spotify de 1.3.0 a été retiré dans 1.4.0. Le catalogue original est court et s'épuise vite. Sur des appareils éloignés, l'acteur peut ne pas entendre le son joué sur l'hôte. Voir [motifs](../app/src/main/java/com/aperoroyale/ArcadeAudio.java), [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java) et [raccourcis musicaux](../app/src/main/java/com/aperoroyale/MusicLinks.java).

**Refonte.** Deux modes entièrement originaux. **Studio** (même pièce) : une carte demande d'imiter un son de soirée sans micro obligatoire ; les autres devinent parmi trois scènes absurdes et votent ensuite pour la meilleure interprétation. **Arcade sonore** (en ligne) : l'application génère localement une phrase courte à partir de banques de timbres et rythmes originaux ; chaque appareil joue la même graine, puis tous identifient un élément ou reproduisent le motif. À deux sur un téléphone, l'acteur découvre la carte puis passe le téléphone au devineur après l'imitation. Sur plusieurs appareils, réponses simultanées. Un mode silencieux utilise une visualisation de rythme et des vibrations.

**Moment racontable.** Carte de la « scène sonore » et du vote ; aucun enregistrement vocal ni extrait de chanson exporté par défaut. **À tester :** six parties consécutives sans répétition perçue ; son audible sur l'appareil qui doit répondre ; le mode en ligne ne dépend pas du haut-parleur hôte ; aucun appel Spotify dans la logique du défi.

## 04 · Réflexe néon — « Duel des pièges »

**Aujourd'hui.** L'acteur touche dix cibles en 15 secondes. Un invité affiche localement la cible suivante avant accusé de l'hôte ; l'hôte refuse une commande si l'indice attendu n'est pas le bon. La difficulté varie donc avec le réseau et les retransmissions. Les autres regardent après leur prono. Voir [validation](../app/src/main/java/com/aperoroyale/GameEngine.java#L411), [affichage](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1133) et [commande](../app/src/main/java/com/aperoroyale/MainActivity.java#L1200).

**Refonte.** Un ami choisit avant le départ un piège lisible (cibles qui se déplacent, faux signal ou inversion de couleur), l'acteur choisit un contre-pouvoir à usage unique. À deux : deux courses successives sur la **même graine** et le même temps local, puis comparaison. À plusieurs : chacun peut faire une courte tentative sur son appareil, les autres attribuent le piège ; sur un téléphone, relais de tentatives, non simultanéité forcée. Valider localement les coups et envoyer un résultat final avec identifiant de manche, durée monotone et trace des actions. Un client non fiable ne peut pas prouver cryptographiquement ses réflexes : ne pas présenter le classement Internet comme une compétition infalsifiable entre inconnus.

**Moment racontable.** Duel de 8 s avec écart final et « piège décisif » en image animée sans vidéo du joueur. **À tester :** mêmes cibles à graine égale ; perte ou réordonnancement d'un paquet sans toucher fantôme ; résultats comparables à 50/150/300 ms de latence simulée ; grandes cibles sur petit écran.

## 05 · Roulette Royale — « Le gobelet du traître »

**Aujourd'hui.** Six gobelets, de un à trois pièges selon la mise, un choix par l'acteur ; les autres n'ont aucune prise après le prono. La probabilité de perdre est essentiellement 1/6, 2/6 ou 3/6 selon la mise ; la manche raconte surtout un tirage. Voir [tirage et sécurité](../app/src/main/java/com/aperoroyale/GameEngine.java#L435) et [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1157).

**Refonte.** Avant le choix, chaque ami pose secrètement un jeton **indice**, **protection** ou **leurre** sur un gobelet. L'acteur voit le nombre de jetons, pas leur type, puis choisit ou propose un échange. Révélation en deux temps : jetons, puis pièges. À deux sur un téléphone, l'autre pose un seul jeton en privé avant de passer ; à plusieurs appareils, les jetons arrivent simultanément. Le générateur doit afficher clairement les probabilités et garantir que le résultat a été fixé **avant** les choix ; une graine de manche révélée après coup permet de vérifier le tirage.

**Moment racontable.** Petit replay des jetons et du dernier échange : « Tu as refusé le gobelet protégé ». **À tester :** au moins une vraie décision chez chaque joueur ; distribution des pertes conforme aux règles simulées ; aucun gobelet perdant divulgué avant la révélation ; pas de sanction répétée par simple hasard.

## 06 · Dessin maudit — « Les faux titres »

**Aujourd'hui.** Un acteur dessine l'une de 17 consignes. Seul le joueur suivant devine, parmi quatre intitulés fournis par le jeu ; les autres regardent. Les leurres proviennent de consignes voisines du tableau, souvent sans rapport, donc le dessin peut être déchiffré sans échange social. Le dessin n'a pas de durée maximale : il peut bloquer la soirée. Voir [consignes](../app/src/main/java/com/aperoroyale/GameEngine.java#L950), [dessin et réponses](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1183) et [passage au devineur](../app/src/main/java/com/aperoroyale/MainActivity.java#L224).

**Refonte.** 25 secondes de dessin, une couleur de base et un outil « gomme » limité ou un deuxième trait spécial. Chaque non-artiste propose un titre leurre très court ; tous devinent ensuite la vraie consigne. Les auteurs des leurres marquent quand quelqu'un les choisit ; l'artiste marque quand le groupe comprend. Sur un téléphone partagé, les titres peuvent être choisis parmi des cartes préécrites pertinentes ou saisis facultativement, avec passage privé ; à deux, le devineur reçoit deux leurres du jeu, puis peut baptiser le dessin après révélation. Sur plusieurs téléphones, titres et votes se font en parallèle. Si quelqu'un ne soumet rien, la manche continue avec un leurre du jeu.

**Moment racontable.** Galerie locale montrant dessin, faux titres, vrai titre et auteurs **seulement s'ils acceptent l'export**. La galerie est un précédent observable dans les jeux de dessin Jackbox, sans prouver qu'elle rendra Apéro Royale viral. [Drawful](https://www-origin.jackboxgames.com/games/drawful), [Civic Doodle](https://www.jackboxgames.com/blog/custom-civic-doodle-t-shirts-and-postcards-available). **À tester :** chacun devine ; un dessin vide ne gagne pas automatiquement ; aucun texte injurieux publié par défaut ; export disponible sans compte ni permission de stockage générale.

## 07 · Mémoire flash — « La chaîne impossible »

**Aujourd'hui.** L'acteur regarde 4 à 7 symboles puis les répète avant un délai total de 22 secondes ; personne d'autre ne touche la séquence. Le temps de révélation consomme une partie du chrono. Les symboles sont couleur **et** forme, ce qui aide la lisibilité, mais la courbe de difficulté vient surtout de la longueur. Voir [génération](../app/src/main/java/com/aperoroyale/GameEngine.java#L290), [chrono](../app/src/main/java/com/aperoroyale/GameEngine.java#L352) et [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1256).

**Refonte.** Chaîne coopérative : une personne rejoue la séquence puis ajoute un symbole ; la suivante recommence, jusqu'à un plafond court (par exemple six symboles) ou une erreur. Le groupe possède une seule « seconde chance » qu'un joueur peut proposer de dépenser ; les autres acceptent ou non. À deux, les deux alternent réellement ; à six, l'interface garde une séquence assez courte pour que personne n'attende une minute. Sur plusieurs appareils, seul le porteur voit et répond, les autres peuvent choisir collectivement la seconde chance. Sur un téléphone, afficher une carte de passage qui cache la séquence et suspend le chrono. Le temps de réponse démarre après la démonstration, pas avant.

**Moment racontable.** Replay coloré de la chaîne et du symbole qui l'a cassée, sans blâme personnel forcé. **À tester :** chacun participe avant la fin d'une manche à six ; le porteur suivant ne voit pas le secret avant son tour ; reprise après veille sans saut de phase ; joueurs daltoniens distinguent formes et noms.

## 08 · Rythme ou rien — « Réponds au beat »

**Aujourd'hui.** L'acteur réussit quatre touches proches du centre d'une période de 600 ms ; les autres n'agissent pas. L'affichage et la validation hôte utilisent désormais la même période, mais le signal sonore passe par un moteur séparé, et la fenêtre réseau de 235 ms reste sensible au délai du téléphone invité. Les touches n'ont pas besoin d'être consécutives. Voir [horloge](../app/src/main/java/com/aperoroyale/RhythmClock.java), [validation](../app/src/main/java/com/aperoroyale/GameEngine.java#L421) et [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1280).

**Refonte.** Un premier joueur crée une phrase de quatre frappes ; un autre la reproduit. Le score mesure les **intervalles relatifs** et montre l'écart de chaque frappe. Le signal sonore et la vague visuelle partent du même horaire local, avec calibration volontaire de la latence audio. Sur un téléphone, les deux jouent successivement. Sur plusieurs appareils, chaque appareil enregistre localement les temps monotones de sa réponse ; l'hôte compare les intervalles reçus, sans utiliser l'heure d'arrivée réseau comme temps de frappe. Une alternative visuelle et haptique reste jouable sans son. Pas de récompense basée sur des morceaux Spotify ou sur l'accès au micro.

**Moment racontable.** Une carte animée des deux courbes de rythme et du « raté légendaire ». **À tester :** écart comparable à latence réseau variable ; mêmes résultats avec son coupé ; calibration rapide, facultative et mémorisée ; pas de manche impossible sur téléphone audio lent.

## 09 · Bluff royal — « Interrogatoire minute »

**Aujourd'hui.** L'acteur raconte une anecdote sur l'un des 17 thèmes et choisit ensuite secrètement « vrai » ou « inventé » ; le jury vote. L'idée sociale est forte, mais le jeu ne peut pas vérifier qu'une histoire personnelle est vraie. À deux, le jugement est un unique vote sans relance ni question. Voir [consignes](../app/src/main/java/com/aperoroyale/GameEngine.java#L970), [choix/verdict](../app/src/main/java/com/aperoroyale/GameEngine.java#L455) et [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1307).

**Refonte.** Deux formats annoncés clairement. **Alibi** : l'application assigne en secret une carte « vrai » ou « inventé » avec des détails fictifs cohérents ; l'acteur improvise autour d'elle, les amis posent chacun une question courte et votent. Le résultat est objectivement connu du jeu. **Anecdote perso** : l'acteur choisit sa propre vérité, le groupe joue pour le rire et la surprise, sans présenter le vote comme une vérification factuelle ni imposer une gorgée sur cette base. À deux, le juge a une seule question puis un verdict ; à six, limiter l'interrogatoire à deux questions choisies par vote pour éviter les longueurs. Les thèmes intimes ou humiliants sont exclus par défaut.

**Moment racontable.** Carte « alibi / questions / verdict / retournement » rédigée par le jeu, sans enregistrer la voix ni publier une histoire personnelle sans choix explicite. **À tester :** la majorité est calculée de façon claire en cas d'égalité ; l'acteur ne peut pas changer son secret après le premier vote ; tous comprennent la différence entre fiction et anecdote personnelle.

## 10 · Bombe à bulles — « La mauvaise couleur »

**Aujourd'hui.** Chaque joueur doit faire deux touches avant de passer ; il faut au moins huit touches en 30 secondes, ou douze à six joueurs. Le relais est réel, mais la touche ne comporte aucun choix, et le même téléphone n'affiche pas de passage protégé entre deux personnes pendant que le chrono court. Voir [ordre et objectif](../app/src/main/java/com/aperoroyale/GameEngine.java#L534) et [écran](../app/src/main/java/com/aperoroyale/ArcadeView.java#L1327).

**Refonte.** À chaque passage, le porteur choisit l'un de deux « fils » ou outils ; l'un avance la désactivation, l'autre crée une contrainte drôle pour le suivant. Le groupe dispose d'une réserve d'indices et vote une fois sur son usage. Une mauvaise décision raccourcit le temps **de jeu**, pas une obligation de boire plus. Sur un téléphone, le chrono est suspendu pendant « Passe à Nova », puis repart quand Nova confirme ; sur plusieurs appareils, chaque joueur agit sur le sien et l'hôte valide le propriétaire du tour. Des manches courtes avec un seul renversement valent mieux que douze touches sans conséquence.

**Moment racontable.** Replay des choix de fils et du sauvetage à la dernière seconde. **À tester :** chacun prend une décision, y compris à deux ; personne ne peut jouer à la place d'un invité distant ; la reconnexion ne détruit pas la bombe ; le temps de passage physique ne pénalise pas l'équipe.

## Ce qui rend ces dix jeux reconnaissables

Les captures 1.3.0 montrent dix **décors** distincts, mais une composition très voisine : titre, grand cadre, même jauge et même bloc d'actions. La refonte visuelle doit réserver le centre à **l'objet que l'on manipule**, pas au cadre. [Captures actuelles](screenshots/games/).

| Jeu | Sprite et mouvement à produire | Signature sonore originale | Révélation |
| --- | --- | --- | --- |
| Culture G | Pupitre de quiz, avatars qui s'alignent ou bifurquent | Trois notes « choix verrouillé », accord de réponse | Alliances qui se retournent |
| Positions | Duo de silhouettes articulées, rideau de scène | Roulement bref, applaudissements optionnels | Affiche et titre du groupe |
| Studio des bruits | Console cassette et formes d'onde dessinées | Banque de bruitages originaux, silence utile | Scène cachée dévoilée |
| Réflexe | Cibles expressives et piège lisible avant l'action | Hits courts à timbre variable | Chronos côte à côte |
| Roulette | Six gobelets qui se retournent, jetons visibles | Cliquetis progressif puis arrêt net | Pièges puis protection |
| Dessin | Toile plein écran et outil pixel, avatars de faux titres | Trait discret, chime de galerie | Faux titres attribués |
| Mémoire | Chaîne de reliques colorées avec symboles | Une note par symbole, coupure à l'erreur | Séquence rejouée |
| Rythme | Deux courbes d'impulsions, point d'impact clair | Beat horodaté, son désactivable | Décalage des courbes |
| Bluff | Dossier d'alibi, questions comme cartes | Tic de suspense, sceau du verdict | Alibi secret retourné |
| Bombe | Câbles et outils différenciés, mèche visible | Tic qui accélère sans couvrir les voix | Dernier choix en gros plan |

Chaque sprite a au minimum repos, anticipation, action, succès et échec. Les dix guides devront montrer **un exemple animé de 5 secondes** et un bouton passer ; cette piste suit l'exemple d'un tutoriel facultatif ajouté par Jackbox, sans supposer que ce choix suffira à notre public. [Jackbox, mise à jour Party Pack 11](https://www.jackboxgames.com/blog/party-pack-11-free-content-update).

## Contrat commun avant d'implémenter les dix refontes

1. **Manche et rôles.** Un identifiant de manche unique, une phase explicite (`brief`, `commit`, `act`, `reveal`, `result`), un propriétaire de chaque action et une date limite par phase. Le passage d'un téléphone suspend les seuls chronos qui doivent attendre une personne ; le temps d'action reprend sur « prêt ».
2. **Réseau.** Commandes sémantiques avec `{roundId, playerId, sequence, action, payload}`, accusé de réception et déduplication côté hôte. Reconnexion sur un instantané adapté au rôle. Un code de salon n'identifie pas un joueur : donner un jeton de session individuel, sans exposer les réponses cachées aux autres invités. Les jeux de réflexe et de rythme mesurent les touches localement et comparent des résultats, sans attribuer une victoire à la vitesse de livraison du paquet.
3. **Contenu.** Cartes FR/EN à identifiant stable, version, catégorie, difficulté, tags `assis`, `sans_contact`, `deux_joueurs`, `distance`, durée et source pour les faits. Tirage sans répétition jusqu'à épuisement du paquet, puis mélange. Revue native des deux langues : la traduction n'est pas un test de jouabilité. Les 17 QCM, 18 poses, 17 dessins, 17 bluffs et 6 motifs actuels sont un prototype de contenu, pas un catalogue de longue durée.
4. **Scores et sauvegarde.** Séparer points d'acteur, aide, piège, leurre, jury et victoire d'équipe dans l'historique. Identifiant de profil stable plutôt que le pseudo comme clé de statistiques. Écriture du résultat exactement une fois par manche, y compris après reprise ou doublon réseau. Classement de **la salle** synchronisé et classement historique local clairement nommé.
5. **Partage volontaire.** Galerie de fin de soirée et export image par l'Android Sharesheet, avec prévisualisation, retrait des noms/photos par défaut et consentement pour toute création attribuable. Pas de publication automatique, de notification à des contacts ni d'accès global aux photos. [Android Sharesheet](https://developer.android.com/social-and-messaging/guides/media-sharing), [Android, permissions minimales](https://developer.android.com/privacy-and-security/minimize-permission-requests).
6. **Consommation libre.** Les gorgées restent des compteurs virtuels. Chaque profil peut choisir eau, défi, zéro consommation ou pause ; plafond individuel de la soirée, sans multiplier les pénalités pour pousser à rejouer. L'OMS rappelle qu'il n'existe pas de consommation d'alcool sans risque. [OMS](https://www.who.int/europe/news/item/04-01-2023-no-level-of-alcohol-consumption-is-safe-for-our-health/).

## Ordre de production recommandé

| Lot | Livrable testable | Condition pour passer au lot suivant |
| --- | --- | --- |
| **0 — confiance** | Retrait de Spotify du quiz et des appels API intégrés au jeu **livré en 1.4.0** ; restent : contrats de manche, identité et commandes idempotentes, événement de résultat unique, plafond/passe | Tests des secrets, doublons, reprise et conformité du mode musical ; APK installable |
| **1 — quatre preuves de fun** | Prototypes Culture G, Dessin, Bluff et Bombe à 2 joueurs, un puis deux téléphones ; tutoriels courts et révélations dédiées | Deux sessions de jeu réelles par prototype ; chacun fait une action ; aucune explication orale indispensable |
| **2 — perception et hasard** | Roulette et Positions avec vrais choix des amis ; premier système de galerie consensuelle | Choix compris, issue jugée juste, export sans donnée involontaire |
| **3 — précision** | Réflexe, Mémoire et Rythme avec horloge/événements robustes et tests de latence | Profils 50/150/300 ms, veille/reprise, six joueurs et petits écrans sans avantage réseau évident |
| **4 — son** | Studio des bruits original hors ligne et multi-appareils, puis pack de cartes FR/EN plus large pour les dix jeux | Six manches de suite sans répétition dominante ni dépendance Spotify/micro |

### Tickets d'implémentation prêts à ouvrir

| ID | Travail et fichiers de départ | Test de sortie |
| --- | --- | --- |
| AR-01 **livré 1.4.0** | Supprimer les appels Spotify du Blind Test et de la radio ; ajouter `MusicLinks` pour Spotify, Deezer, Apple Music et Amazon Music ; corriger libellés et guides | Aucune lecture de titre externe pilotée par le jeu ; défi sonore hors ligne terminé à deux |
| AR-02 | Introduire `RoundId`, phase, rôles et résultats dans `GameEngine` ; sérialisation versionnée et migration de sauvegarde | Reprise pendant chaque phase, résultat inscrit une seule fois, deux commandes identiques sans double score |
| AR-03 | Remplacer les coordonnées réseau par des commandes typées dans `PartyNetwork`, `BluetoothPartyNetwork`, `InternetPartyNetwork` et `MainActivity` ; jeton de session par joueur | Tests 2/6 clients, action d'un ancien tour rejetée, réponse secrète absente du mauvais instantané, reconnexion au bon rôle |
| AR-04 | Prototyper Culture G et Dessin avec deux profils locaux puis un invité ; faux titres et réponses collectives dans `GameEngine`, `ArcadeView`, `GameStore` | Deux amis participent à chaque manche ; quatre et six amis terminent sans attente excessive ; score identique sur les appareils |
| AR-05 | Prototyper Bluff et Bombe, avec deux rôles distincts et passage privé dans `GameEngine`/`ArcadeView` | À deux, chacun fait une action de fond ; vérité de l'Alibi figée avant le vote ; passage de bombe sans chrono perdu |
| AR-06 | Introduire des packs de contenu versionnés et un validateur FR/EN dans `app/src/main/assets` et les tests | Aucune clé manquante, tirage sans répétition jusqu'à épuisement, sources de QCM conservées |
| AR-07 | Créer la galerie locale et l'export volontaire avec un URI `content://` et l'Android Sharesheet | Prévisualisation, retrait des pseudos/photos par défaut, export annulable, aucun droit général aux médias |
| AR-08 | Réaliser les six autres refontes par paires, avec sprites, son, guide et mesures de latence propres à chaque jeu | Critères par jeu ci-dessus et sessions réelles 2/4/6 personnes sur un et plusieurs appareils |

**Mesure de l'envie de partager, sans télémétrie imposée.** Sessions observées de 2, 4 et 6 personnes : temps de la première manche, part de joueurs ayant une action par manche, durée d'attente la plus longue, manche terminée/reprise, demande spontanée de revanche, moments cités au débrief et usage volontaire de l'export. Tester un et plusieurs appareils, débutants et habitués, FR/EN mélangés. Les objectifs de départ sont : première manche < 60 s, aucune attente passive > 45 s, > 95 % de manches terminées sur le réseau de test et au moins un moment que le groupe veut montrer ou raconter après cinq manches. Ce sont des **seuils à éprouver**, non des résultats mesurés. Suivre les résultats dans des notes de test locales consenties ; ne pas les convertir en promesse de croissance.

**Décision de portée.** Une mécanique qui fonctionne à quatre mais ennuie à deux ne remplace pas la version à deux. Une animation partageable qui exige un compte, de la consommation réelle ou l'enregistrement des amis ne sert pas cette soirée. Le produit gagne d'abord parce que les personnes présentes veulent immédiatement rejouer ensemble.

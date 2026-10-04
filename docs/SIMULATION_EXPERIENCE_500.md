# Audit de rythme, de répétition et de participation — 500 soirées simulées

**4 octobre 2026, audit initial 1.4.2 et contre-essai 1.4.3.** Cet audit complète le [test de robustesse du moteur](SIMULATION_500_PARTIES.md). Le test reproductible [`PartyExperienceSimulationTest`](../app/src/test/java/com/aperoroyale/PartyExperienceSimulationTest.java) traverse **500 soirées et 7 990 manches par exécution** avec le vrai tirage, les modes Vote/Libre/Turbo, deux à six profils FR/EN et les transitions du moteur. Il compte les variantes réellement tirées et les rôles qui touchent au mini-jeu. Le temps est un **modèle**, pas un chronométrage humain ni une mesure du réseau.

## Contre-essai 1.4.3 — un relais plus vivant, une attente encore longue

La version 1.4.3 enlève le prono générique avant Poses, Bluff et Bombe : les jurys et le relais donnent déjà aux amis une action. En Vote et Turbo, après deux manches où seuls certains participent au défi, la sélection suivante vient de ces trois jeux collectifs. La bombe fait choisir le prochain porteur, oblige tout le monde à la toucher avant de proposer un fil rouge/bleu risqué et suspend son chrono pendant le passage physique. Réflexe, Mémoire et Rythme possèdent six variantes mécaniques ; Roulette six dispositions de pièges ; les six motifs du Blind Test existent dans quatre tonalités. Le tirage de variantes épuise chaque paquet avant de le recommencer.

| Cohorte sur un téléphone | Moyenne modélisée 1.4.2 → 1.4.3 | Manches où seul l'acteur agit 1.4.2 → 1.4.3 | Plus longue attente sans action propre au mini-jeu 1.4.2 → 1.4.3 |
| --- | ---: | ---: | ---: |
| 2 joueurs, Vote | 41,7 → **41,3 s** | 59,5 → **52,8 %** | 1 → **1 manche** |
| 4 joueurs, Vote | 64,8 → **61,1 s** | 59,8 → **50,0 %** | 3 → **2 manches** |
| 6 joueurs, Vote | 87,3 → **81,0 s** | 60,7 → **50,9 %** | 5 → **2 manches** |
| 6 joueurs, Turbo | 59,3 → **53,1 s** | 59,7 → **51,4 %** | 5 → **2 manches** |

Le générateur a choisi **330 coupes de fil** dans les 989 manches de bombe de ce contre-essai : 156 réussites, 174 échecs, et 659 relais complets. Les statistiques comptent seulement des règles exécutées par le moteur ; elles n'observent ni plaisanteries, ni revanche volontaire, ni satisfaction. Les deux versions utilisent les mêmes 500 identifiants de soirée et les mêmes hypothèses de temps, mais leurs tirages divergent après les changements de mécanique : ce tableau compare des cohortes simulées, pas des manches appariées une à une. Le rapport actuel imprimé par le test couvre les 30 cohortes et ne répète aucune variante avant épuisement de son paquet.

**Décision : le critère « tous les jeux sont viraux » n'est pas atteint ni mesurable par cette simulation.** Six mini-jeux restent des défis solo au cœur de la manche ; à six sur un téléphone, 81 s modélisées en Vote et jusqu'à deux manches sans action propre au défi sont encore trop longues pour promettre une soirée fluide. Les variantes rendent les manches moins identiques, sans créer à elles seules un choix amusant pour les amis. Le mode Turbo raccourcit la préparation mais ne résout pas la participation. Les prototypes collectifs et les vrais tests de groupe décrits dans [la refonte des dix jeux](REFONTE_DIX_MINI_JEUX.md) restent nécessaires.

Les sections suivantes conservent les hypothèses, mesures et correctifs de l'audit initial **1.4.2** comme point de comparaison.

## Ce que le modèle suppose

Une sélection prend 3 s par personne et 2 s de passage sur un téléphone partagé ; les pronostics prennent 3 s + 2 s de passage par ami. Sur plusieurs téléphones, votes et pronostics sont supposés parallèles et prennent chacun 5 s pour la salle. Transition, confirmation, mise et résultat prennent ensemble 15 s sur un téléphone et 13 s sur plusieurs. Les actions sont estimées à 4–30 s selon le jeu, bornées par ses vrais chronos lorsque ceux-ci existent. Jury et devinette ajoutent du temps et des passages. Les actions humaines, le rire, les discussions, la latence, les déconnexions et les boissons peuvent allonger ces durées ; personne ne les a mesurées ici.

Chaque soirée a 12 à 20 manches. En mode Libre, le simulateur choisit souvent un jeu favori pour éprouver les répétitions. « Action dans le mini-jeu » signifie un toucher, une réponse, un dessin, un vote de jury ou un relais de bombe ; **cela ne signifie pas un choix intéressant**. Le vote du prochain jeu et le pronostic générique sont comptés dans la préparation, jamais comme action propre au mini-jeu.

## Résultats qui changent la décision produit

| Configuration | Manche moyenne modélisée | Part de préparation | Manches > 45 s | Manches où seul l'acteur agit dans le mini-jeu | Plus longue série sans action propre au mini-jeu |
| --- | ---: | ---: | ---: | ---: | ---: |
| 2 joueurs, 1 téléphone, Vote | 41,7 s | 67,2 % | 30,2 % | 59,5 % | 1 manche |
| 4 joueurs, 1 téléphone, Vote | 64,8 s | 74,1 % | 100 % | 59,8 % | 3 manches |
| 6 joueurs, 1 téléphone, Vote | **87,3 s** | **77,9 %** | **100 %** | **60,7 %** | **5 manches**, jusqu'à 434 s modélisées |
| 6 joueurs, 1 téléphone, Turbo | 59,3 s | 67,4 % | 90,1 % | 59,7 % | 5 manches |
| 6 joueurs, plusieurs téléphones, Vote | 36,8 s | 62,6 % | 9,1 % | 62,7 % | 5 manches |

La préparation de six amis sur un seul téléphone en Vote vaut environ **68 s par manche** dans ce scénario, avant l'action centrale. Même en supposant des gestes 35 % plus rapides, ce coût resterait proche de 44 s. Le mode Turbo évite le vote, mais conserve cinq pronostics séquentiels. Ces chiffres identifient un risque de rythme, pas la durée réelle d'une soirée. Ils invalident toute affirmation selon laquelle « 500 parties simulées » aurait prouvé que six personnes s'amusent sans attendre.

## Redondance et densité des dix jeux

| Jeu | Manches simulées | Qui agit dans le mini-jeu ? | Contenu distinct actuel | Diagnostic de rejouabilité |
| --- | ---: | --- | ---: | --- |
| Culture G | 743 | acteur | 17 QCM | Les amis ne font que pronostiquer ; réponses à créer par chacun. |
| Positions | 785 | acteur + jurés | 18 défis | Vrai moment de groupe, mais verdict binaire et même structure. |
| Blind Test | 718 | acteur | **6 motifs** | Banque trop courte ; les autres n'ont pas de pari sur le son lui-même. |
| Réflexe | 870 | acteur | cible aléatoire, **1 règle** | Les coordonnées changent, l'objectif « dix touches » reste identique. |
| Roulette | 800 | acteur | piège aléatoire, **1 règle** | Choix rapide ; les amis n'influencent pas le retournement. |
| Dessin | 765 | artiste + un devineur | 17 concepts | À six, quatre amis n'ont pas de rôle pendant la révélation. |
| Mémoire | 830 | acteur | séquence aléatoire, **1 règle** | Longueur progresse, mais aucune décision des amis. |
| Rythme | 783 | acteur | **1 règle** | Quatre frappes ; aucune comparaison ou contretemps collectif. |
| Bluff | 839 | conteur + jurés | 17 amorces | Le plus social avec Positions ; il faut tester la qualité des histoires en vrai. |
| Bombe | 857 | tout le monde touche | **1 règle** | Relais réel, mais deux touches imposées ne sont pas des décisions. |

**Six jeux sur dix** n'offrent une action dans leur défi qu'à l'acteur : Culture G, Blind Test, Réflexe, Roulette, Mémoire et Rythme. Le pronostic commun donne des points, mais il se répète à chaque tour. Le modèle ne sait pas mesurer une blague, une rivalité, une surprise ou l'envie de revanche. [Jackbox distingue lui aussi plusieurs formes de participation du public](https://www.jackboxgames.com/blog/how-audience-play-along-differs-in-each-jackbox-game) ; nous retenons ici comme piste de conception des actions qui modifient réellement la manche, et non un simple bouton de présence. Son [journal de développement de 2026](https://www-origin.jackboxgames.com/blog/trivia-murder-party-3-dev-diary-6) décrit le recours à des groupes de test pour savoir ce qui est drôle, frustrant ou sensible à la latence. C'est une raison supplémentaire de ne pas confondre ces métriques avec un verdict de plaisir.

## Correctifs inclus dans 1.4.2

1. **Cartes sans répétition prématurée.** QCM, poses, motifs sonores, dessins et amorces de bluff utilisent maintenant un paquet mélangé par jeu, persistant dans la sauvegarde. Chaque élément sort une fois avant tout retour, et la première carte du nouveau paquet diffère de la dernière de l'ancien. La simulation trouve **zéro doublon avant épuisement** sur les 7 990 manches. Les jeux Libre peuvent toujours être redemandés de suite par le groupe ; c'est un choix explicite.
2. **Manches qui ne restent plus ouvertes indéfiniment.** La roulette a un chrono de choix de 10 s ; le gobelet choisi peut terminer son animation. Le dessin a 30 s pour créer puis 12 s pour deviner. Le passage du téléphone au devineur suspend son chrono jusqu'à sa confirmation. Le bonus de temps et le soutien peuvent encore allonger les chronos selon les règles existantes.
3. **Régression automatique.** Les tests couvrent épuisement des paquets après sauvegarde/restauration, absence de carte identique au raccord, délais des deux phases de dessin et fin de la roulette après révélation.

## Ce qui n'est pas encore validé

La simulation classe le **mode Vote local à quatre à six** comme risque de rythme, et les six défis solo comme risque de spectateur passif. Elle ne valide donc pas le critère « fun entre joueurs ». Les correctifs de 1.4.2 ciblent la répétition des contenus écrits et les manches sans fin ; ils ne transforment pas encore les jeux solo en défis collectifs ni la bombe en dilemme. Les pistes précises par jeu sont dans [la refonte des dix mini-jeux](REFONTE_DIX_MINI_JEUX.md).

Pour ouvrir une vraie validation : trois groupes indépendants de 2, 4 et 6 personnes, chacun sur un puis plusieurs téléphones, FR/EN mélangés, cinq manches de découverte puis dix manches libres. Observer sans guider : temps du premier plaisir, durée de préparation et d'attente par personne, incompréhensions, actions qui influencent le résultat, retours spontanés, choix volontaire de refaire un jeu, et vote final « lequel retire-t-on ? ». Rejouer avec les mêmes groupes après modification. Les séances réseau doivent enregistrer délai de réception et divergence de score, séparément du ressenti. La [spécification de refonte](REFONTE_DIX_MINI_JEUX.md) contient les critères de sortie de chaque jeu.

Reproduire l'audit : `./gradlew testDebugUnitTest --tests com.aperoroyale.PartyExperienceSimulationTest`. Le tableau complet des **30 cohortes** est imprimé dans le résultat du test (`app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.PartyExperienceSimulationTest.xml`).

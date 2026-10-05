# Laboratoire de soirées synthétiques — 5 octobre 2026

> La version 1.4.8 approfondit ce travail avec une [horloge virtuelle branchée sur les délais du moteur, trois graines et 5,4 millions de manches](LABO_EXTREME_2026.md). Les chiffres ci-dessous restent le résultat du premier modèle 1.4.7.

## Ce que le laboratoire peut conclure

Le test [`ExperienceRiskLabTest`](../app/src/test/java/com/aperoroyale/ExperienceRiskLabTest.java) fait tourner le vrai moteur de sélection, les dix défis, les votes, les paris, les jurys, les devinettes, les relais et l'avancement des tours. Il traverse **3 000 salles de 2 à 6 joueurs, FR/EN, sur un ou plusieurs appareils**, soit **36 000 manches** en modes Vote, Libre et Turbo. Les profils de vitesse restent stables pendant une soirée. Chaque action varie autour de ce profil ; les passages de téléphone, la préparation, les pauses et trois niveaux de délai réseau sont modélisés séparément. Les dix jeux reçoivent chacun plus de 3 500 manches.

Le modèle est un **test de risque et de sensibilité**, pas une mesure de plaisir ou une estimation de rétention. Les durées, les profils, les pauses et les délais réseau sont des hypothèses explicites dans le code. Les téléphones ne sont pas réellement connectés. La victoire aléatoire fait seulement avancer le moteur. Le simulateur n'entend pas les discussions, ne juge pas les dessins, les blagues, le son ou les graphismes, et ne prédit pas les téléchargements.

Les colonnes ont des sens distincts :

- `admin_pct` : part de sélection, transition, passage, pari, résultat et pause. Les choix des amis qui préparent un défi ne sont **pas** comptés comme administration.
- `p90_max_no_input_s` : au 90e percentile, plus longue période sans toucher l'écran pour **au moins un** joueur pendant une manche. Pendant une pose ou une histoire, cette personne peut néanmoins parler ou rire ; la mesure est un signal à examiner, pas une condamnation du jeu.
- `crew_only_pct` : part des manches où, pour les non-acteurs, l'unique action propre au défi précède l'épreuve centrale. Elle peut modifier le défi, mais ne garantit pas un échange social pendant l'action de l'acteur.
- `repeat_variant_pct` : proportion de variantes déjà vues dans la **même soirée**. Les paquets ne répètent pas avant épuisement ; le mode Libre peut néanmoins ramener vite un jeu favori.
- `repeat_concept_pct` : même calcul au niveau de l'idée jouée. Pour le Blind Test, les 24 variantes sont six motifs transposés dans quatre tonalités : retrouver le même motif dans une autre tonalité compte comme une répétition de concept.

## Résultats reproductibles

| Groupe | Mode | Moyenne modélisée | P90 manche | Administration | P90 plus longue période sans toucher l'écran |
| --- | --- | ---: | ---: | ---: | ---: |
| 2, un téléphone | Vote | 51,5 s | 73,2 s | 52,4 % | 49,0 s |
| 2, un téléphone | Turbo | 39,3 s | 56,3 s | 38,1 % | 49,4 s |
| 4, un téléphone | Vote | 74,8 s | 96,8 s | 57,5 % | 61,7 s |
| 4, un téléphone | Turbo | 49,9 s | 69,5 s | 36,8 % | 62,1 s |
| 6, un téléphone | Vote | **102,6 s** | **131,8 s** | **58,9 %** | 79,5 s |
| 6, un téléphone | Libre | 69,2 s | 94,5 s | 40,2 % | 86,5 s |
| 6, un téléphone | Turbo | 63,8 s | 86,3 s | 34,5 % | 78,7 s |
| 6, plusieurs téléphones | Vote | 47,7 s | 67,2 s | 42,7 % | 51,0 s |
| 6, plusieurs téléphones | Turbo | 40,7 s | 60,3 s | 30,9 % | 52,3 s |

**Lecture :** la cadence du Vote partagé à quatre à six dépend fortement de l'enchaînement des gestes et des passages physiques. Turbo réduit le temps administratif, mais ne change presque pas la plus longue attente individuelle. Une interface plus rapide peut donc aider sans remplacer une refonte des rôles dans les six épreuves centrées sur l'acteur. Les valeurs sont plus élevées que l'ancien modèle à durées fixes ([audit de 500 soirées](SIMULATION_EXPERIENCE_500.md)) parce que celui-ci varie les comportements et ajoute les passages et pauses. Aucun des deux modèles n'est calibré avec un groupe réel.

Sur appareils séparés, trois **hypothèses** de délai par action (0,12 s, 0,65 s et 2 s) donnent respectivement **40,0 s, 42,7 s et 48,5 s** de manche moyenne sur les modes mélangés. La différence montre la sensibilité du parcours ; elle ne représente ni la latence mesurée du Bluetooth/Wi-Fi/Internet ni les pertes de connexion.

## Lecture des dix défis

| Défi | Moyenne / P90 sans saisie | Ce que le code permet | Hypothèse prioritaire à éprouver |
| --- | ---: | --- | --- |
| Culture G | 46,7 / 42,8 s | Les amis répondent ; l'acteur peut suivre la majorité. | Observer si ce dilemme suscite discussion et revanche, ou si les amis attendent simplement sa réponse. |
| Positions | 59,0 / 72,4 s | Action physique, jury collectif. | La pose doit être compréhensible, drôle et réalisable dans un salon ou un bar ; mesurer les refus sans mettre la pression. |
| Blind Test | 51,5 / 47,4 s | Six motifs originaux en quatre tonalités, réponses privées ; 11,0 % de manches retrouvent un motif de la même soirée malgré 0 % de variante exacte répétée. | Tester si la reconnaissance de motifs inconnus intéresse vraiment le groupe ; une tonalité différente ne crée pas une nouvelle chanson. |
| Réflexe | 50,0 / 46,6 s | Les amis placent des zones du parcours. | Le défi central reste visuel et individuel ; comparer un duel court et un parcours construit par les amis. |
| Roulette | 39,5 / 40,3 s | Les protections des amis modifient le risque. | Rendre le bluff et la révélation lisibles ; vérifier que la décision est perçue comme juste. |
| Dessin | 71,7 / **91,8 s** | Tous les non-artistes devinent après le dessin. | L'attente pendant la création peut être une scène drôle ou un vide ; observer le groupe avant de raccourcir le chrono. |
| Mémoire | 52,1 / 49,6 s | Les amis construisent le début de la chaîne. | Tester une intervention pendant la restitution, sans rendre la tâche injuste. |
| Rythme | 48,2 / 44,0 s | Les amis choisissent des temps de la mesure. | Vérifier sur appareils physiques la synchronisation entre son, image et validation ; le simulateur ne peut pas la juger. |
| Bluff | 60,2 / 74,1 s | Histoire vraie/inventée et verdict du jury. | Qualité des amorces, liberté de ne rien révéler de personnel et force de la révélation. |
| Bombe | 48,7 / 50,9 s | Chaque porteur agit et choisit le suivant. | Mesurer si le relais crée de la tension ou se réduit à des tapotements obligés. |

**Priorités de conception déduites du modèle, non prouvées par des joueurs :** (1) Vote sur téléphone partagé à quatre à six ; (2) Dessin et Bluff, où le temps long peut être social ou vide ; (3) les six défis à contribution préalable, pour donner aux amis une action ou une réaction pendant l'épreuve centrale ; (4) richesse musicale et sonore, invisible aux simulations mécaniques. Le Blind Test et le Rythme demandent en particulier des essais audio réels.

## Votes absents : défaut corrigé

Avant cette itération, un seul vote non transmis laissait l'écran `VOTE` ouvert sans échéance. Le moteur donne désormais **18 s au premier vote**, puis **12 s après chaque vote reçu**. Les absents s'abstiennent, leurs voix ne sont pas inventées, et le jeu continue avec les votes présents. Le passage du téléphone, les menus et l'arrière-plan local suspendent cette échéance. Un choix de règle secrète sans réponse reçoit aussi une option par défaut après son délai. Le décompte est visible à l'écran.

Un second test simule **1 000 scrutins pour chaque combinaison de 2 à 6 joueurs et de 2 %, 10 % ou 30 % de votes manquants**. Ces taux sont des scénarios de panne, **pas des taux observés**. À six, le scénario à 2 % entraîne 12,4 % de scrutins avec au moins une abstention dans cette graine ; les 1 000 continuent. Les tests vérifient la sauvegarde/restauration et le choix automatique d'une règle secrète non choisie.

## D'où viennent les critères

- [Ryan, Rigby et Przybylski (2006)](https://selfdeterminationtheory.org/SDT/documents/2006_RyanRigbyPrzybylski_MandE.pdf) relient autonomie, compétence et lien social au plaisir et à l'intention de rejouer dans leurs études. Transposer ces dimensions à une soirée d'apéro est une **hypothèse de conception**, pas une mesure de nos joueurs.
- [GameFlow, Sweetser et Wyeth (2005)](https://doi.org/10.1145/1077246.1077253) sert de grille pour clarté, contrôle, retour des actions et interaction sociale. Il ne fournit pas de seuil universel en secondes pour notre jeu.
- [Exploring Gameplay With AI Agents](https://arxiv.org/abs/1811.06962) montre l'intérêt d'agents automatisés pour explorer l'équilibre, les récompenses sans effet et les séquences d'actions. Nous suivons cette logique pour éliminer des risques, sans appeler le résultat « plaisir simulé ».
- La [revue de 263 travaux sur le jeu social](https://doi.org/10.1016/j.chb.2023.107851) souligne l'importance du groupe et du contexte, et les limites des évaluations éloignées de la situation réelle.
- Les avis visibles sur les fiches [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup), [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych), [Undercover](https://play.google.com/store/apps/details?id=com.yanstarstudio.joss.undercover) et [Outsmarted!](https://play.google.com/store/apps/details?id=com.qplay.outsmarted) signalent notamment commandes, répétition, reconnexion, fluidité des menus, salons et justice perçue. Nous les utilisons comme **cas de panne à reproduire**, pas comme échantillon représentatif ni preuve de causalité commerciale.

| Avis public observé | Cas de test transposé à Apéro Royale |
| --- | --- |
| [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup) : un avis décrit des commandes par mouvement peu fiables ; un autre signale des mots qui reviennent vite. | Une action doit rester possible par gros bouton ; les contenus et les **concepts**, pas seulement les identifiants de variantes, doivent être suivis. |
| [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych) : des avis rapportent lenteur, interruptions et retour difficile dans la partie après changement d'application. | Tester veille, bascule vers la musique, reconnexion et sauvegarde sans perdre une manche ni doubler les points. |
| [Undercover](https://play.google.com/store/apps/details?id=com.yanstarstudio.joss.undercover) : un avis apprécie mots et rôles faciles à jouer ; d'autres décrivent des salons en ligne laborieux. | Mesurer la qualité des consignes sociales et l'entrée dans une salle réelle, pas seulement la capacité du moteur à transporter un vote. |
| [Outsmarted!](https://play.google.com/store/apps/details?id=com.qplay.outsmarted) : des avis parlent de trop de clics, de téléphones déconnectés et d'un hasard perçu comme injuste. | Mesurer le temps administratif, une perte de client au mauvais moment et la lisibilité des risques avant révélation. |

## Prochaine calibration

Pour rendre le modèle prédictif, chronométrer quelques soirées FR/EN sans guider les joueurs : durée de chaque étape, passages, interactions orales, abandon, choix libre de refaire un défi et retour volontaire à une autre soirée. Conserver les événements anonymes au niveau de la table plutôt qu'enregistrer voix ou conversations. Ajuster les distributions du laboratoire sur ces observations, puis comparer deux variantes sur des groupes différents. Un petit nombre de groupes peut éliminer les hypothèses de temps les plus fausses ; seule une cohorte réelle peut établir l'envie de rejouer.

Reproduire : `./gradlew testDebugUnitTest --tests com.aperoroyale.ExperienceRiskLabTest --offline`. Les 30 cohortes et les dix défis sont imprimés dans `app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.ExperienceRiskLabTest.xml`.

# La table est la console

## Stress test social des succès Android et direction artistique · 5 octobre 2026

![Concept de la table nocturne : les regards et la performance avant l'écran](concepts/night-table-2026.png)

*Planche d'intention originale, pas une capture du jeu livré.*

Apéro Royale a réussi des tests de moteur et des milliers de soirées synthétiques. Ce travail vérifie des transitions, de la sauvegarde et des délais **dans les conditions du modèle**. Il ne peut pas dire si une réplique est drôle, si un silence est complice, si un refus pique trop, ni si quelqu'un réclame spontanément une revanche. Cette étude change d'unité d'observation : **la scène entre les personnes**. L'application distribue les rôles, garde les secrets et orchestre les révélations ; les amis font le spectacle.

## Méthode et limites

J'ai confronté les captures et les règles de la 1.5.0 à six fiches Google Play de jeux de soirée largement installés, à des avis publics précis et à des travaux de recherche sur le jeu mobile côte à côte. Les badges de téléchargements indiquent une diffusion, pas une qualité ou une causalité. Les avis mis en avant par le Play Store ne sont pas un échantillon représentatif. Je n'ai pas installé les concurrents, observé une soirée réelle, ni mesuré leur rétention. Les scènes ci-dessous sont des **épreuves de conception** : leur bifurcation humaine est intentionnelle, pas une probabilité inventée.

| Jeu Android, fiche consultée le 5 octobre 2026 | Ce qui met les amis en mouvement | Friction exprimée dans les avis visibles | Épreuve posée à Apéro Royale |
| --- | --- | --- | --- |
| [Picolo](https://play.google.com/store/apps/details?id=com.picolo.android), 5 M+ téléchargements | On entre des noms et une instruction lance la conversation ; le téléphone peut ensuite s'effacer. | Des joueurs regrettent des abonnements jugés disproportionnés pour un usage occasionnel. | Peut-on lancer une scène mémorable sans faire passer tout le monde par un tunnel de votes, mises et confirmations ? |
| [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup), 10 M+ | Le téléphone devient un accessoire physique ; le groupe crie, mime et improvise autour d'une personne. | Un avis demande des commandes tactiles de secours pour le gyroscope ; d'autres décrivent des cartes répétées et la perte de contenus achetés. | Chaque jeu doit-il se toucher autant, ou l'écran doit-il parfois n'être qu'un signe tenu à bout de bras ? |
| [Undercover](https://play.google.com/store/apps/details?id=com.yanstarstudio.joss.undercover), 5 M+ | Un secret simple produit des mensonges, une discussion et un retournement au dévoilement. | Les mots et les rôles sont appréciés ; un avis récent signale une bascule confuse entre en ligne et hors ligne, d'autres limites de contenu. | Notre bluff et nos secrets produisent-ils une accusation racontable, ou seulement un verdict numérique ? |
| [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych), 5 M+ | Les joueurs fabriquent les mauvaises réponses et se piègent eux-mêmes. | Un avis réclame du chat et des statistiques sur *qui a dupé qui* ; d'autres signalent pubs, lenteur, plantages et perte de partie après changement d'app. | Montrons-nous les auteurs du chaos et survivons-nous à une interruption au moment du rire ? |
| [Truth or Dare: Spin the Bottle](https://play.google.com/store/apps/details?id=com.therisingtechie.truthordare), 10 M+ | Plusieurs portes d'entrée : tour de rôle, hasard, roue, cartes personnalisées, jeu hors ligne. | Des avis récents apprécient une interface plus rapide et la variété des modes. Cela reste un retour sélectionné, pas une mesure de fidélité. | La liberté de choisir l'ambiance sert-elle le groupe, ou ajoute-t-elle encore un menu avant de jouer ? |
| [Truth or Dare de Snash](https://play.google.com/store/apps/details?id=snash.app.truthordare), 1 M+ | Les cartes personnelles peuvent transformer une consigne générique en blague interne. | Des avis reprochent des défis vagues, sans condition de fin, et une catégorisation homme/femme rigide qui se trompe de personne. | Nos consignes ont-elles une fin claire sans enfermer les joueurs dans un rôle ou un type de personne ? |

Les études sur le [jeu mobile côte à côte](https://pure.au.dk/portal/en/publications/designing-for-social-play-in-co-located-mobile-games/) décrivent l'intérêt des corps, du lieu et des informations asymétriques. Une [étude des conversations au pub](https://nottingham-repository.worktribe.com/output/775042/using-mobile-phones-in-pub-talk) montre que téléphone et conversation s'entrelacent de façon nuancée. J'en déduis un principe de design pour Apéro Royale : **une action à l'écran doit donner une raison de relever les yeux**. Ce principe est une interprétation, pas un résultat mesuré sur notre jeu.

## Stress test de la 1.5.0 : ce que le système sait, ce que la soirée peut faire

| Scène sous pression | Comportements humains également plausibles | Ce que fait la version actuelle | Ce qu'il faut essayer |
| --- | --- | --- | --- |
| **Deux amis, cinq minutes avant de sortir.** L'un veut rire tout de suite, l'autre déteste perdre. | Ils savourent une revanche ; ou ils abandonnent au premier passage de téléphone. | Profils retrouvés, mais sélection du mode, jeu, mise, éventuelles contributions puis relais restent séquentiels. | Une ouverture « lance une scène » qui choisit un jeu adapté à deux, sans renoncer aux réglages ensuite. |
| **Six amis sur un seul téléphone.** La table parle encore pendant qu'une décision privée circule. | Le relais devient une plaisanterie ; ou l'app monopolise la parole et les mains. | Le moteur suspend certains chronos pendant les passages ; les choix privés restent nombreux. | Distinguer les secrets indispensables des votes que la table peut faire à voix haute. |
| **Quatre téléphones, deux conversations à la fois.** | Une personne raconte son pari ; ou chacun fixe son écran et rate la blague. | Les choix sont synchronisés et les joueurs peuvent agir sur leur appareil. | Un seul moment de révélation commun, lisible à distance, avec un temps volontaire pour réagir avant la suite. |
| **Une amie arrive en plein tour et se fait refuser pour plaisanter.** | Elle rit et retente ; ou elle se sent vraiment exclue alors que personne ne le voulait. | Spectatrice, trois secrets locaux, scrutin majoritaire et gorgée virtuelle en cas de refus. | Transformer le refus en *incident de comédie* réversible : la table peut inviter quand même, l'intéressée peut refuser le défi, aucune pénalité en boucle. |
| **Quelqu'un ne veut pas raconter sa vraie vie ni faire une pose.** | Il improvise une fiction brillante ; ou il décroche s'il faut justifier son refus. | Le jury arbitre la pose ou le bluff ; certains défis demandent une performance. | Prévoir explicitement « fiction », « fais-le en duo », « passe » et une sortie élégante sans commentaire moralisateur. |
| **Une question de Culture G est contestée.** | Le débat devient le meilleur moment ; ou la table accuse le jeu d'être injuste. | Quatre réponses, choix de la salle puis réponse de l'acteur. | Révéler une explication courte et la source ; autoriser une contestation légère sans bloquer la soirée. |
| **La musique couvre les voix ou quelqu'un ouvre Deezer.** | Les amis chantent par-dessus ; ou le jeu et la radio se battent pour l'attention. | Son original réglable et liens vers plusieurs services ; pas de contrôle natif de leur lecture. | Un bouton de silence immédiat et des sons de jeu conçus comme ponctuation, avec pauses lisibles. |
| **Le réseau saute au moment du dévoilement.** | Le groupe rit quand même ; ou il ne sait plus qui a gagné et se décourage. | L'hôte garde la partie ; la reprise et certains relais sont testés, mais pas tous sur des téléphones physiques. | Un résultat idempotent, une reconnexion au bon rôle et la possibilité de raconter le verdict à voix haute. |
| **Une blague interne vaut plus qu'un classement.** | La table cite la phrase toute la nuit ; ou le score absorbe un moment drôle. | Points, gorgées et leaderboard persistent ; les captures sont surtout des écrans de défi. | Capturer une *anecdote choisie* en fin de manche, puis une carte souvenir locale sans photo ou pseudo imposé. |

**Constat visuel direct.** Les [captures de Culture G](screenshots/games/trivia.png), [Positions](screenshots/games/poses.png), [Dessin](screenshots/games/drawing.png) et [Bombe](screenshots/games/bomb.png) ont des décors et sprites distincts, mais partagent un grand cadre, un titre, un chrono et une action dominante. Cette cohérence aide à se repérer. Elle atténue aussi la personnalité du moment : le dessin ressemble encore à une étape du système, pas à une galerie improvisée ; la bombe ressemble encore à un gros bouton, pas à un objet que l'on redoute de recevoir. C'est une lecture artistique des images, pas une mesure d'appréciation.

## Dix scènes, dix façons de faire exister la table

Ces pistes décrivent **une mise en scène à prototyper**, pas des fonctions déjà livrées. Chaque jeu gagne une entrée, un geste social et une révélation propre ; sa réussite ne se résume pas au résultat du moteur.

| Jeu | Nouveau moment à chercher | Réaction de la table à rendre visible |
| --- | --- | --- |
| Culture G | « Qui veux-tu croire ? » L'acteur choisit une personne avant de connaître sa réponse. | La chaîne des alliances se révèle avant la bonne réponse ; les amis défendent leur mauvaise intuition. |
| Positions | Une affiche de pose absurde et un complice volontaire plutôt qu'un corps seul face à un jury. | Le groupe nomme la pose et peut applaudir, pardonner ou réclamer un rappel. |
| Blind Test | Des motifs originaux deviennent des fausses pubs, génériques ou jingles de fiction, avec indices écrits par les amis. | L'histoire inventée autour du son compte autant que le nom de la réponse. |
| Réflexe | Un ami tient le rôle de faussaire : il annonce le piège, puis l'acteur tente de le déjouer. | La confrontation et la presque-erreur sont plus intéressantes que quelques millisecondes de classement. |
| Roulette | Chaque verre reçoit une promesse, une menace ou une protection donnée par quelqu'un. | La révélation montre qui a protégé ou trahi qui, sans masquer le hasard. |
| Dessin | Les amis écrivent de faux titres pendant que l'artiste dessine ; la galerie révèle les signatures. | Le dessin raté devient souvent la meilleure affiche de la soirée. |
| Mémoire | Une chaîne d'objets laissés par les amis, pas une simple suite de touches. | Au raté, la table revoit la chaîne et reconnaît la contribution de chacun. |
| Rythme | Un appel-réponse de table, où un joueur lance un motif et un autre le contredit. | Les décalages volontaires deviennent une improvisation, sans que la latence réseau décide du vainqueur. |
| Bluff | Un dossier d'alibi que l'acteur raconte ou invente ; le jury pose une question courte. | On dévoile *qui a cru qui*, puis on laisse quelques secondes au conteur pour se défendre. |
| Bombe | Un accessoire qui circule, avec un choix de destinataire et une dette comique. | La personne qui reçoit l'objet réagit avant l'écran suivant ; le fil final appartient à toute la table. |

## Direction artistique : « la nuit est à vous »

La planche ci-dessus donne la hiérarchie : **visages → gestes → objet → interface**. On peut garder l'élégance nocturne de la 1.5.0 sans transformer l'app en casino miniature. La ville et l'arcade sont un décor de théâtre pour des adultes ; le chat pixel est un trouble-fête discret. Une même soirée peut passer du bistro feutré au plateau télé bricolé puis à la salle d'interrogatoire, selon le jeu. Les sprites doivent avoir une fonction dramatique : hésiter, provoquer, protéger, se trahir, célébrer.

- **Palette et matière.** Aubergine profonde, laiton usé, ambre de lampe, pétrole et corail fané ; papier de sous-verre, émail rayé, grain d'affiche sérigraphiée. Une couleur vive signale un moment, elle ne peint pas toute la page.
- **Typographie.** Une voix affichée courte et insolente pour les moments de scène ; une police très lisible pour les consignes. Les longs textes sont dits par la table ou coupés en actes, pas plaqués sur un décor détaillé.
- **Mouvement.** Attente respirante, puis une rupture franche à la révélation. Les confettis appartiennent à une vraie victoire collective ; une simple touche n'en déclenche pas une pluie.
- **Son.** Ambiance facultative et basse ; motifs courts, chaleur analogique, petits silences avant le verdict. Le son laisse les voix passer et les effets gardent une identité par jeu.
- **UI.** Les commandes existent quand elles servent. Pendant une pose ou un bluff, le téléphone s'efface ; pendant un pari privé, il protège le secret ; au dévoilement, il devient une affiche qu'on peut montrer à tous.
- **Tonalité.** Taquiner sans humilier. La mauvaise foi est jouable, la porte de sortie est toujours disponible. Une gorgée est un enjeu virtuel que la table peut remplacer par un défi ou rien, pas une mécanique d'escalade.

## Prochaine tranche de création

Produire une petite séquence complète plutôt que dix couches de chrome identiques : **Bluff → question du jury → dévoilement de ceux qui ont cru → revanche**, puis **Dessin → faux titres → galerie attribuée**. Dans chaque prototype, l'art et le son doivent servir la conversation, et l'écran de résultat doit raconter une histoire qu'une personne peut répéter sans regarder le téléphone. Revoir ensuite Culture G et l'arrivée tardive avec la même logique. Les autres jeux héritent de ce langage de scène, pas d'un gabarit d'interface.

Le test suivant est interprétatif : observer plusieurs tables sans leur souffler les règles ; noter le moment où quelqu'un reprend la parole, improvise une variante, protège un ami, demande à rejouer ou range son téléphone. Conserver aussi les scènes ratées et les avis contradictoires. Aucun tableau de scores synthétiques ne doit convertir ces comportements en une fausse « probabilité de fun ».

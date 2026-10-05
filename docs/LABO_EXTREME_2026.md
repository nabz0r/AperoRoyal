# Laboratoire de soirée à horloge virtuelle — 5 octobre 2026

## Ce qui a réellement été testé

Le test [`ExtremePartySimulationTest`](../app/src/test/java/com/aperoroyale/ExtremePartySimulationTest.java) pilote **le vrai `GameEngine` et ses vrais délais** avec une horloge virtuelle. Trois graines indépendantes ont chacune parcouru **100 000 soirées de 18 manches**, soit **300 000 soirées et 5,4 millions de manches** au total. Chaque graine couvre les groupes de 2 à 6 personnes, FR/EN alternés, un téléphone, deux téléphones ou un appareil par personne, les modes Vote/Libre/Turbo et quatre environnements. Les dix jeux, la sélection, les contributions, le dessin, les jurys, le rythme, les relais, les règles secrètes et des sauvegardes suivies d'une vraie continuation sont exercés.

Les graines sont `11674260475909`, `20261005` et `8675309`. Les [180 cohortes de la première graine](data/extreme-100k-cohorts.csv) et les [résultats par jeu](data/extreme-100k-games.csv) sont conservés en CSV. L'exécution longue prend environ 4,5 secondes de calcul de test sur cette machine **sans rendu, son ni transport réseau** ; cette vitesse ne dit rien de la vitesse de l'application sur téléphone.

L'ancien laboratoire de 3 000 soirées utilisait des durées séparées de l'horloge du moteur. Ici, le moteur reçoit l'heure virtuelle et peut donc lui-même fermer un vote, couper un défi, enregistrer un score, puis reprendre depuis une sauvegarde. Une action arrivée après une échéance n'est plus comptée comme une contribution réussie.

## Hypothèses, pas données de joueurs

| Variable simulée | Plage ou règle | Pourquoi elle existe |
| --- | --- | --- |
| Vitesse stable d'une table | Facteur de 0,80 à 1,45 | Les gestes de tous les joueurs d'une même soirée sont corrélés. |
| Vitesse stable d'une personne | Distribution lognormale, bornée à 0,55–2,40 fois le temps de base | Un joueur hésitant le reste souvent plusieurs manches ; chaque geste varie aussi. |
| Conversation | Temps d'action ×1,35 ; interruptions occasionnelles de 7–30 s | Éviter de supposer des clics indépendants et instantanés. |
| Soirée interrompue | Temps d'action ×1,55 ; 5,5 % de gestes avec une interruption supplémentaire | Explorer une soirée bruyante sans prétendre que ce taux a été observé. |
| Réponse absente | Bases de 0,6 %, 1,5 %, 4,5 % ou 2,5 % selon scénario ; une absence récente augmente la suivante | Tester les réponses manquantes corrélées et leur résolution. |
| Plusieurs appareils | Délai lognormal ; scénario perturbé avec pointes de 3–15 s et coupures corrélées par manche | Explorer des enveloppes de délai et de perte ; **aucun réseau réel n'a été mesuré**. |
| Un téléphone, joueur absent | Après 20 s d'attente, un ami utilise « Absent ? Passer son tour » | Représenter la nouvelle action visible plutôt qu'une disparition magique du joueur. |

Ces nombres sont des **paramètres d'exploration choisis**, non des estimations scientifiques de la population des joueurs. Les trois graines mesurent surtout la stabilité du calcul sous *ces mêmes hypothèses* ; elles ne corrigent pas une hypothèse fausse. Cette séparation suit les principes de [vérification, validation externe et analyse de sensibilité décrits par les National Academies](https://www.nationalacademies.org/read/6037/chapter/11). Les [agents de test de jeux](https://arxiv.org/abs/1811.06962) sont utiles pour explorer des parcours et détecter des trous mécaniques, mais ils ne ressentent pas le plaisir d'une soirée.

## Résultats du modèle final

Les plages ci-dessous sont les minimum et maximum des trois graines. Une ligne représente un même nombre de joueurs, une configuration, un mode et un environnement. Le « P90 sans saisie » est la plus longue période sans toucher l'écran pour au moins une personne, au 90e percentile ; parler ou rire pendant cette période n'est pas enregistré.

| Soirée | Manche moyenne | P90 manche | P90 attente individuelle | Administration | Contribution incomplète | Défi coupé par délai |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 2, un téléphone, Vote, calme | 46,4–46,8 s | 66–67 s | 48–49 s | 53,5–54,1 % | 0,6 % | 9,7–11,2 % |
| 6, un téléphone, Vote, calme | 94,5–95,0 s | 116–117 s | 84 s | 53,3 % | 2,8–3,0 % | 20,5–21,0 % |
| 6, un téléphone, Vote, interrompu | 117,4–118,0 s | 150–151 s | 133–134 s | 52,2–52,5 % | 29,0–30,6 % | 52,4–53,2 % |
| 6, un téléphone, Turbo, calme | 57,1–57,5 s | 75–76 s | 72 s | 26,0–26,1 % | 10,1–10,7 % | 26,7–28,0 % |
| 6, un téléphone, Turbo, interrompu | 66,7–67,1 s | 93–94 s | 91–92 s | 26,6–26,7 % | 42,6–44,2 % | 59,5–61,2 % |
| 6, chacun son téléphone, Vote, calme | 42,1–42,2 s | 60–61 s | 50–51 s | 45,7–45,8 % | 1,4–2,0 % | 18,2–19,1 % |
| 6, chacun son téléphone, Vote, liaison perturbée | 61,1–61,3 s | 83–84 s | 77–78 s | 46,5–46,7 % | 20,8–21,4 % | 42,9–43,2 % |

Les « défis coupés » sont des résultats du **modèle de comportement choisi**, et non des taux d'échec observés chez des humains. L'examen jeu par jeu indique encore des zones à tester avec de vraies tables : [Positions, Dessin et Bluff](data/extreme-100k-games.csv) dépendent beaucoup du temps que les amis prennent pour créer et raconter ; [Bombe](data/extreme-100k-games.csv) dépend du passage physique et de la synchronisation. Le simulateur ne peut pas dire si cette durée provoque un fou rire ou un ennui.

## Corrections déclenchées par le test

**Passage du téléphone.** Une personne absente pouvait laisser le téléphone sur « C'est moi » sans fin : les délais étaient suspendus pendant ce passage. Le nouvel écran possède « Absent ? Passer son tour ». Un vote sauté devient une abstention ; une contribution de groupe sautée ne donne aucun point ; un jury ou un dessin se résout avec les réponses effectivement données. La bombe abandonnée termine le défi. [Capture réelle sur émulateur](screenshots/handoff-skip.png).

**Contribution de groupe.** Avant correction, à six sur un téléphone en Turbo calme, le modèle n'obtenait pas toutes les contributions dans 60,3 % des manches concernées. Le délai était fixe à cinq secondes. Il est maintenant de huit secondes en Turbo et douze secondes dans les autres modes, **renouvelé après chaque personne**. Avec la même graine et les mêmes hypothèses, la contribution incomplète tombe à 9,7 % en Turbo calme ; la manche moyenne passe de 42,3 à 53,7 s. En Vote calme, elle passe de 54,3 à 2,7 %, et la manche de 87,2 à 92,1 s. Les [CSV avant](data/extreme-100k-before-crew-timer.csv) et [après ce seul changement](data/extreme-100k-after-crew-timer.csv) conservent la comparaison. Leur ancienne colonne `p90_global_no_touch_s` mesurait l'absence de saisie *sur tous les écrans à la fois* ; la colonne `p90_no_touch_s` du modèle final mesure désormais l'attente individuelle et ne doit pas lui être comparée. Cette décision privilégie la participation à quelques secondes gagnées.

**Temps de création sociale.** Positions et Bluff accordent désormais 40 secondes pour agir ou raconter ; Dessin accorde 40 secondes pour créer avant les devinettes. Sur la première graine, avec ces changements et le passage de bombe correctement suspendu, les pertes par délai de Positions passent de 67,6 à 43,9 %, de Dessin de 65,7 à 47,2 %, de Bluff de 67,7 à 43,8 %, et de Bombe de 63,5 à 53,2 %. Ces écarts sont des **tests de sensibilité du modèle**. Ils justifient un essai réel ; ils ne prouvent pas que 40 secondes est le meilleur réglage.

## Ce qui manque pour parler de réalisme prédictif

Le modèle ne voit ni rires, ni gêne, ni conversation, ni refus d'un défi, ni fatigue, ni plaisir musical. Il ne mesure pas le temps d'affichage, le toucher réel, la synchronisation audio, le Wi-Fi, le Bluetooth, le relais Internet ou une reconnexion entre deux téléphones physiques. Le score de victoire des agents est une règle de test, pas une prédiction de réussite humaine. Les captures sur émulateur vérifient le parcours UI ; elles ne calibrent pas les vitesses des joueurs.

L'étape empirique est de chronométrer des soirées consenties, FR/EN et à différents nombres de téléphones, avec seulement des événements de phase anonymes : début/fin de sélection, passage, contribution, défi, résultat, reprise et abandon. Une courte évaluation après la partie doit distinguer fluidité, sentiment de jouer ensemble et envie de rejouer. Les observations serviront à ajuster les distributions et à comparer ensuite des variantes sur des groupes distincts. Sans cela, annoncer un « taux de viralité » ou une durée réelle serait inventé.

## Reproduire

```sh
APERO_SIM_PARTIES=100000 APERO_SIM_SEED=11674260475909 ./gradlew testDebugUnitTest --tests com.aperoroyale.ExtremePartySimulationTest --rerun-tasks
python3 tools/export_extreme_report.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.ExtremePartySimulationTest.xml docs/data/extreme-100k
```

Le test court en CI utilise 3 600 soirées. Le code accepte jusqu'à 100 000 soirées par exécution. Les données publiées viennent de la graine ci-dessus ; les deux autres graines ont servi à vérifier les plages du tableau. Les paramètres de temps et de comportement se trouvent en tête du test et dans `Session` ; changer ces hypothèses est précisément l'analyse de sensibilité à mener après mesure humaine.

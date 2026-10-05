# Laboratoire de joueurs virtuels — 5 octobre 2026

Pour la lecture sociale et artistique de ces chiffres, voir le [stress test des succès Android et des scènes autour de la table](STRESS_TEST_SOCIAL_2026.md).

## Ce que font ces joueurs

[`VirtualPeopleSimulationTest`](../app/src/test/java/com/aperoroyale/VirtualPeopleSimulationTest.java) fait agir **des personnes artificielles avec une mémoire** dans le vrai `GameEngine`. Chacune possède des goûts et aptitudes propres aux dix défis, un rythme stable, de la patience, une tolérance à la mise, une confiance, une énergie, une humeur et une affinité avec chaque ami. Ces états évoluent d'une manche à l'autre. Une personne peut préférer un défi inédit, voter pour celui que son ami actif aime, réduire sa mise après des défaites, juger une histoire selon sa confiance dans le conteur ou répondre moins souvent après une soirée frustrante. Un test contrôlé vérifie que la mémoire et l'amitié changent effectivement ses choix.

Ce sont des **agents procéduraux**, pas des personnages pilotés par un grand modèle de langage, ni des copies de vrais invités. Ils n'improvisent pas de conversation et ne ressentent ni humour, ni gêne, ni plaisir. Les coefficients de comportement sont choisis pour explorer des risques de conception et **ne sont pas calibrés sur des observations humaines**. L'architecture « mémoire puis choix » s'inspire des [Generative Agents](https://arxiv.org/abs/2304.03442), tandis que l'usage de styles de jeu pour tester automatiquement des parcours suit les travaux sur les [personas procéduraux](https://www.antoniosliapis.com/papers/automated_playtesting_with_procedural_personas_through_mcts_with_evolved_heuristics.pdf). Notre implémentation est volontairement plus simple et ne reprend pas leurs résultats comme preuves de réalisme.

## Expérience reproductible

Trois graines (`20261005`, `8675309`, `11674260475909`) × **30 000 soirées de 24 manches**, soit **90 000 soirées et 2,16 millions de manches par version de chrono**. Une comparaison rejoue les anciens délais du Blind Test et de Mémoire dans le moteur courant ; au total, 180 000 soirées et 4,32 millions de manches ont été exécutées. Chaque graine croise 2–6 joueurs, langues FR/EN alternées, un téléphone, deux téléphones ou un appareil par personne, trois modes et quatre ambiances fictives : amis, mixte, compétitive et distraite. Les dix jeux et leurs vraies transitions, contributions, jurys, dessin, paris, scores et délais sont exercés. Les gestes de plusieurs appareils peuvent arriver en parallèle ; un appareil partagé sérialise les passages. Aucun réseau, écran ou son réel n'est mesuré.

La comparaison garde les mêmes graines et les mêmes cohortes. Les décisions peuvent ensuite diverger, car une victoire ou un délai modifie l'humeur puis les votes futurs. Les écarts ne sont donc pas des effets causaux estimés sur des humains. Les tableaux complets sont dans [`docs/data`](data/) : fichiers `virtual-people-legacy-*-{games,players-mode,topology-group}.csv` et `virtual-people-v149-*-{games,players-mode,topology-group}.csv`.

| Signal du modèle | Ancien chrono | 1.4.9 | Lecture prudente |
| --- | ---: | ---: | --- |
| Blind Test coupé par son délai | 30,0–30,5 % | 14,4–14,6 % | +5 s donne davantage de place à l'écoute et à la réponse. |
| Mémoire coupée par son délai | 20,9–21,3 % | 12,1–12,4 % | +4 s réduit les fins forcées. |
| Soirée d'amis, un téléphone : défi coupé | 5,8–5,9 % | 4,0 % | Le gain global reste modeste. |
| Table distraite, un téléphone : défi coupé | 48,6–49,1 % | 44,5–44,9 % | La soirée difficile reste un risque majeur. |
| Six joueurs, mode Vote : manche moyenne | 41,7–41,9 s | 41,8–42,0 s | Modèle rapide, sans temps réel de conversation. |
| Six joueurs, mode Turbo : défi coupé | 19,8–19,9 % | 16,4 % | Le mode rapide garde un compromis de temps. |

Les seuils « énergie basse » (<0,4) et « humeur basse avec énergie <0,6 » sont **des signaux internes arbitraires du modèle**, pas des probabilités d'abandon ou d'envie de rejouer. Par exemple, une table distraite sur un téléphone reste autour de 85,5–86,2 % d'observations « humeur et énergie basses » après l'ajustement ; cette valeur indique surtout que nos hypothèses de fatigue et de retard se renforcent entre elles. Elle ne doit pas être citée comme taux de rétention humaine.

## Ce que le laboratoire change, et ce qu'il ne sait pas

Le Blind Test passe de 18 à **23 secondes** et Mémoire de 22 à **26 secondes** avant les modificateurs de mode et de bonus. Le même modèle montre une baisse des défis coupés sans allonger sensiblement la manche moyenne dans ses scénarios. Dessin reste autour de 17,8–17,9 % de fins par délai ; son problème dépend aussi du temps de création, du passage du téléphone et des devinettes. Nous ne modifions pas tous les chronos d'un coup sur cette seule base.

Les agents ne voient pas le rendu, n'entendent pas les motifs ni les effets, n'ont pas de conversation libre, ne disposent d'aucune perception du consentement ou de la consommation et ne peuvent pas juger si une pose fait rire. Une « action réseau » n'utilise pas réellement le Wi-Fi, Bluetooth ou Internet. Les durées de réflexion et les probabilités de réponse sont des scénarios écrits dans le test. Trois graines vérifient la stabilité du calcul **sous ces hypothèses** ; elles ne corrigent pas une hypothèse inexacte. Pour passer du stress test à une prévision, il faut des soirées consenties, des temps de phase anonymes et des retours séparés sur fluidité, participation, confort et envie de rejouer. C'est la distinction entre vérification interne et validation externe décrite par les [National Academies](https://www.nationalacademies.org/read/6037/chapter/11).

## Reproduire

```sh
APERO_PEOPLE_PARTIES=30000 APERO_PEOPLE_SEED=20261005 APERO_PEOPLE_OLD_TIMERS=1 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks
python3 tools/export_virtual_people.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.VirtualPeopleSimulationTest.xml docs/data/virtual-people-legacy-20261005
APERO_PEOPLE_PARTIES=30000 APERO_PEOPLE_SEED=20261005 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks
python3 tools/export_virtual_people.py app/build/test-results/testDebugUnitTest/TEST-com.aperoroyale.VirtualPeopleSimulationTest.xml docs/data/virtual-people-v149-20261005
```

Sans variable, le test court de la CI joue 3 600 soirées. `APERO_PEOPLE_PARTIES` accepte 180 à 100 000 ; `APERO_PEOPLE_OLD_TIMERS=1` restaure uniquement les deux anciens délais dans la simulation. Les CSV versionnés rendent les paramètres, cohortes et sorties auditables.

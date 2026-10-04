# Simulation de 500 soirées — Apéro Royale 1.4.1

Le test reproductible [`PartySimulationTest`](../app/src/test/java/com/aperoroyale/PartySimulationTest.java) exécute le **vrai moteur de jeu** avec 500 graines différentes, de deux à six joueurs, en modes Vote, Libre et Turbo. Chaque soirée dure de 12 à 20 manches. Le test alterne acteurs, mises, pronostics, votes, jury, roulette, réflexes, bombe, règles secrètes et signalements de règle. Il sauvegarde et restaure régulièrement l'état en JSON, comme lors d'une reprise ou d'une synchronisation, puis vérifie tours, scores, compteurs et secrets des instantanés invités.

## Résultat après corrections

| Mesure | Résultat |
| --- | ---: |
| Soirées simulées | 500 |
| Manches terminées | 7 990 |
| Victoires / défaites | 4 041 / 3 949 |
| Pronostics expirés | 1 012 |
| Jurys arrivés au délai | 396 |
| Règles secrètes choisies | 498, dont 166 en Turbo |
| Signalements soumis au vote | 436 |
| Restaurations JSON contrôlées | 40 190 |

Chaque mini-jeu apparaît au moins une fois. Aucun tour perdu, score négatif, victoire au-delà du nombre de manches, répétition immédiate en Vote/Turbo ou secret divulgué dans les instantanés vérifiés. Les suites de décisions et les tirages du test sont semés ; deux exécutions complètes ont produit les mêmes nombres le 4 octobre 2026. Il ne s'agit pas de mesures de parties humaines.

## Problèmes trouvés et corrigés

1. **Turbo sautait le choix d'une règle secrète.** Après un déblocage, `startSelection()` lançait directement le défi suivant : le propriétaire du secret n'avait plus d'écran où choisir la règle. Le moteur passe maintenant par `RULE_PICK` et n'enchaîne qu'après le choix. L'écran est utilisable sur le téléphone du propriétaire ou depuis son téléphone invité.
2. **Le délai du jury ignorait ses votes dans `GameEngine.checkTimeout()`.** Un verdict positif devenait une défaite lorsque cette méthode traitait l'expiration. Elle utilise maintenant `juryVerdict()` pour les défis de pose et de bluff. Le chemin d'horloge de l'activité appliquait déjà ce verdict ; ce correctif garde le moteur cohérent avec lui.

Les deux cas ont des tests ciblés dans [`GameEngineRulesTest`](../app/src/test/java/com/aperoroyale/GameEngineRulesTest.java). Le test des 500 soirées reste dans la suite pour détecter une régression.

## Portée de la preuve

La simulation exerce les règles et la sérialisation du moteur, **pas 500 sessions d'interface Android ni 500 connexions réseau réelles**. Les gestes, l'audio, SQLite, les appareils physiques, les pertes de paquets et le relais Internet demandent leurs propres essais. Le parcours Android des dix jeux à deux profils complète cette simulation, mais ne prouve pas à lui seul la qualité de l'expérience en soirée.

Commande : `./gradlew testDebugUnitTest --tests com.aperoroyale.PartySimulationTest`

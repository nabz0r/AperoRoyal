# Interface 1.5.0 et stress test de soirée

## Intention visuelle

L'écran d'accueil conserve son illustration de soirée et réduit le poids visuel des commandes. Les boutons sont plus fins, sombres, avec un repère coloré et un libellé lisible. Leur zone tactile reste large. Le salon affiche les amis comme une liste et place les choix Vote, Libre et Turbo côte à côte. Les paramètres audio et de partie sont des lignes plutôt qu'une pile de grandes cartes.

Dans les mini-jeux, l'identité des dix scènes et leurs sprites restent présents. Le cadrage est plus sobre et chaque scène garde sa couleur. Culture G, Blind Test et les réponses de Dessin utilisent des lignes A–D ; Roulette dessine six verres ; Mémoire emploie des cases sombres au liseré coloré ; Rythme et Bombe gardent une grande zone tactile avec un noyau visuel plus petit. Les autres actions bénéficient du même contrôle compact. [Captures de l'accueil](screenshots/home.png), du [salon](screenshots/lobby-four.png) et des [dix jeux](../README.md#dix-défis-dix-ambiances).

## Parcours réels sur émulateur

- Android 16 : deux joueurs ont traversé les dix défis, avec relais, paris, actions de l'entourage, sauvegarde et classement ; les deux premiers tours en mode Vote et Turbo alternent le joueur actif.
- Android 16 : choix directs Vote / Libre / Turbo, réglages et captures sur écran long 1080 × 2340 et écran court 1080 × 1920.
- Deux émulateurs connectés en Wi-Fi local : arrivée d'un invité pendant une manche, affichage du défi en cours sur son téléphone Android 8.0, enchaînement des trois secrets tactiles, scrutin d'admission au changement de manche, refus par délai, gorgée enregistrée dans SQLite et bouton de nouvelle demande. Un parcours antérieur a aussi vérifié son admission après deux votes favorables puis son retour dans l'interface de jeu normale.
- Ces parcours vérifient des interactions et leur rendu. Ils ne mesurent pas le plaisir, la musique sur des enceintes réelles ou la latence de téléphones physiques.

## Trois stress tests reproductibles

| Modèle | Échantillon | Résultat automatisé |
| --- | ---: | --- |
| `TenThousandDevicePartiesTest` | 10 000 soirées, 40 000 manches, 160 000 vues privées FR/EN | 2 tests réussis ; secrets et restaurations exercés |
| `VirtualPeopleSimulationTest` avec `APERO_PEOPLE_PARTIES=10000` | 10 000 soirées, 240 000 manches | 3 tests réussis ; agents à mémoire, votes et contributions |
| `ExtremePartySimulationTest` avec `APERO_SIM_PARTIES=10000` | 10 000 soirées, 180 000 manches | 1 test réussi ; 2–6 joueurs, un/deux/plusieurs appareils modélisés, trois modes, quatre scénarios d'attention |

Les trois modèles ont des hypothèses différentes ; leurs volumes ne constituent pas 30 000 soirées réelles. L'horloge virtuelle évalue des délais de moteur et des temps de réponse supposés. Dans son mélange de scénarios, la moyenne d'une manche va de **82 s** en Vote sur un téléphone à **41,6 s** en Turbo sur des téléphones individuels. Les fins par délai atteignent environ **26 %** en Vote sur un téléphone et **39 %** en Turbo sur téléphones individuels dans cet échantillon : raccourcir la manche ne suffit donc pas à la rendre fluide. Ces chiffres servent à repérer les étapes à expliquer ou raccourcir ; ils ne mesurent ni l'envie de rejouer ni la facilité perçue par de vraies personnes. Le nouveau parcours d'arrivée en cours de partie dispose de tests de transition, de reprise, de bulletins privés et d'un test sur deux émulateurs ; les trois simulations de 10 000 soirées ci-dessus n'intègrent pas encore le comportement social des demandes d'entrée.

**Ce que l'interface change concrètement :** le salon explique chaque mode avant de lancer la partie ; une commande claire occupe chaque étape ; les réponses se lisent comme des choix plutôt que comme un mur de boutons ; les zones tactiles restent larges. Le prochain contrôle utile est chronométré avec de vrais amis : temps jusqu'à la première manche, hésitations avant chaque geste et besoin d'explications orales.

## Reproduire

```sh
./gradlew testDebugUnitTest --tests com.aperoroyale.TenThousandDevicePartiesTest --offline
APERO_PEOPLE_PARTIES=10000 ./gradlew testDebugUnitTest --tests com.aperoroyale.VirtualPeopleSimulationTest --rerun-tasks --offline
APERO_SIM_PARTIES=10000 ./gradlew testDebugUnitTest --tests com.aperoroyale.ExtremePartySimulationTest --rerun-tasks --offline
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turns.py
ADB_SERIAL=emulator-5554 python3 tools/smoke_turbo.py
```

Les scripts `smoke_*` pilotent l'app sur l'émulateur ciblé et peuvent remplacer sa session locale. Les tests Java ne nécessitent pas d'appareil.

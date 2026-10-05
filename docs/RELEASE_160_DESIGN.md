# 1.6.0 — La nuit est à vous

## Direction

Les deux images de référence de la soirée fixent le ton : des adultes autour d'une table, le bar et l'arcade au fond, une lumière chaude, un trait illustré et un humour de situation. La hiérarchie est **les personnes, leur geste, la réaction des amis, puis l'interface**. Les dix mini-jeux conservent une lecture immédiate à une main, mais ne donnent plus l'impression d'un catalogue pour enfants.

Douze portraits originaux représentent des rôles de soirée (vigneron, directrice créative, DJ, antiquaire, diva, chef, photographe…). Ce sont des personnages de fiction, sans rôle attribué par origine ni genre. Un portrait peut être choisi plusieurs fois ; l'import d'une photo reste possible. Chaque jeu reçoit un fragment d'une fresque nocturne distincte et un signe de jeu (enseignes, rideau, vinyle, sous-verres, galerie, cartes, rythme, faisceau, fil). Le résultat garde l'illustration lisible et met le visage du joueur en avant.

Le [jeu officiel Narcos: Cartel Wars & Strategy sur l'App Store](https://apps.apple.com/gb/app/narcos-cartel-wars-strategy/id1143052259) met en avant des personnages à recruter, des alliances et des événements. **Notre interprétation** de cette référence porte sur la clarté des rôles, la collection de visages et l'anticipation des révélations ; ce jeu de stratégie ne constitue ni une preuve de viralité pour un party game, ni une source d'illustrations ou de personnages à reprendre. Les références Android et les limites de leurs avis publics figurent dans [l'analyse sociale](STRESS_TEST_SOCIAL_2026.md).

## Ce qui a changé dans les dix défis

| Jeu | Geste autour de la table | Dévoilement 1.6.0 |
| --- | --- | --- |
| Culture G | Réponses privées des amis ; l'acteur suit la salle ou assume sa réponse. | Bonne réponse et noms de ceux qui avaient vu juste. Une question contestable sur la pieuvre a été remplacée. |
| Positions | Cartes de jeu de rôle de bistrot, solo, assis ou avec un complice volontaire. | Vote du jury et noms des soutiens ; passage possible sans points ni gorgée. |
| Blind Test | Les motifs originaux deviennent des jingles de fiction ; chacun propose son choix. | Intitulé du jingle et oreilles fines nommées. Aucun catalogue tiers n'est utilisé. |
| Réflexe | Chaque ami pose une cible sur le parcours. | Nombre de cibles touchées et auteurs du parcours. |
| Roulette | Les amis protègent des sous-verres ; l'acteur choisit son risque. | Sous-verre, issue et protecteurs du choix. |
| Dessin | Sujets plus situés dans la soirée ; les amis devinent en privé. | Sujet et noms des personnes qui ont reconnu le dessin. |
| Mémoire | Les amis composent la chaîne de symboles. | Progression et premières contributions attribuées. |
| Rythme | La salle compose la mesure à rejouer. | Mesure et personnes qui l'ont composée. |
| Bluff | Alibis de bistrot, vrais ou inventés, défendus devant le jury. | Vérité et noms des personnes convaincues ; passage sans pénalité. |
| Le dernier fil | La bombe circule entre les mains avant le fil final. | Parcours nominatif et fil choisi, conservés à la reprise. |

Le guide dans l'app explique chaque geste en FR/EN. La langue des menus se choisit sur l'accueil et dans **Options → Partie** ; elle est conservée sur l'appareil. La langue d'un tour reste celle du joueur actif. Les deux commandes sont indépendantes.

La bande sonore originale a été réorchestrée vers des phrases plus courtes et plus espacées. Le jeu baisse son ambiance pendant le jingle, le rythme et le bluff. Les sons de touche et le verdict sont moins insistants, avec une ponctuation propre au jeu. Les réglages de musique, style, volume, SFX et vibrations restent séparés.

## Vérifications reproductibles

Commande :

```sh
APERO_SIM_PARTIES=10000 APERO_PEOPLE_PARTIES=10000 ./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline --rerun-tasks
ADB_SERIAL=emulator-5554 python3 tools/smoke_v120.py
```

| Épreuve | Résultat |
| --- | --- |
| Tests JUnit | 48 tests, zéro échec, dont cinq nouveaux tests de langue, portraits partagés, passage, réponses verrouillées, sauvegarde du relais et récits FR/EN des dix jeux. |
| Salles multi-appareils **simulées** | 10 000 soirées, 40 000 manches, 160 000 instantanés privés FR/EN, 80 000 actions interdites rejetées et 400 reprises. |
| Horloge et interruptions **simulées** | 10 000 soirées de 18 manches, 180 000 manches, avec 2 à 6 joueurs, trois modes et différentes absences. |
| Joueurs virtuels **hypothétiques** | 10 000 soirées de 24 manches, 240 000 manches ; goûts, relations, humeur et fatigue influencent les choix. |
| Android 16, un téléphone | Deux profils, dix jeux, alternance des acteurs, mises, actions des amis, historique et captures des dix scènes : `tools/smoke_v120.py`. |
| Deux émulateurs en Wi-Fi simulé | Android 16 héberge, Android 8 rejoint avec IP/PIN. La salle et le premier résultat se synchronisent ; à la manche suivante, l'invité choisit une réponse de Culture G sur son écran et l'hôte reçoit « 1 réponse » avec le choix majoritaire. Le lien passe par une redirection locale du port du simulateur. |

Dans le modèle de joueurs virtuels, le temps moyen d'une manche dépend fortement du mode et du nombre de téléphones : environ **19,5 s** avec chacun son téléphone pour une table « mixed » contre **34,6 s** sur un téléphone partagé, et **51,3 s** dans le scénario partagé « distracted ». Ces nombres viennent des comportements supposés du test. Ils signalent où observer les vrais passages d'appareil ; ils ne mesurent ni des personnes, ni une latence réseau réelle. Les résultats détaillés sont écrits dans les rapports JUnit de Gradle.

## Ce que cette release ne démontre pas

Une simulation ne peut pas prouver qu'une anecdote sera drôle, qu'un portrait plaira, que les sons seront agréables dans un vrai bar ou que dix jeux deviendront viraux. Les parcours matériels détaillés ci-dessus concernent des téléphones **émulés**. Le réseau Wi-Fi, Bluetooth et Internet conserve l'architecture et les tests des versions précédentes ; cette release ne prétend pas mesurer sa latence sur plusieurs téléphones physiques. Les services musicaux externes s'ouvrent dans leur propre application ; le jeu ne pilote pas leurs catalogues. Le relais Internet public reste un service de test.

Prochain test humain utile : observer plusieurs tables sans leur expliquer les scènes à l'avance, noter les règles que les amis inventent spontanément, les hésitations, les phrases reprises le lendemain, puis garder aussi les sessions où personne ne rit. C'est la seule façon d'arbitrer la promesse sociale de cette version.

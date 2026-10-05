# 1.7.0 — Le temps de la table

La soirée doit laisser assez de place au dessin, à l'écoute et aux amis qui hésitent, sans obliger toute la table à attendre une réponse absente. Cette version transforme les observations du [laboratoire organique](LABO_ORGANIQUE_2026.md) en quatre règles de jeu vérifiables.

## Les règles qui changent

1. **Jury et galerie :** dès que la moitié des amis a répondu, les autres disposent de sept secondes pour se prononcer, sans dépasser le chrono de la manche. Si tout le monde répond, le verdict part aussitôt. Sur un téléphone partagé, le décompte est suspendu pendant le passage de main ; l'hôte synchronise sa date de fin aux autres appareils.
2. **Votes exprimés :** Pose et Dessin évaluent les réponses reçues. Une personne absente ne compte pas comme un refus. Une égalité au jury ne valide pas la pose ; au dessin, au moins la moitié des devinettes effectivement jouées doit être juste. Les amis peuvent toujours répondre pendant la grâce.
3. **Un peu d'air :** Dessin reçoit 50 secondes pour créer l'image au lieu de 40 ; Blind Test reçoit 28 secondes au lieu de 23. À deux en Turbo, la réduction est de deux secondes au lieu de quatre. Ce sont des durées maximales : les joueurs qui terminent tôt passent à la suite.
4. **Relances de table :** une phrase liée au jeu apparaît ponctuellement au résultat et au passage de téléphone, en FR ou EN. Elle invite à raconter, taquiner ou improviser, sans bloquer la manche. **Options → Partie → Relances de table** les active ou les coupe sur chaque appareil.

## Comparaison reproductible

Trois graines identiques ont été rejouées avant et après : **30 000 soirées et 720 000 manches par version**, dix jeux, 2 à 6 profils, FR/EN, un, deux ou plusieurs téléphones simulés. Les tableaux donnent l'intervalle min–max des trois graines pour le modèle de récupération médian. Les agents virtuels utilisent des gestes et délais supposés, aucune mesure de vraies personnes ni aucun paquet réseau.

| Situation | Manches expirées 1.6.0 | Manches expirées 1.7.0 | Plus long silence p90 1.6.0 | Plus long silence p90 1.7.0 |
| --- | ---: | ---: | ---: | ---: |
| Un téléphone | 15,3–15,6 % | 12,7–13,1 % | 106,1–107,6 s | 106,5–108,6 s |
| Deux téléphones | 8,2–8,5 % | 6,3–6,6 % | 69,7–70,9 s | 70,7–70,9 s |
| Un téléphone chacun | 8,1–8,3 % | 6,3–6,4 % | 64,7–65,3 s | 66,1–66,4 s |
| Turbo | 14,5–14,8 % | 11,6–11,9 % | 93,0–94,9 s | 92,4–94,6 s |
| Deux joueurs | 9,7–10,2 % | 7,0–7,4 % | 53,5–53,9 s | 55,1–56,1 s |
| Six joueurs | 12,1–12,5 % | 10,1–10,4 % | 98,2–101,2 s | 98,9–100,7 s |

Le silence p90 augmente légèrement dans certaines configurations : les secondes ajoutées au dessin et au Blind Test donnent du temps aux réponses mais ne créent pas forcément plus d'actions. Les relances de table ne sont pas interprétées par les agents virtuels ; leur effet social reste donc à observer avec des personnes.

| Jeu | Manches expirées 1.6.0 | Manches expirées 1.7.0 |
| --- | ---: | ---: |
| Culture G | 12,0–12,5 % | 11,2–12,0 % |
| Positions | 8,4–8,8 % | 7,7–8,0 % |
| Blind Test | 14,2–14,9 % | 6,1–6,9 % |
| Réflexe | 12,4–12,8 % | 11,7–12,3 % |
| Roulette | 1,1–1,2 % | 1,1–1,2 % |
| Dessin | 17,8–18,1 % | 7,9–8,3 % |
| Mémoire | 12,0–12,4 % | 11,9–12,1 % |
| Rythme | 0 % | 0 % |
| Bluff | 14,0–14,4 % | 13,6–13,9 % |
| Bombe | 10,6–10,9 % | 10,6–10,7 % |

Rythme vaut 0 % parce que les agents terminent son script avant le délai ; ce n'est pas une garantie sur un téléphone réel. Les petites variations des jeux sans réglage direct viennent de l'évolution de la soirée simulée après les manches touchées.

| Graine | Cohortes 1.7.0 | Dix jeux 1.7.0 |
| --- | --- | --- |
| `20261005` | [CSV](data/organism-v170-10k-20261005-cohorts.csv) | [CSV](data/organism-v170-10k-20261005-games.csv) |
| `8675309` | [CSV](data/organism-v170-10k-8675309-cohorts.csv) | [CSV](data/organism-v170-10k-8675309-games.csv) |
| `11674260475909` | [CSV](data/organism-v170-10k-11674260475909-cohorts.csv) | [CSV](data/organism-v170-10k-11674260475909-games.csv) |

Les [CSV de 1.6.0](LABO_ORGANIQUE_2026.md#fichiers-et-reproduction), hypothèses et commandes d'export sont dans le laboratoire. Pour reproduire 1.7.0, relancer le même test avec `APERO_ORGANISM_PARTIES=10000` et chacune des trois graines, puis exporter avec `tools/export_organism_stress.py`.

## Vérifications et limites

- 56 tests Java réussis, dont cinq nouveaux tests pour le quorum, les votes exprimés, la grâce à deux, les délais et les relances FR/EN. Les trois campagnes de 10 000 soirées passent avec les autres simulations.
- Le parcours sur émulateur Android 16 a terminé les dix jeux avec deux profils alternés, mises, actions de l'ami, résultats et historique. La capture [Dessin](screenshots/games/drawing-result.png) montre une relance au résultat ; [Blind Test](screenshots/games/blind-test.png) montre le nouveau chrono ; [Options](screenshots/settings-party.png) montre l'interrupteur.
- APK 1.7.0 signée et installée sur Android 16 et Android 8 émulés ; le certificat est identique à celui de 1.6.0. La mise à jour signée de 1.6.0 vers 1.7.0 a été acceptée sur les deux émulateurs avant le parcours de débogage.

Le test virtuel ne valide ni l'amusement, ni le goût graphique ou sonore, ni la latence Wi-Fi/Bluetooth/Internet sur des téléphones physiques. La radio externe ouvre les services musicaux dans leur application ; le Blind Test reste fondé sur des motifs originaux hors ligne.

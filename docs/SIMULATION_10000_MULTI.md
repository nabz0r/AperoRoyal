# Audit 1.4.5 — 10 000 soirées sur appareils distincts

## Ce qui a été simulé

`TenThousandDevicePartiesTest` crée **10 000 salles indépendantes**, de 2 à 6 joueurs, avec profils FR et EN alternés. Chaque salle traverse quatre manches en mode Vote, Libre ou Turbo. Les dix mini-jeux sont sélectionnés ; un appareil virtuel distinct reçoit pour chaque joueur son propre instantané réseau. L’hôte reste seul arbitre des réponses, secrets, tours et règles.

Chaque invité éligible sonde la ruelle, joue les quatre gestes du micro-jeu découvert, tente une mauvaise case puis reçoit un nouvel état. Le test vérifie que le joueur actif ne peut pas exploiter l’attente, que la bombe en cours conserve son relais, que les objectifs et les secrets des autres ne sont pas transmis, et que les langues des profils survivent aux états privés. Un deuxième test reprend un secret après un résultat, restaure la sauvegarde et vérifie qu’une découverte remet en jeu la règle **au tour suivant**, sans changer les gorgées ou points de la manche active.

| Mesure | Résultat |
| --- | ---: |
| Soirées simulées | 10 000 |
| Manches terminées | 40 000 |
| Instantanés privés FR/EN inspectés | 160 000 |
| Secrets terminés | 35 945 |
| Poursuite / code / miroir | 12 731 / 14 731 / 8 483 |
| Gestes interdits rejetés | 80 000 |
| Reprises d’état hôte | 400 |

Le test est reproductible avec :

```sh
./gradlew testDebugUnitTest --tests com.aperoroyale.TenThousandDevicePartiesTest
```

## Décisions de rythme et de design

- Une rencontre ne démarre que lorsque l’action principale du joueur est déjà envoyée. Le secret n’empêche pas de répondre, voter, miser, juger ou passer la bombe.
- Trois tapotements sur un indice discret découvrent un jeu. Ensuite, quatre gestes avec une logique différente remplacent l’ancien compteur mécanique de 20 tapotements.
- Le secret continue sur les écrans de résultat et de sélection ; sa progression est enregistrée si la manche s’achève. Une seule découverte par joueur et par tour évite de transformer l’attente en course de score.
- Les réussites ne donnent pas de points au classement : un ami dont le téléphone attend plus longtemps ne doit pas prendre l’avantage. La première réussite d’un type de secret peut proposer une nouvelle règle à toute la salle au prochain tour.
- Le nouvel écran dessine une ruelle nocturne, quatre cases contrastées et un chat pixel animé. Le joueur EN voit le texte EN sur son téléphone même si l’acteur est FR.

La participation pendant l’attente est un principe déjà exploité par les party games : [Jackbox décrit l’« Audience Play-Along »](https://www.jackboxgames.com/blog/the-ability-to-kick-players-and-other-new-features-coming-to-party-pack-9) et [Quiplash fait voter le public depuis ses propres appareils](https://checkout.jackboxgames.com/en-gb/products/quiplash). Ici, la découverte reste facultative et ne détourne pas la manche des amis.

## Portée des preuves

Les 10 000 salles tournent **dans le moteur**, sans 10 000 sockets ni 10 000 personnes. Les captures privées testent la forme des données distribuées et les règles d’autorisation, pas la latence réelle du Wi‑Fi, du Bluetooth ou d’Internet. Une session Wi‑Fi entre émulateurs Android 16 et Android 8 a vérifié le parcours complet : invité EN, hôte FR, indice découvert, code secret terminé **après** le résultat de la manche, trophée reçu, choix de règle sur le téléphone invité, règle `3` appliquée par l’hôte. Le plaisir, la fréquence naturelle de découverte et l’envie de rejouer demandent encore des essais avec des groupes réels.

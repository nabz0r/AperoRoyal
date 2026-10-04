# 1.4.6 — Une table qu'on retrouve, une soirée qu'on pilote

## Ce qui bloquait la soirée

L'accueil affichait cinq grands boutons semblables après une image statique. « Nouvelle soirée » effaçait tous les joueurs, même lorsqu'ils venaient de jouer ensemble. Le menu en partie menait aux réglages, sans moyen visible d'annuler un défi ni de revenir au salon. Les liens musicaux étaient dispersés dans plusieurs écrans et la sélection d'une plateforme ne la lançait pas immédiatement.

## Références Play Store et décisions

Fiches publiques consultées le 4 octobre 2026. Les téléchargements et notes sont des indicateurs de diffusion, **pas une preuve que tel élément explique le succès**.

| Jeu | Observation de la fiche | Décision pour Apéro Royale |
| --- | --- | --- |
| [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup) · 10 M+ téléchargements affichés | Un joueur agit immédiatement, les amis donnent les indices, catégories et manche courte. | L'accueil met le groupe et l'action de jeu au premier plan ; accès direct au guide des dix défis. |
| [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych) · 5 M+ affichés | Le bluff et les révélations viennent des amis, et non d'une interface à apprendre. | Les dix jeux gardent une action collective, le pari et les réponses cachées ; les sorties de partie ne forcent plus à refaire l'équipe. |
| [Plato](https://play.google.com/store/apps/details?id=com.plato.android) · 50 M+ affichés | Profils personnalisables, catalogue de jeux, salon social et classement. | Une table locale mémorise noms, langues et portraits ; le classement et la radio ont un accès court dès l'accueil. Le salon multijoueur existant reste séparé des profils locaux. |

Nous ne réutilisons ni personnages, ni graphismes, ni marques de ces jeux. La nouvelle hiérarchie visuelle conserve l'univers nocturne Apéro Royale : scène panoramique, typographie plus expressive, carte de l'équipe, quatre raccourcis compacts et balayage lumineux discret sur l'action principale. Les commandes de jeu gardent une grande surface tactile ; les boutons de navigation n'occupent plus chacun toute la largeur.

## Règles des nouveaux parcours

- **Même équipe.** Les profils locaux sont enregistrés dans une table SQLite dédiée, indépendante de la partie en cours. Une nouvelle soirée recharge ces profils et remet les scores de soirée à zéro. Un profil conserve son portrait importé. On peut corriger pseudo/langue, changer l'image ou retirer un membre depuis le salon. Renommer un pseudo déplace aussi ses statistiques et son historique vers le nouveau nom, sauf si celui-ci appartient déjà à un ancien profil. « Changer de groupe » vide seulement cette table ; l'historique des résultats reste conservé.
- **Pause locale.** Le menu ☰ offre reprise, annulation du défi courant, retour au salon et accueil avec reprise ultérieure. Le chrono est suspendu tant que ce menu reste ouvert. Annuler un défi relance la sélection du jeu sans écrire de résultat, point ni gorgée. Quitter le salon remet la soirée à zéro après confirmation ; les manches déjà terminées restent dans le classement historique. En réseau, l'hôte reste l'autorité et ce menu local ne coupe pas la partie des autres.
- **Radio Apéro.** Un panneau sombre accessible depuis l'accueil ou l'en-tête rassemble bande originale, Spotify, Deezer, Apple Music, Amazon Music, silence, ouverture de la playlist et commandes play/pause/suivant. Choisir une plateforme ouvre directement son application ou sa page Web ; le lien de playlist HTTPS mémorisé est utilisé s'il correspond au domaine autorisé. Les touches play/pause/suivant sont transmises au lecteur Android actif via [`AudioManager.dispatchMediaKeyEvent`](https://developer.android.com/reference/android/media/AudioManager#dispatchMediaKeyEvent). Elles exigent qu'un lecteur externe soit déjà actif et ne garantissent pas que le service choisi accepte la commande. Le jeu ne connaît ni compte, ni titre en lecture.

Le [contrôle des sessions des autres applications](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName)) demanderait un accès privilégié ou un service de notifications activé. Cette version n'ajoute pas cette permission. Spotify, Deezer, Apple Music et Amazon Music restent des raccourcis et des lecteurs externes ; le Blind Test utilise les motifs originaux hors ligne.

## Vérification attendue

Le script `tools/smoke_release_146.py` contrôle sur émulateur : deux profils créés une fois, nouveau jeu et redémarrage de l'application, renommage sans perte de score ni d'historique, pause de plus de 12 secondes pendant une préparation, défi annulé sans ligne d'historique, retour au salon, changement de groupe, ouverture de la radio et sauvegarde de la source choisie. La suite Java vérifie les règles et les simulations déjà présentes. Une mise à niveau signée sur Android 8 contrôle la migration SQLite de v3 vers v4.

Ce parcours technique ne remplace pas une soirée avec de vrais joueurs : envie de rejouer, qualité de la musique externe, latence Wi-Fi/Bluetooth/Internet et reprise depuis les applications musicales restent à observer sur téléphones physiques.

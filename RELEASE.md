# Apéro Royale 1.4.2 — Le rythme sous la loupe

- [APK Android signé 1.4.2](releases/AperoRoyale-v1.4.2.apk) · `com.aperoroyale` · versionCode `9` · Android 8.0+ (API 26)
- SHA-256 : `9bc4da3199785da590d1d8ddb49aa5b91227bf77d77cef3f262c156eb61ba02a`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.1

## Changements

- [Audit de 500 soirées supplémentaires](docs/SIMULATION_EXPERIENCE_500.md) : 7 990 manches avec temps modélisé, part de préparation, répétition des jeux et cartes, et participation propre à chaque défi. Le scénario à six sur un téléphone en mode Vote ressort à 87,3 s modélisées par manche ; six défis sur dix restent essentiellement solo. Ces constats sont des risques de conception, non une validation du plaisir.
- QCM, poses, motifs sonores, dessins et amorces de bluff passent dans des paquets mélangés persistants : aucune carte ne revient avant épuisement du paquet, y compris après reprise de la partie.
- Roulette : 10 s pour choisir un gobelet, puis révélation complète. Dessin : 30 s pour dessiner, 12 s pour deviner ; le relais sur téléphone partagé suspend le délai du devineur.

## Vérifications

- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. Les deux simulations totalisent 1 000 soirées et 15 980 manches ; la nouvelle simulation sert au diagnostic de rythme avec hypothèses publiées.
- Parcours sur émulateur Android 16 : les dix mini-jeux terminés à deux profils locaux avec passage de téléphone, mises, pronostics et historique.
- Test de passage privé sur Android 16 : pronostic Turbo et jury suspendus pendant le relais, reprise après fermeture et arrière-plan. Le script a été adapté à l'emplacement actuel du bouton « Reprendre ».
- Test de rythme sur Android 16 : après plus de 12 s de passage privé, le devineur garde son nouveau chrono ; la roulette sans choix expire avec une défaite enregistrée.
- APK release installée et lancée sur émulateur Android 8.0. `apksigner verify` valide la signature v2 ; `aapt` confirme versionCode 9 / minSdk 26.

**Limites.** Les durées du rapport sont des estimations, pas des observations humaines ou des mesures de latence Bluetooth/Wi-Fi/Internet. Le plaisir entre amis, l'attente ressentie et les mécaniques collectives à refaire nécessitent des tests avec de vrais groupes. Les dix refontes de gameplay décrites dans la documentation ne sont pas livrées dans 1.4.2.

---

# Apéro Royale 1.4.1 — Une soirée qui continue

- [APK Android signé 1.4.1](releases/AperoRoyale-v1.4.1.apk) · `com.aperoroyale` · versionCode `8` · Android 8.0+ (API 26)
- SHA-256 : `a02808a53e268c7f75635272f07ad3849508e8d08bad878d15aef82437927fc5`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.4.0

## Correctifs

- En Turbo, une règle secrète débloquée dispose maintenant d'un écran de choix avant le défi suivant, y compris lorsque la partie est reprise ou synchronisée.
- À l'expiration du chrono d'un jury, le moteur applique les votes exprimés pour les défis de pose et de bluff.

## Vérifications

- [Simulation reproductible de 500 soirées](docs/SIMULATION_500_PARTIES.md) : 7 990 manches de deux à six joueurs sur les dix jeux et les trois modes, 40 190 restaurations JSON, 498 choix de règle secrète dont 166 en Turbo, et 436 signalements soumis au vote. Les deux anomalies ont été détectées puis couvertes par des tests ciblés.
- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite. `apksigner verify` valide la signature v2 et `aapt` confirme versionCode 8 / minSdk 26.
- Émulateur Android 16 : les dix jeux terminés à deux profils avec passage de tour, mises, pronostics et historique ; deux défis Turbo terminés ; secret Turbo débloqué puis règle choisie à l'écran après une manche.
- Émulateur Android 8.0 : APK signé installé, activité lancée et processus encore actif après affichage de l'accueil.

**Limites.** La simulation porte sur le moteur et ses instantanés ; elle ne représente pas 500 soirées humaines, connexions réseau réelles, interactions audio ou sessions sur téléphones physiques. Les limites de l'intégration musicale et du relais Internet de 1.4.0 restent applicables.

---

# Apéro Royale 1.4.0 — La nuit vous appartient

- [APK Android signé 1.4.0](releases/AperoRoyale-v1.4.0.apk) · `com.aperoroyale` · versionCode `7` · Android 8.0+ (API 26)
- SHA-256 : `3525d3465b5828ca91d6d0ca4f8915d79de0b511bcd806ac77c0aaa898cc1f20`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.3.0

## Changements

- **Nouvelle direction visuelle.** Accueil illustré comme un salon d'arcade nocturne, palette charbon/laiton, composants moins arrondis et icône redessinée. Les dix scènes de jeu conservent leurs mécaniques et adoptent la nouvelle palette ; leurs captures ont été régénérées.
- **Musique à votre goût.** Le menu de réglages permet de choisir bande originale, Spotify, Deezer, Apple Music, Amazon Music ou silence. Un lien HTTPS de playlist peut être mémorisé pour chaque plateforme ; le bouton d'ouverture et le raccourci ♫ en jeu lancent l'application musicale ou le navigateur. Musique du jeu, effets et vibrations sont indépendants. Le volume initial de la bande originale passe à 18 % sur une nouvelle installation.
- **Blind Test autonome.** L'ancien accès OAuth et les appels Web API Spotify ont été retirés du jeu. Le mini-jeu reste fondé sur ses motifs sonores originaux et jouable hors ligne. La [politique développeur Spotify](https://developer.spotify.com/policy) interdit de construire un jeu ou quiz avec sa plateforme sans autorisation applicable.
- **Plan de la suite.** [Audit et refonte des dix jeux](docs/REFONTE_DIX_MINI_JEUX.md) : rôle de chaque joueur, latence, contenu, rejouabilité, moments à partager et critères de test. Ces refontes collectives sont des spécifications, pas des mécaniques déjà livrées.

## Vérifications de 1.4.0

- `./gradlew testDebugUnitTest assembleDebug assembleRelease lintVitalRelease --offline` : réussite, dont tests des liens HTTPS et fournisseurs.
- Émulateur Android 16 : écran d'accueil, réglages, choix de Deezer, lien externe ouvert dans Chrome. Le parcours automatisé a terminé les dix mini-jeux avec deux profils, alternance des acteurs, mises, pronostics, historique et dix captures actualisées.
- Émulateur Android 8.0 : APK signé installé et activité lancée. `apksigner verify` valide la signature v2 ; `aapt` confirme versionCode 7 et minSdk 26.

**Limites de validation.** Aucune session sur de vrais téléphones avec comptes Spotify, Deezer, Apple Music ou Amazon Music, ni test complet Bluetooth ou Internet sur cette version. Le bouton musical ouvre un service externe : Apéro Royale n'en pilote pas lecture, pause ou volume. Les dix refontes de gameplay restent à développer et à éprouver auprès de groupes réels. Le relais Internet public, l'identité réseau et le classement historique par pseudo gardent les limites décrites ci-dessous.

---

# Apéro Royale 1.3.0 — La salle joue avec toi

- [APK Android signé 1.3.0](releases/AperoRoyale-v1.3.0.apk) · `com.aperoroyale` · versionCode `6` · Android 8.0+ (API 26)
- SHA-256 : `690edb2c48925e8d8034ae14ba035510e7521aaf525f738a7f4a28d26cca79a6`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.2.1

## Changements

- **Pronostics qui agissent sur la manche.** Couvrir ajoute jusqu'à six secondes aux jeux chronométrés compatibles et retire une gorgée virtuelle sur une défaite. Défier augmente le gain du joueur actif s'il l'emporte et rapporte davantage à un pronostiqueur qui avait annoncé sa défaite. Le passage privé reste actif sur un seul téléphone.
- **Bluff et bombe plus collectifs.** Le conteur choisit secrètement si son anecdote est vraie ou inventée ; le jury vote et peut être trompé. La bombe demande deux actions par joueur avant chaque passage.
- **Rythme vérifié par l'hôte.** Les touches sont évaluées sur la même période de 600 ms que l'affichage, avec une marge réseau bornée. La séquence ne peut plus être validée avec quatre touches simplement croissantes.
- **Secrets mieux isolés.** Les instantanés invités masquent réponses, gobelet perdant, vérité du bluff et détail des votes avant révélation. Les actions restent arbitrées par l'hôte.
- **Son et visuel.** Six motifs auditifs distincts remplacent les mélodies anonymes du Blind Test hors ligne ; la musique de fond a quatre phrases et des variations par jeu. La Radio Apéro prend la place de la musique synthétique lorsqu'elle joue. Les sprites réagissent davantage dans les dix scènes, dont les captures sont actualisées.
- **Navigation.** Les actions principales du Canvas exposent désormais des boutons et cibles nommés à l'accessibilité Android ; les barres système se recachent après les boîtes de dialogue.
- **Contenu.** Une carte ne peut plus revenir immédiatement dans le même mini-jeu pendant la partie.

## Vérifications de 1.3.0

- `./gradlew testDebugUnitTest assembleRelease lintVitalRelease --offline` : réussite. Les tests unitaires couvrent l'horloge du rythme, le soutien/défi, la vérité du bluff, la bombe et le non-retour immédiat d'une carte.
- `tools/smoke_v120.py` sur émulateur Android 16 : les dix jeux sont terminés avec deux profils, alternance des tours, mises, pronostics et historique ; dix captures de jeu régénérées.
- Deux émulateurs Android 16 et Android 8.0 reliés en Wi-Fi local simulé : vote, passage, mise, pronostic, dessin depuis le téléphone invité, réponse sur l'hôte et résultat synchronisé.
- L'arborescence Android de la version signée expose les boutons nommés de l'accueil. L'APK 1.3.0 signé s'installe et démarre sur l'émulateur Android 8.0 ; `aapt` confirme versionCode 6 / minSdk 26 et `apksigner verify` valide la signature.

**Limites de validation.** Aucun essai de 1.3.0 n'a été réalisé sur des téléphones physiques appairés en Bluetooth, via le relais Internet public ou avec un compte Spotify réel. Le relais Internet dépend d'un service public de test ; le code partagé n'est pas une identité cryptographique par joueur. Le classement historique reste sur l'hôte et utilise encore le pseudo comme clé. Les commandes accessibles ont été détectées par Android, sans session complète avec TalkBack ni test utilisateur en soirée. Les mesures de latence et l'appréciation du plaisir de jeu restent à faire avec de vrais groupes.

---

# Apéro Royale 1.2.1 — Un tour sans chrono perdu

- [APK Android signé 1.2.1](releases/AperoRoyale-v1.2.1.apk) · `com.aperoroyale` · versionCode `5` · Android 8.0+ (API 26)
- SHA-256 : `6061e22e774cd8868833726b529acc766fc485b6d58ab1ffd033337962624c96`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que 1.2.0

## Changements

- **Passage de téléphone serein.** Le chrono des pronostics, du jury et des votes de règle attend maintenant la confirmation du joueur suivant. L’écran de passage l’indique clairement.
- **Reprise fiable.** Une partie rouverte restaure le passage privé et redonne un chrono utilisable. Les manches locales ne se terminent pas pendant que l’application est en arrière-plan ; les réglages préservent aussi les chronos des pronostics et votes de règle.
- **Formats courts et langues.** L’interface garde au moins 820 unités de hauteur virtuelle et centre son contenu sur les écrans 16:9. Les consignes privées suivent la langue du joueur qui prend le téléphone.
- **Musique plus discrète.** La musique procédurale se tait lorsque l’application est en arrière-plan. Les menus hors défi sont rafraîchis moins souvent pour économiser les ressources.

## Vérifications de 1.2.1

- `./gradlew test assembleDebug assembleRelease lintVitalRelease` : réussite ; Gradle indique `NO-SOURCE` pour les tests unitaires Java, donc les parcours ci-dessous portent la vérification fonctionnelle.
- `tools/smoke_handoff.py` sur émulateur Android 16 : pronostic Turbo toujours ouvert après 6 secondes de passage, jury toujours ouvert après 21 secondes, reprise après fermeture, puis après passage en arrière-plan.
- `tools/smoke_turbo.py` : deux défis consécutifs et alternance des joueurs ; `tools/smoke_v120.py` : dix jeux terminés à deux joueurs avec mises, pronostics et historique.
- Affichage et navigation vérifiés sur émulateur 1080 × 1920 ; format 1080 × 2340 utilisé pour les parcours complets.
- APK release signé installé en mise à jour et lancé sur émulateur Android 8.0 ; `aapt` confirme versionCode 5 / minSdk 26 et `apksigner verify` valide la signature.

Les limites réseau, Bluetooth et Spotify décrites ci-dessous restent applicables ; aucun nouvel essai sur téléphones physiques ou comptes Spotify réels n’a été effectué pour 1.2.1.

---

# Apéro Royale 1.2.0 — Le tour passe enfin

- [APK Android signé](releases/AperoRoyale-v1.2.0.apk) · `com.aperoroyale` · versionCode `4` · Android 8.0+ (API 26)
- SHA-256 de l’APK : `037f250b157d30595c8d6dc773844ce3a8210fd8c5aa39118552c3dbaf0cba5f`
- Certificat SHA-256 : `3128c11a3bdfba86472d2ca304cafe4105b8d1e13f20d56986ed10c20f94e120` — même clé que les versions précédentes

## Ce qui change

**Le tour de chacun est explicite.** L’écran de passage annonce le joueur et le défi, puis attend son « Je suis prêt » avant la mise. À deux sur un téléphone, le premier vote part désormais du joueur actif ; après chaque manche, l’ordre s’inverse. Les pronostics, jurys et dessins à deviner ont leur propre relais privé. Le second joueur intervient donc pendant chaque manche au lieu d’attendre passivement la suivante.

**Mode Turbo.** Le lobby propose maintenant Vote, Libre et Turbo. Turbo choisit un défi surprise sans attendre le scrutin et raccourcit les chronos de jeu ; les mises et pronostics restent présents.

**Décors et son.** Dix décors rétro nocturnes originaux habillent les mini-jeux et l’écran de passage, sans masquer les commandes. Le morceau procédural est moins répétitif, démarre à volume réduit et marque le changement de joueur avec un court signal. Musique, volume, style, SFX et vibrations restent réglables.

**Dessin en réseau.** Le devineur distant choisit sa réponse depuis son propre téléphone ; l’hôte vérifie cette action avant d’attribuer le résultat. Le devineur local reçoit un écran de passage avant de voir les réponses.

Le [README](README.md) présente le nouveau parcours et les captures des dix jeux ; [l’écran de passage](docs/screenshots/handoff.png) montre le moment « à toi de jouer ».

## Vérifications

- `./gradlew test assembleDebug assembleRelease lintVitalRelease` : réussite. Les tâches unitaires Gradle indiquent encore `NO-SOURCE` ; les parcours réels sont couverts par les scripts ci-dessous.
- `tools/smoke_v120.py` sur émulateur Android 16 : dix jeux terminés, deux profils alternés, dix mises et pronostics, relais du dessin, historique SQLite et dix nouvelles captures.
- `tools/smoke_turns.py` : deux manches Vote complètes, premier votant et joueur actif alternés.
- `tools/smoke_turbo.py` : deux manches Turbo, sélection directe et alternance des joueurs.
- Deux émulateurs Android 16 et Android 8.0 en Wi-Fi via redirection locale : le second téléphone rejoint la salle, vote, reçoit son tour, valide lui-même « Je suis prêt » et place sa mise. Le jeu de dessin a aussi été terminé avec l’artiste sur l’hôte et le devineur sur l’autre téléphone ; le résultat a été enregistré sur l’hôte.
- L’APK signé s’installe en mise à jour sur Android 8.0. `aapt` annonce versionCode 4 / minSdk 26 ; `apksigner verify` valide la signature et le certificat ci-dessus.

## Limites actuelles

Bluetooth RFCOMM demande deux téléphones appairés et n’a pas été revalidé sur les émulateurs. Le salon Internet dépend d’un relais MQTT TLS public de test ; la version précédente a été vérifiée jusqu’au vote et à la transition, mais cette version n’a pas refait un tour complet sur ce relais. Spotify demande une configuration et un appareil compatible ; aucun compte Spotify réel n’a été utilisé dans cette validation. Le jeu en réseau garde l’hôte ouvert pendant la partie.

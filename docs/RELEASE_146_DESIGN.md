# 1.4.6 — A party you can return to

## The problem

The home screen showed five nearly identical large buttons below a static image. “New party” erased the local lineup even if the same friends had just played. The in-game menu reached settings but offered no obvious cancel or return-to-lobby action. Music links were scattered and choosing a service did not open it immediately.

## Historical Play Store references

Public listings reviewed on 4 October 2026 suggested useful patterns; displayed download counts at the time were distribution indicators, **not proof that any feature caused success**.

| Example | Observed pattern | Decision for Apéro Royale |
| --- | --- | --- |
| [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup) | One person acts immediately; friends give clues in short rounds. | Put the group and the next action first; make game guides easy to reach. |
| [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych) | Friends create much of the bluff and reveal. | Keep a collective action and hidden choices without forcing roster recreation. |
| [Plato](https://play.google.com/store/apps/details?id=com.plato.android) | Profiles, game catalog, social rooms and rankings. | Remember the local table; keep ranking and radio close to home. |

The update borrowed no characters, art or branding. Its hierarchy used a panoramic night scene, expressive type, a lineup card, four compact shortcuts and a restrained highlight on the primary action. Gameplay targets stayed comfortably touchable while navigation controls stopped occupying an entire row each.

## Shipped flows

- **Same lineup:** a dedicated SQLite table stored local names, languages and portraits independently of the running party. A new party reloaded them and reset party scores. Players could rename, change language or image, and remove a member in the lobby. A rename migrated matching stats and history unless the new name already belonged to an old profile. “Change group” cleared only the local lineup, leaving result history.
- **Local pause:** the menu offered resume, cancel current challenge, return to lobby and home with later resume. Timers paused while the menu stayed open. Canceling a challenge returned to game selection without a result, points or sips. Leaving the lobby reset the current party after confirmation while completed historical results remained. In a network room, the host remained authoritative.
- **Radio Apéro:** one panel gathered original soundtrack, Spotify, Deezer, Apple Music, Amazon Music, mute, saved playlist URL and play/pause/next controls. Choosing a service opened its app or website. The saved HTTPS playlist URL applied only to allowed domains. [Android media key events](https://developer.android.com/reference/android/media/AudioManager#dispatchMediaKeyEvent) were dispatched to an already active external player; acceptance was not guaranteed. The game did not know the account or playing track.

Access to [other apps' active media sessions](https://developer.android.com/reference/android/media/session/MediaSessionManager#getActiveSessions(android.content.ComponentName)) would require additional privileges or notification access; this release did not add them. The sound quiz kept original offline motifs.

## Verification boundary

The emulator script tools/smoke_release_146.py checked profile reuse after a new party and app restart, rename without losing history, a pause longer than 12 seconds during setup, cancel without a history row, lobby return, group change and radio-source persistence. Java tests covered rules and existing simulations. An Android 8 signed upgrade checked the SQLite v3→v4 migration. These technical checks did not measure real-party replay appeal, external music quality or physical-device network behavior.

# The table is the console

## Social stress test and art direction · 5 October 2026

![Original night-table concept art: faces and performance ahead of the screen](concepts/night-table-2026.png)

*Original concept board, not a screenshot of the shipped game.*

Apéro Royale's engine tests and synthetic parties can check transitions, saves and delays **under a model's assumptions**. They cannot tell us whether a line is funny, a pause feels companionable, a refusal stings, or someone spontaneously demands a rematch. This study therefore examined **scenes between people**. The app deals roles, keeps secrets and stages the reveal; friends make the show.

## Method and limits

This October 2026 review compared the then-current 1.5.0 screenshots and rules with six Google Play party-game listings, selected visible public reviews and research on co-located mobile play. Download badges showed distribution at the time, not quality or causality. Featured reviews were not representative. I did not install the competitors, observe a real party or measure their retention. The scenes below are design stress cases, not probabilities of human behavior.

| Play Store example reviewed then | Social spark | Friction in the visible reviews | Question for Apéro Royale |
| --- | --- | --- | --- |
| [Picolo](https://play.google.com/store/apps/details?id=com.picolo.android) | Enter names, receive a prompt, let the phone fade behind the conversation. | Some objected to subscription cost for occasional use. | Can a scene start without a tunnel of votes and confirmations? |
| [Heads Up!](https://play.google.com/store/apps/details?id=com.wb.headsup) | The phone becomes a prop while friends shout and improvise. | Requests for touch fallback, more varied cards and reliable purchase restoration. | When should our screen be a physical sign instead of a dashboard? |
| [Undercover](https://play.google.com/store/apps/details?id=com.yanstarstudio.joss.undercover) | A simple secret triggers lies and a group reveal. | Some described confusing online/offline behavior or limited words. | Does our bluff create an accusation people can retell? |
| [Psych!](https://play.google.com/store/apps/details?id=com.wb.goog.ellen.psych) | Friends write the wrong answers that fool one another. | Requests for chat and who-fooled-whom stats; complaints about ads and lost sessions. | Do we show the authors of the chaos and survive interruptions? |
| [Truth or Dare: Spin the Bottle](https://play.google.com/store/apps/details?id=com.therisingtechie.truthordare) | Turn order, random choices, wheels, custom cards and offline play offer easy entry points. | Some visible reviews welcomed quicker UI and variety. | Does choice of mood help the group, or add another pre-game menu? |
| [Truth or Dare by Snash](https://play.google.com/store/apps/details?id=snash.app.truthordare) | Personal cards can turn a generic task into an inside joke. | Some found prompts vague or gender categories restrictive. | Are our prompts finishable without assigning people an unwanted role? |

Research on [co-located mobile play](https://pure.au.dk/portal/en/publications/designing-for-social-play-in-co-located-mobile-games/) and [phone use in pub conversation](https://nottingham-repository.worktribe.com/output/775042/using-mobile-phones-in-pub-talk) informed this **design inference**: a good screen action should give people a reason to look up again. Neither study measured Apéro Royale.

## Stress scenes for the 1.5.0 flow

| Scene | Plausible human outcomes | State then | Prototype to try |
| --- | --- | --- | --- |
| Two friends, five minutes before going out | They relish a rematch or leave during the first handoff. | Recovered profiles, then sequential mode, game, wager and contribution choices. | “Start a scene” chooses a two-player game immediately. |
| Six friends sharing one phone | The handoff becomes a joke or absorbs every hand and conversation. | Some timers paused, but private choices still circulated. | Separate truly secret choices from votes the table can make aloud. |
| Four phones and two conversations | A wager becomes a story or everyone watches their own screen. | Choices synced across devices. | One common reveal visible across the table, then room to react. |
| A late friend is jokingly refused entry | They laugh and retry or feel excluded. | Spectator secrets, majority vote and virtual sip after refusal. | Make refusal reversible; let the person decline the challenge; avoid penalty loops. |
| Someone declines a personal story or pose | They invent a funny fiction or disengage. | Jury-driven performance prompts. | Explicit fiction, duo and pass routes without a lecture. |
| A Trivia answer is disputed | The debate becomes the best moment or feels unfair. | Multiple choice and final reveal. | Brief sourced explanation and a light, non-blocking challenge option. |
| Music covers voices | Friends sing along or fight the sound mix. | Game audio plus external music launchers, no native control of service playback. | Immediate mute and short cues that leave gaps for talk. |
| Network drops during reveal | The group laughs anyway or loses confidence in the score. | Host held the state; physical-device recovery was not fully evidenced. | Idempotent result, role-correct reconnect and an aloud-readable verdict. |
| The inside joke beats the score | A phrase lives all night or vanishes behind points. | Persistent scores and leaderboard. | Player-chosen local memory card, with no required photo or name. |

The [Trivia](screenshots/games/trivia.png), [Poses](screenshots/games/poses.png), [Drawing](screenshots/games/drawing.png) and [Bomb](screenshots/games/bomb.png) captures had distinct scenery and sprites yet a similar frame, title, timer and main action. That consistency helped orientation, but made some scenes feel like steps in a system. This was an artistic reading, not measured preference.

## Ten scenes worth prototyping

These are **staging ideas**, not shipped features:

| Game | Scene | Table reaction to stage |
| --- | --- | --- |
| Trivia | “Who do you trust?” Choose a friend before seeing their answer. | Reveal the alliance chain before the correct answer. |
| Poses | An absurd poster and a willing co-star. | Friends title the pose, applaud or request an encore. |
| Sound | Original motifs become fake adverts, TV themes or fictional jingles. | The story around the sound matters as much as the label. |
| Reflex | A friend announces a readable trap. | The near miss becomes the drama, not a millisecond ranking. |
| Roulette | Each cup gets a promise, threat or protection. | Reveal who protected or betrayed whom. |
| Drawing | Friends write fake titles; the gallery reveals authors. | The failed drawing becomes the party poster. |
| Memory | Friends leave a chain of objects. | Replay their contributions after the break. |
| Rhythm | One player calls a pattern; another answers or contradicts it. | Show the expressive deviations without letting network lag decide. |
| Bluff | An alibi dossier and one short jury question. | Reveal who believed whom, then let the storyteller defend the tale. |
| Bomb | A prop with a chosen recipient and comic debt. | Give the recipient a reaction before the next screen. |

## Art direction: “The night is yours”

**Faces → gestures → game object → interface.** The city and arcade can be a theatrical backdrop for adults: a quiet bistro, homemade TV studio or interrogation room as the game needs. Pixel cats should be occasional troublemakers. Sprites should hesitate, provoke, protect, betray and celebrate.

- **Color and material:** deep aubergine, worn brass, lamp amber, petrol blue and muted coral; coaster paper, scratched enamel and screen-printed poster grain. Bright color marks a moment.
- **Type:** short, cheeky display copy for stage moments; highly readable instructions. Long rules should become acts or spoken play.
- **Motion:** breathing anticipation, then a crisp break at reveal. Confetti belongs to an actual group win.
- **Sound:** optional low ambience, warm short motifs and small silences before judgment. Friends' voices stay first.
- **Controls:** show an action when it matters. A bluff screen protects a secret; a reveal screen becomes a poster the holder can show to everyone.
- **Tone:** tease without humiliation. Bad faith can be a game move; passing remains available. Virtual sips can become water, a challenge or nothing.

The next useful production slice would stage **Bluff → jury question → reveal believers → rematch**, then **Drawing → fake titles → attributed gallery**. Observe several real tables without feeding them the rules. Note spontaneous talk, invented variants, protection of a friend, replay requests and abandoned screens, including failures and contradictory reactions. A synthetic score must never be sold as a “probability of fun.”

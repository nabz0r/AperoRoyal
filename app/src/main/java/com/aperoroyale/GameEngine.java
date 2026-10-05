package com.aperoroyale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.function.LongSupplier;
import org.json.JSONArray;
import org.json.JSONObject;

/** Single authority for turn order, game rotation and scoring. All state is serializable. */
public final class GameEngine {
  public static final String[] TYPES = {
    "QUIZ", "POSE", "BLIND", "REFLEX", "ROULETTE", "DRAW", "MEMORY", "RHYTHM", "BLUFF", "BOMB"
  };

  public static final class Player {
    public String name, language, photo = "";
    public int avatar, score, wins, games, drinks, sips;

    Player(String name, String language, int avatar) {
      this.name = name;
      this.language = language;
      this.avatar = avatar;
    }

    JSONObject json(boolean includePhoto) {
      JSONObject j = new JSONObject();
      try {
        j.put("name", name);
        j.put("language", language);
        j.put("avatar", avatar);
        if (includePhoto) j.put("photo", photo);
        j.put("score", score);
        j.put("wins", wins);
        j.put("games", games);
        j.put("drinks", drinks);
        j.put("sips", sips);
      } catch (Exception ignored) {
      }
      return j;
    }

    static Player from(JSONObject j) {
      Player p =
          new Player(
              j.optString("name", "Player"), j.optString("language", "FR"), j.optInt("avatar"));
      p.score = j.optInt("score");
      p.wins = j.optInt("wins");
      p.games = j.optInt("games");
      p.drinks = j.optInt("drinks");
      p.sips = j.optInt("sips", p.drinks);
      p.photo = j.optString("photo", "");
      return p;
    }
  }

  public final ArrayList<Player> players = new ArrayList<>();
  private final Random random;
  private final LongSupplier clock;

  public GameEngine() {
    this(new Random().nextLong());
  }

  GameEngine(long seed) {
    this(seed, System::currentTimeMillis);
  }

  GameEngine(long seed, LongSupplier clock) {
    random = new Random(seed);
    this.clock = clock;
  }
  private final ArrayList<Integer> deck = new ArrayList<>();
  private final int[] lastVariant = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
  private final ArrayList<ArrayList<Integer>> variantDecks = new ArrayList<>();
  public String screen = "HOME";
  public int active = 0,
      turn = 0,
      game = 0,
      variant = 0,
      progress = 0,
      taps = 0,
      selected = -1,
      target = 0,
      rhythmHits = 0;
  public int wager = 1;
  public int bombNext = 0;
  public int bombVisitedMask = 0;
  public boolean bombAwaitingPass = false;
  public boolean bombCutAttempted = false;
  public int bombCutWire = -1;
  public int roundPoints = 0, roundSips = 0, chosenCup = -1, reflexSeed = 0;
  public long revealUntil = 0, revision = 0;
  public boolean juryPhase = false;
  public int bluffTruth = -1;
  public int[] juryVotes = {};
  public int[] predictions = {};
  /** Private one-tap contribution from every non-actor in the six arcade challenges. */
  public int[] crewChoices = {}, crewPoints = {}, drawGuesses = {};
  /** One named friend can be trusted by the actor in quiz and music rounds. */
  public int trustedFriend = -1;
  private int publicCrewLead = -2;
  public boolean betPlaced = false;
  public long deadline = 0, started = 0;
  /** A short grace period after a real majority has answered; never outlives the hard deadline. */
  public long audienceClosingAt = 0;
  public boolean lastWon = false, drawingReady = false;
  /** A performance may be declined without a drink or a score penalty. */
  public boolean roundPassed = false;
  public String note = "";
  public int[] sequence = {};
  public String screenPromptFr = "", screenPromptEn = "";
  public String[] screenChoicesFr = {}, screenChoicesEn = {};
  public float targetX = 200, targetY = 390;
  public final ArrayList<float[]> strokes = new ArrayList<>();
  public int loserCup = 0;
  /** Public relay order, shown only after the bomb round resolves. */
  public int[] bombRoute = {};
  public String mode = "VOTE";
  /** Device-local menu language; turns continue to use each player's own language. */
  public String menuLanguage = "FR";
  /** Consecutive rounds without an action from every player. */
  public int passiveStreak = 0;
  public int[] offers = {}, votes = {};
  public String ruleOwner = "";
  public int ruleId = -1, voteWinner = -1, freePick = 0;
  public int bonusId = 0;
  public int secretMask = 0, secretId = -1;
  /** Waiting-room discoveries are private until solved; the host validates every move. */
  public int[] hiddenProbe = {}, hiddenKind = {}, hiddenStep = {}, hiddenSeed = {},
      hiddenWonMask = {}, hiddenMistakes = {}, hiddenSolvedTurn = {};
  public String hiddenLastOwner = "";
  public int hiddenLastKind = -1, hiddenLastTurn = -1;
  public String queuedRuleOwner = "";
  public int queuedSecretId = -1;
  public int[] queuedRuleOffers = {};
  public int[] ruleOffers = {};
  public String reportTarget = "", reportBy = "", reportReturn = "VOTE";
  public int[] reportVotes = {};
  public long reportDeadline = 0;
  public boolean rulePenaltyApplied = false;
  /** A late guest watches the current round, then the room votes at the next break. */
  public String pendingJoinName = "", pendingJoinLanguage = "FR", lastJoinName = "";
  public boolean lastJoinAccepted = false;
  public int lastJoinSips = 0;
  public int[] joinVotes = {};
  public long joinVoteDeadline = 0;
  public long joinDecisionUntil = 0;

  public Player current() {
    return players.isEmpty() ? null : players.get(Math.min(active, players.size() - 1));
  }

  public boolean english() {
    if ("HOME".equals(screen) || "LOBBY".equals(screen) || "SETTINGS".equals(screen)
        || "GUIDE".equals(screen) || "STATS".equals(screen) || "LIBRARY".equals(screen))
      return "EN".equals(menuLanguage);
    Player p = current();
    return p == null ? "EN".equals(menuLanguage) : "EN".equals(p.language);
  }

  public String t(String fr, String en) {
    return english() ? en : fr;
  }

  public boolean addPlayer(String name, String language, int avatar) {
    name = name.trim();
    if (name.isEmpty() || players.size() >= 6 || name.length() > 16) return false;
    for (Player p : players) if (p.name.equalsIgnoreCase(name)) return false;
    int chosen = Math.floorMod(avatar, 12);
    players.add(new Player(name, language, chosen));
    screen = "LOBBY";
    return true;
  }

  public boolean requestJoin(String name, String language) {
    name = name.trim();
    if (name.isEmpty() || name.length() > 16 || players.size() >= 6
        || !pendingJoinName.isEmpty()) return false;
    for (Player player : players) if (player.name.equalsIgnoreCase(name)) return false;
    if (!name.equalsIgnoreCase(lastJoinName)) lastJoinSips = 0;
    pendingJoinName = name;
    pendingJoinLanguage = "EN".equals(language) ? "EN" : "FR";
    lastJoinName = "";
    return true;
  }

  public GameEngine.Player nextJoinVoter() {
    if (!"JOIN_VOTE".equals(screen)) return null;
    for (int step = 0; step < players.size(); step++) {
      int i = (active + step) % players.size();
      if (i < joinVotes.length && joinVotes[i] < 0) return players.get(i);
    }
    return null;
  }

  public boolean castJoinVote(String name, boolean yes) {
    int i = indexOf(name);
    if (!"JOIN_VOTE".equals(screen) || i < 0 || i >= joinVotes.length
        || joinVotes[i] >= 0) return false;
    joinVotes[i] = yes ? 1 : 0;
    boolean complete = true;
    for (int vote : joinVotes) if (vote < 0) complete = false;
    if (complete) resolveJoinVote();
    else joinVoteDeadline = clock.getAsLong() + 12000;
    return true;
  }

  public boolean joinVoteTimedOut() {
    if (!"JOIN_VOTE".equals(screen) || clock.getAsLong() < joinVoteDeadline) return false;
    resolveJoinVote();
    return true;
  }

  private void resolveJoinVote() {
    int yes = 0, no = 0;
    for (int vote : joinVotes) { if (vote == 1) yes++; else if (vote == 0) no++; }
    lastJoinName = pendingJoinName;
    lastJoinAccepted = yes > no;
    joinDecisionUntil = clock.getAsLong() + 5000;
    if (lastJoinAccepted) {
      String previous = screen;
      if (!addPlayer(pendingJoinName, pendingJoinLanguage, players.size())) lastJoinAccepted = false;
      screen = previous;
      if (lastJoinAccepted) {
        Player guest = players.get(players.size() - 1);
        guest.sips += lastJoinSips;
        guest.drinks += lastJoinSips;
        extendHiddenForNewPlayer();
      }
    }
    if (!lastJoinAccepted) lastJoinSips++;
    pendingJoinName = "";
    joinVotes = new int[0];
    joinVoteDeadline = 0;
    startSelection();
  }

  private void extendHiddenForNewPlayer() {
    int count = players.size(), old = hiddenKind.length;
    hiddenProbe = java.util.Arrays.copyOf(hiddenProbe, count);
    hiddenKind = java.util.Arrays.copyOf(hiddenKind, count);
    hiddenStep = java.util.Arrays.copyOf(hiddenStep, count);
    hiddenSeed = java.util.Arrays.copyOf(hiddenSeed, count);
    hiddenWonMask = java.util.Arrays.copyOf(hiddenWonMask, count);
    hiddenMistakes = java.util.Arrays.copyOf(hiddenMistakes, count);
    hiddenSolvedTurn = java.util.Arrays.copyOf(hiddenSolvedTurn, count);
    for (int i = old; i < count; i++) {
      hiddenKind[i] = -1;
      hiddenSolvedTurn[i] = -1;
    }
  }

  public void newParty() {
    players.clear();
    active = turn = 0;
    screen = "LOBBY";
    deck.clear();
    variantDecks.clear();
    java.util.Arrays.fill(lastVariant, -1);
    mode = "VOTE";
    passiveStreak = 0;
    offers = votes = new int[0];
    ruleOwner = "";
    ruleId = voteWinner = -1;
    secretMask = 0;
    secretId = -1;
    hiddenProbe = hiddenKind = hiddenStep = hiddenSeed = hiddenWonMask = hiddenMistakes =
        hiddenSolvedTurn = new int[0];
    hiddenLastOwner = "";
    hiddenLastKind = -1;
    hiddenLastTurn = -1;
    queuedRuleOwner = "";
    queuedSecretId = -1;
    queuedRuleOffers = new int[0];
    ruleOffers = new int[0];
    reportTarget = reportBy = "";
    reportReturn = "VOTE";
    reportVotes = new int[0];
    reportDeadline = 0;
    rulePenaltyApplied = false;
    pendingJoinName = lastJoinName = "";
    pendingJoinLanguage = "FR";
    lastJoinAccepted = false;
    lastJoinSips = 0;
    joinVotes = new int[0];
    joinVoteDeadline = 0;
    joinDecisionUntil = 0;
    deadline = 0;
    audienceClosingAt = 0;
    juryPhase = false;
    bluffTruth = -1;
    roundPassed = false;
    bombRoute = new int[0];
    juryVotes = predictions = crewChoices = crewPoints = drawGuesses = new int[0];
    trustedFriend = -1;
    publicCrewLead = -2;
    revision = 0;
  }

  public void begin() {
    if (players.size() < 2) return;
    active = 0;
    turn = 0;
    initHidden();
    startSelection();
  }

  private void initHidden() {
    int count = players.size();
    hiddenProbe = new int[count];
    hiddenKind = new int[count];
    java.util.Arrays.fill(hiddenKind, -1);
    hiddenStep = new int[count];
    hiddenSeed = new int[count];
    hiddenWonMask = new int[count];
    hiddenMistakes = new int[count];
    hiddenSolvedTurn = new int[count];
    java.util.Arrays.fill(hiddenSolvedTurn, -1);
  }

  public void startSelection() {
    if (!pendingJoinName.isEmpty()) {
      screen = "JOIN_VOTE";
      joinVotes = new int[players.size()];
      java.util.Arrays.fill(joinVotes, -1);
      joinVoteDeadline = clock.getAsLong() + 12000;
      return;
    }
    deadline = 0;
    audienceClosingAt = 0;
    started = clock.getAsLong();
    voteWinner = -1;
    if (!queuedRuleOwner.isEmpty()) {
      ruleOwner = queuedRuleOwner;
      secretId = queuedSecretId;
      ruleOffers = queuedRuleOffers;
      ruleId = -1;
      queuedRuleOwner = "";
      queuedSecretId = -1;
      queuedRuleOffers = new int[0];
    }
    if ("TURBO".equals(mode) && !ruleOwner.isEmpty() && ruleId < 0) {
      screen = "RULE_PICK";
      deadline = clock.getAsLong() + 15000;
      return;
    }
    bonusId = random.nextInt(4);
    if ("FREE".equals(mode)) {
      screen = "LIBRARY";
      return;
    }
    if (deck.isEmpty()) {
      for (int i = 0; i < TYPES.length; i++) deck.add(i);
      Collections.shuffle(deck, random);
    }
    ArrayList<Integer> pool = new ArrayList<>(deck);
    if (turn > 0) pool.remove(Integer.valueOf(game));
    if (pool.size() < 3)
      for (int i = 0; i < TYPES.length; i++)
        if (!pool.contains(i) && (turn == 0 || i != game)) pool.add(i);
    Collections.shuffle(pool, random);
    if ("TURBO".equals(mode)) {
      offers = new int[0];
      votes = new int[0];
      startNext(pool.get(0));
      return;
    }
    offers = new int[] {pool.get(0), pool.get(1), pool.get(2)};
    votes = new int[players.size()];
    java.util.Arrays.fill(votes, -1);
    screen = "VOTE";
    deadline = clock.getAsLong() + 18000;
  }

  public int indexOf(String name) {
    for (int i = 0; i < players.size(); i++)
      if (players.get(i).name.equals(name)) return i;
    return -1;
  }

  public boolean castVote(String name, int choice) {
    int i = indexOf(name);
    if (!"VOTE".equals(screen) || i < 0 || i >= votes.length || votes[i] >= 0 || choice < 0 || choice >= 3) return false;
    votes[i] = choice;
    // Give the next voter time to act without making the whole room wait indefinitely.
    deadline = clock.getAsLong() + 12000;
    completeVoteIfReady();
    return true;
  }

  /** Explicit abstention lets a shared-phone group continue when someone steps away. */
  public boolean skipParticipation(String name) {
    int i = indexOf(name);
    if (i < 0) return false;
    if ("VOTE".equals(screen) && i < votes.length && votes[i] < 0) {
      votes[i] = 3;
      deadline = clock.getAsLong() + 12000;
      completeVoteIfReady();
      return true;
    }
    if ("CREW".equals(screen) && i != active && i < crewChoices.length
        && crewChoices[i] == -1) {
      crewChoices[i] = -2;
      if (crewReady()) startGame();
      else deadline = clock.getAsLong() + ("TURBO".equals(mode) ? 8000 : 12000);
      return true;
    }
    if ("PREDICT".equals(screen) && i != active && i < predictions.length
        && predictions[i] == -1) {
      predictions[i] = 2;
      if (predictionsReady()) startGame();
      return true;
    }
    if ("GAME".equals(screen) && juryPhase && i != active && i < juryVotes.length
        && juryVotes[i] == -1) {
      juryVotes[i] = 2;
      return true;
    }
    if ("GAME".equals(screen) && game == 5 && drawingReady && i != active
        && i < drawGuesses.length && drawGuesses[i] == -1) {
      drawGuesses[i] = 4;
      return true;
    }
    if ("RULE_VOTE".equals(screen) && i < reportVotes.length && reportVotes[i] == -1) {
      reportVotes[i] = 2;
      if (ruleVoteReady()) resolveRuleVote();
      return true;
    }
    return false;
  }

  /** Missing votes become abstentions. An unanswered secret rule uses its first offered option. */
  public boolean voteTimedOut(long now) {
    if (!"VOTE".equals(screen) || deadline <= 0 || now < deadline) return false;
    if (!ruleOwner.isEmpty() && ruleId < 0)
      ruleId = ruleOffers.length == 0 ? 0 : ruleOffers[0];
    for (int i = 0; i < votes.length; i++) if (votes[i] < 0) votes[i] = 3;
    completeVoteIfReady();
    return true;
  }

  public boolean rulePickTimedOut(long now) {
    if (!"RULE_PICK".equals(screen) || deadline <= 0 || now < deadline) return false;
    return chooseRule(ruleOwner, ruleOffers.length == 0 ? 0 : ruleOffers[0]);
  }

  public int voteCount() {
    int n = 0;
    for (int v : votes) if (v >= 0) n++;
    return n;
  }

  private void completeVoteIfReady() {
    if (voteCount() != players.size() || (!ruleOwner.isEmpty() && ruleId < 0)) return;
    int[] counts = new int[3];
    for (int v : votes) if (v >= 0 && v < counts.length) counts[v]++;
    int max = Math.max(counts[0], Math.max(counts[1], counts[2]));
    ArrayList<Integer> tied = new ArrayList<>();
    for (int i = 0; i < 3; i++) if (counts[i] == max) tied.add(i);
    voteWinner = tied.get(random.nextInt(tied.size()));
    startNext(offers[voteWinner]);
  }

  /** Only a remote spectator who has finished their real contribution may hunt a secret. */
  public boolean hiddenWaiting(String name) {
    int i = indexOf(name);
    if (i < 0 || hiddenKind.length != players.size()) return false;
    if ("VOTE".equals(screen)) return i < votes.length && votes[i] >= 0;
    if ("LIBRARY".equals(screen)) return true;
    if (i == active) return false;
    if ("RESULT".equals(screen)) return true;
    if ("CREW".equals(screen)) return i < crewChoices.length && crewChoices[i] >= 0;
    if (!"GAME".equals(screen) || game == 9) return false;
    if (juryPhase) return i < juryVotes.length && juryVotes[i] >= 0;
    if (game == 5 && drawingReady)
      return i < drawGuesses.length && drawGuesses[i] >= 0;
    return true;
  }

  /** -1 probes the small alley hint; 0..3 are the four secret controls. */
  public boolean hiddenTap(String name, int slot) {
    if (!hiddenWaiting(name) || slot < -1 || slot > 3) return false;
    int i = indexOf(name);
    if (hiddenKind[i] < 0) {
      if (slot != -1 || hiddenWonMask[i] == 7 || hiddenSolvedTurn[i] == turn) return false;
      if (++hiddenProbe[i] < 3) return true;
      hiddenProbe[i] = 0;
      for (int offset = 0; offset < 3; offset++) {
        int kind = (turn + i + offset) % 3;
        if ((hiddenWonMask[i] & (1 << kind)) == 0) {
          hiddenKind[i] = kind;
          break;
        }
      }
      hiddenStep[i] = 0;
      hiddenMistakes[i] = 0;
      hiddenSeed[i] = random.nextInt(256);
      return true;
    }
    if (slot < 0) return false;
    if (slot != hiddenTarget(i)) {
      hiddenMistakes[i]++;
      if (hiddenKind[i] == 1) hiddenStep[i] = 0;
      return true;
    }
    if (++hiddenStep[i] < 4) return true;
    int kind = hiddenKind[i];
    hiddenWonMask[i] |= 1 << kind;
    hiddenKind[i] = -1;
    hiddenStep[i] = 0;
    hiddenProbe[i] = 0;
    hiddenLastOwner = name;
    hiddenLastKind = kind;
    hiddenLastTurn = turn;
    hiddenSolvedTurn[i] = turn;
    int secret = kind == 0 ? 0 : 10 + kind;
    boolean firstInRoom = (secretMask & (1 << secret)) == 0;
    unlockSecret(secret, name);
    if (firstInRoom && ruleId >= 0 && queuedRuleOwner.isEmpty()) {
      queuedRuleOwner = name;
      queuedSecretId = secret;
      int base = (secret * 3) % 10;
      queuedRuleOffers = new int[] {base, (base + 3) % 10, (base + 7) % 10};
    }
    return true;
  }

  public int hiddenTarget(int i) {
    if (i < 0 || i >= hiddenKind.length || hiddenKind[i] < 0) return -1;
    int step = hiddenStep[i];
    int seed = hiddenSeed[i];
    return switch (hiddenKind[i]) {
      case 0 -> (seed + step * 3) & 3; // cat chases across four rooftops
      case 1 -> (seed >>> (step * 2)) & 3; // read the four neon paw glyphs in order
      default -> ((seed >>> (step * 2)) & 3) ^ 1; // tap across the vertical mirror
    };
  }

  private void unlockSecret(int id, String owner) {
    int bit = 1 << id;
    if ((secretMask & bit) != 0) return;
    secretMask |= bit;
    if (ruleId >= 0 || !ruleOwner.isEmpty()) return;
    ruleOwner = owner;
    secretId = id;
    int base = (id * 3) % 10;
    ruleOffers = new int[] {base, (base + 3) % 10, (base + 7) % 10};
  }

  public boolean chooseRule(String name, int id) {
    if (!name.equals(ruleOwner) || ruleId >= 0 || id < 0 || id >= 10) return false;
    boolean offered = false;
    for (int value : ruleOffers) if (value == id) offered = true;
    if (!offered) return false;
    ruleId = id;
    if ("VOTE".equals(screen)) deadline = clock.getAsLong() + 18000;
    if ("VOTE".equals(screen)) completeVoteIfReady();
    else if ("RULE_PICK".equals(screen)) startSelection();
    return true;
  }

  public void startNext(int chosen) {
    if (deck.isEmpty()) for (int i = 0; i < TYPES.length; i++) deck.add(i);
    game = chosen;
    passiveStreak = 0; // Every challenge now gives each friend a game action.
    deck.remove(Integer.valueOf(chosen));
    int variants = variantCount(chosen);
    while (variantDecks.size() < TYPES.length) variantDecks.add(new ArrayList<>());
    ArrayList<Integer> variantDeck = variantDecks.get(chosen);
    if (variantDeck.isEmpty()) {
      for (int i = 0; i < variants; i++) variantDeck.add(i);
      Collections.shuffle(variantDeck, random);
      // The first card of a fresh pack must differ from the previous pack's last card.
      if (variants > 1 && variantDeck.get(variantDeck.size() - 1) == lastVariant[chosen])
        Collections.swap(variantDeck, variantDeck.size() - 1, 0);
    }
    variant = variantDeck.remove(variantDeck.size() - 1);
    lastVariant[chosen] = variant;
    target = random.nextInt(4);
    progress = chosen == 7 ? -1 : 0;
    taps = 0;
    selected = -1;
    rhythmHits = 0;
    wager = 1;
    roundPoints = roundSips = 0;
    chosenCup = -1;
    revealUntil = 0;
    juryPhase = false;
    bluffTruth = -1;
    roundPassed = false;
    juryVotes = new int[0];
    predictions = new int[0];
    crewChoices = crewPoints = drawGuesses = new int[0];
    trustedFriend = -1;
    publicCrewLead = -2;
    betPlaced = false;
    bombNext = active;
    bombRoute = new int[] {active};
    bombVisitedMask = 0;
    bombAwaitingPass = false;
    bombCutAttempted = false;
    bombCutWire = -1;
    drawingReady = false;
    note = "";
    strokes.clear();
    reflexSeed = random.nextInt();
    targetX = reflexX(0);
    targetY = reflexY(0);
    loserCup = random.nextInt(6);
    int count = 4 + Math.min(3, turn / 5);
    sequence = chosen == 6 ? memorySequence(random, variant, count) : new int[0];
    screen = "TRANSITION";
    started = clock.getAsLong();
    deadline = 0;
  }

  private static int variantCount(int chosen) {
    return switch (chosen) {
      case 0 -> QUIZ_FR.length;
      case 1 -> POSES.length;
      case 2 -> TUNES.length * 4; // Six original contours in four musical keys.
      case 5 -> DRAW.length;
      case 8 -> BLUFF.length;
      default -> 6;
    };
  }

  static int[] memorySequence(Random rng, int pattern, int count) {
    int[] result = new int[count];
    if (count == 0) return result;
    switch (Math.floorMod(pattern, 6)) {
      case 1 -> { // mirror
        for (int i = 0; i < (count + 1) / 2; i++) result[i] = rng.nextInt(4);
        for (int i = (count + 1) / 2; i < count; i++) result[i] = result[count - 1 - i];
      }
      case 2 -> { // alternate two symbols
        int first = rng.nextInt(4), second = (first + 1 + rng.nextInt(3)) % 4;
        for (int i = 0; i < count; i++) result[i] = i % 2 == 0 ? first : second;
      }
      case 3 -> { // every symbol differs from the previous one
        result[0] = rng.nextInt(4);
        for (int i = 1; i < count; i++)
          result[i] = (result[i - 1] + 1 + rng.nextInt(3)) % 4;
      }
      case 4 -> { // color wheel, clockwise or anticlockwise
        int first = rng.nextInt(4), step = rng.nextBoolean() ? 1 : 3;
        for (int i = 0; i < count; i++) result[i] = (first + i * step) % 4;
      }
      case 5 -> { // pairs of the same symbol
        int color = rng.nextInt(4);
        for (int i = 0; i < count; i++) {
          if (i > 0 && i % 2 == 0) color = (color + 1 + rng.nextInt(3)) % 4;
          result[i] = color;
        }
      }
      default -> {
        for (int i = 0; i < count; i++) result[i] = rng.nextInt(4);
      }
    }
    return result;
  }

  public void enterGame() {
    if (!"TRANSITION".equals(screen)) return;
    screen = "HANDOFF";
    started = clock.getAsLong();
  }

  public void readyTurn() {
    if (!"HANDOFF".equals(screen)) return;
    screen = "BET";
    started = clock.getAsLong();
  }

  public boolean placeBet(int sips) {
    if (!"BET".equals(screen) || sips < 1 || sips > 3) return false;
    wager = sips;
    betPlaced = true;
    predictions = new int[players.size()];
    java.util.Arrays.fill(predictions, -1);
    predictions[active] = 2;
    // The jury, drawing gallery and bomb have their own group interaction.
    if (game == 1 || game == 5 || game == 8 || game == 9) {
      startGame();
      return true;
    }
    crewChoices = new int[players.size()];
    java.util.Arrays.fill(crewChoices, -1);
    crewChoices[active] = 2;
    screen = "CREW";
    deadline = clock.getAsLong() + ("TURBO".equals(mode) ? 8000 : 12000);
    return true;
  }

  public int crewOptionCount() { return game == 4 ? 6 : game == 7 ? 8 : 4; }

  public boolean crewPick(String name, int choice) {
    int i = indexOf(name);
    if (!"CREW".equals(screen) || i < 0 || i == active || i >= crewChoices.length
        || crewChoices[i] >= 0 || choice < 0 || choice >= crewOptionCount()) return false;
    crewChoices[i] = choice;
    if (crewReady()) startGame();
    else deadline = clock.getAsLong() + ("TURBO".equals(mode) ? 8000 : 12000);
    return true;
  }

  public int crewCount() {
    int count = 0;
    for (int i = 0; i < crewChoices.length; i++)
      if (i != active && crewChoices[i] >= 0) count++;
    return count;
  }

  public boolean crewReady() {
    if (!"CREW".equals(screen)) return false;
    for (int i = 0; i < crewChoices.length; i++)
      if (i != active && crewChoices[i] == -1) return false;
    return true;
  }

  public boolean crewTimedOut() {
    if (!"CREW".equals(screen) || clock.getAsLong() < deadline) return false;
    startGame();
    return true;
  }

  public int crewChoiceCount(int choice) {
    int count = 0;
    for (int i = 0; i < crewChoices.length; i++)
      if (i != active && crewChoices[i] == choice) count++;
    return count;
  }

  /** A strict lead gives the actor a useful, understandable crowd option. */
  public int crewLead() {
    if (publicCrewLead >= -1) return publicCrewLead;
    int best = -1, votes = 0;
    for (int choice = 0; choice < crewOptionCount(); choice++) {
      int n = crewChoiceCount(choice);
      if (n > votes) { best = choice; votes = n; }
      else if (n == votes && n > 0) best = -1;
    }
    return best;
  }

  /** Select a visible contributor without revealing their private answer. */
  public int featuredFriend() {
    for (int step = 1; step < players.size(); step++) {
      int i = (active + step) % players.size();
      if (i < crewChoices.length && crewChoices[i] >= 0) return i;
    }
    return -1;
  }

  /** The next friend in turn order asks the bluff's spoken follow-up. */
  public int crossExaminer() {
    return players.size() < 2 ? -1 : (active + 1 + turn % (players.size() - 1)) % players.size();
  }

  public boolean predict(String name, boolean win) {
    int i = indexOf(name);
    if (!"PREDICT".equals(screen) || i < 0 || i == active
        || i >= predictions.length || predictions[i] >= 0) return false;
    predictions[i] = win ? 1 : 0;
    if (predictionsReady()) startGame();
    return true;
  }

  public boolean predictionsReady() {
    if (!"PREDICT".equals(screen)) return false;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] < 0) return false;
    return true;
  }

  public boolean predictionTimedOut() {
    if (!"PREDICT".equals(screen) || clock.getAsLong() < deadline) return false;
    startGame();
    return true;
  }

  private void startGame() {
    if ("CREW".equals(screen)) {
      if (game == 6) {
        int length = Math.max(players.size() - 1, 4 + Math.min(3, turn / 5));
        int[] original = sequence;
        sequence = new int[length];
        int placed = 0;
        for (int step = 1; step < players.size(); step++) {
          int choice = crewChoices[(active + step) % players.size()];
          if (choice >= 0) sequence[placed++] = choice;
        }
        for (int i = placed; i < length; i++) sequence[i] = original[i % original.length];
      }
      if (game == 3) {
        targetX = reflexX(0);
        targetY = reflexY(0);
      }
    }
    screen = "GAME";
    resumeGame();
  }

  public void resumeGame() {
    if (!"GAME".equals(screen)) return;
    started = clock.getAsLong();
    audienceClosingAt = 0;
    if (juryPhase) {
      deadline = started + 20000;
      return;
    }
    int seconds =
        switch (game) {
          case 0 -> 16;
          case 1 -> 40;
          case 2 -> 28;
          case 3 -> 15;
          case 4 -> 10;
          case 5 -> drawingReady ? 12 + 8 * Math.max(0, players.size() - 2) : 50;
          case 6 -> 26;
          case 7 -> 16;
          case 8 -> 40;
          case 9 -> variant >= 3 ? 24 : 30;
          default -> 0;
        };
    if ("TURBO".equals(mode) && seconds > 0)
      seconds = Math.max(10, seconds - (players.size() <= 2 ? 2 : 4));
    if (bonusId == 3 && seconds > 0) seconds += 5;
    if (seconds > 0 && game != 7) seconds += Math.min(6, supportCount() * 2);
    deadline = seconds == 0 ? 0 : started + seconds * 1000L;
  }

  public int supportCount() {
    int count = 0;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] == 1) count++;
    return count;
  }

  public int challengeCount() {
    int count = 0;
    for (int i = 0; i < predictions.length; i++)
      if (i != active && predictions[i] == 0) count++;
    return count;
  }

  public int winPoints() {
    return (ruleId == 0 ? 2 : 1) * (100 + 50 * (wager - 1)
        + (bonusId == 1 ? 50 : 0)) + challengeCount() * 25
        + (game == 4 ? 10 * crewChoiceCount(chosenCup) : 0);
  }

  public int lossSips() {
    return ruleId == 1 ? 0 : Math.max(0, wager - (bonusId == 2 ? 1 : 0)
        - (supportCount() > 0 ? 1 : 0)
        - (game == 4 && chosenCup >= 0 && crewChoiceCount(chosenCup) > 0 ? 1 : 0));
  }

  public float reflexX(int step) {
    Random pattern = new Random(reflexSeed ^ (0x9e3779b9L * (step + 1)));
    int quadrant = reflexCrewQuadrant(step);
    if (quadrant >= 0) return (quadrant % 2 == 0 ? 77 : 208) + pattern.nextInt(115);
    return 77 + pattern.nextInt(246);
  }

  private int reflexCrewQuadrant(int step) {
    int found = 0;
    for (int offset = 1; offset < players.size(); offset++) {
      int i = (active + offset) % players.size();
      if (i >= crewChoices.length || crewChoices[i] < 0) continue;
      if (found++ == step) return crewChoices[i];
    }
    return -1;
  }

  public int reflexGoal() {
    return new int[] {8, 10, 12, 9, 11, 10}[Math.floorMod(variant, 6)];
  }

  public float reflexHitRadius() {
    return new int[] {57, 50, 43, 53, 46, 49}[Math.floorMod(variant, 6)];
  }

  public float reflexY(int step) {
    Random pattern = new Random((reflexSeed * 31L) ^ (0x6a09e667L * (step + 1)));
    int quadrant = reflexCrewQuadrant(step);
    if (quadrant >= 0) return (quadrant < 2 ? 380 : 481) + pattern.nextInt(97);
    return 380 + pattern.nextInt(198);
  }

  public boolean reflexTap(String name, int expectedStep) {
    if (!"GAME".equals(screen) || game != 3 || current() == null
        || !current().name.equals(name) || expectedStep != taps || taps >= reflexGoal()
        || (deadline > 0 && clock.getAsLong() > deadline)) return false;
    taps++;
    targetX = reflexX(taps);
    targetY = reflexY(taps);
    return true;
  }

  public boolean rhythmTap(String name, int beat) {
    return rhythmTap(name, beat, false);
  }

  public boolean rhythmTap(String name, int beat, boolean remote) {
    if (!"GAME".equals(screen) || game != 7 || current() == null
        || !current().name.equals(name) || rhythmHits >= 4 || beat <= progress
        || Math.floorMod(beat, 8) != rhythmPattern()[rhythmHits]
        || !RhythmClock.accepts(started, clock.getAsLong(), beat, remote)
        || (deadline > 0 && clock.getAsLong() > deadline)) return false;
    progress = beat;
    rhythmHits++;
    return true;
  }

  public int[] rhythmPattern() {
    if (crewCount() > 0) {
      int[] beats = new int[8];
      for (int i = 0; i < 8; i++) beats[i] = i;
      for (int i = 0; i < beats.length; i++)
        for (int j = i + 1; j < beats.length; j++)
          if (crewChoiceCount(beats[j]) > crewChoiceCount(beats[i])
              || (crewChoiceCount(beats[j]) == crewChoiceCount(beats[i])
                  && Math.floorMod(beats[j] - variant, 8) < Math.floorMod(beats[i] - variant, 8))) {
            int swap = beats[i]; beats[i] = beats[j]; beats[j] = swap;
          }
      int[] result = java.util.Arrays.copyOf(beats, 4);
      java.util.Arrays.sort(result);
      return result;
    }
    int[][] patterns = {
        {0, 1, 2, 3}, {0, 2, 4, 6}, {1, 2, 4, 5},
        {0, 1, 3, 5}, {0, 3, 4, 7}, {1, 3, 5, 7}
    };
    return patterns[Math.floorMod(variant, patterns.length)];
  }

  public boolean selectCup(int cup) {
    if (!"GAME".equals(screen) || game != 4 || cup < 0 || cup >= 6 || chosenCup >= 0) return false;
    chosenCup = cup;
    revealUntil = clock.getAsLong() + 1300;
    deadline = 0; // Let the selected cup finish its reveal even if picked at the last second.
    return true;
  }

  public boolean cupIsCursed(int cup) {
    if (cup < 0 || cup >= 6) return false;
    int[][] layouts = {
        {0, 1, 2, 3, 4, 5}, {0, 2, 4, 1, 3, 5}, {0, 3, 1, 4, 2, 5},
        {0, 5, 4, 3, 2, 1}, {0, 1, 2, 5, 4, 3}, {0, 2, 3, 5, 1, 4}
    };
    int[] layout = layouts[Math.floorMod(variant, layouts.length)];
    for (int i = 0; i < Math.min(wager, 6); i++)
      if ((loserCup + layout[i]) % 6 == cup) return true;
    return false;
  }

  public boolean cupIsSafe() { return chosenCup >= 0 && !cupIsCursed(chosenCup); }

  public boolean lockAnswer(int choice) {
    if (!"GAME".equals(screen) || (game != 0 && game != 2)
        || choice < 0 || choice >= 4 || selected >= 0) return false;
    selected = choice;
    return true;
  }

  public boolean trustFriend(String actor, int friend) {
    if ((game != 0 && game != 2) || current() == null || !current().name.equals(actor)
        || friend != featuredFriend() || friend < 0 || friend >= crewChoices.length
        || crewChoices[friend] < 0 || crewChoices[friend] > 3) return false;
    if (!lockAnswer(crewChoices[friend])) return false;
    trustedFriend = friend;
    return true;
  }

  public boolean passPerformance(String name) {
    if (!"GAME".equals(screen) || (game != 1 && game != 8) || juryPhase
        || current() == null || !current().name.equals(name)) return false;
    roundPassed = true;
    return true;
  }

  public boolean beginJury() {
    if (!"GAME".equals(screen) || (game != 1 && game != 8) || juryPhase
        || (game == 8 && bluffTruth < 0)) return false;
    juryPhase = true;
    juryVotes = new int[players.size()];
    java.util.Arrays.fill(juryVotes, -1);
    if (active < juryVotes.length) juryVotes[active] = 2;
    deadline = clock.getAsLong() + 20000;
    audienceClosingAt = 0;
    return true;
  }

  public boolean chooseBluffTruth(String name, boolean trueStory) {
    if (!"GAME".equals(screen) || game != 8 || juryPhase || bluffTruth >= 0
        || current() == null || !current().name.equals(name)) return false;
    bluffTruth = trueStory ? 1 : 0;
    return beginJury();
  }

  public boolean castJury(String name, boolean yes) {
    int i = indexOf(name);
    if (!"GAME".equals(screen) || !juryPhase || i < 0 || i == active
        || i >= juryVotes.length || juryVotes[i] >= 0
        || (deadline > 0 && clock.getAsLong() >= visibleDeadline())) return false;
    juryVotes[i] = yes ? 1 : 0;
    scheduleAudienceClose(juryAnsweredCount());
    return true;
  }

  public int juryAnsweredCount() {
    int count = 0;
    for (int i = 0; i < juryVotes.length; i++)
      if (i != active && (juryVotes[i] == 0 || juryVotes[i] == 1)) count++;
    return count;
  }

  private void scheduleAudienceClose(int answered) {
    int audience = players.size() - 1;
    if (audience <= 1 || answered < (audience + 1) / 2 || audienceClosingAt > 0) return;
    long close = clock.getAsLong() + 7000;
    if (close < deadline) audienceClosingAt = close;
  }

  public boolean audienceReadyToClose() {
    return "GAME".equals(screen) && audienceClosingAt > 0
        && clock.getAsLong() >= audienceClosingAt;
  }

  public long visibleDeadline() {
    return audienceClosingAt > 0 ? Math.min(deadline, audienceClosingAt) : deadline;
  }

  public boolean juryComplete() {
    if (!juryPhase) return false;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] < 0) return false;
    return true;
  }

  public boolean juryVerdict() {
    int yes = 0;
    int no = 0;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] == 1) yes++;
    for (int i = 0; i < juryVotes.length; i++) if (i != active && juryVotes[i] == 0) no++;
    if (game == 8) return yes + no > 0 && (yes > no ? 1 : 0) != bluffTruth;
    return yes > no;
  }

  public boolean beginDrawGuess() {
    if (!"GAME".equals(screen) || game != 5 || drawingReady) return false;
    drawingReady = true;
    drawGuesses = new int[players.size()];
    java.util.Arrays.fill(drawGuesses, -1);
    drawGuesses[active] = 2;
    resumeGame();
    return true;
  }

  public boolean drawGuess(String name, int choice) {
    int i = indexOf(name);
    if (!"GAME".equals(screen) || game != 5 || !drawingReady || i < 0 || i == active
        || i >= drawGuesses.length || drawGuesses[i] >= 0 || choice < 0 || choice > 3
        || (deadline > 0 && clock.getAsLong() >= visibleDeadline())) return false;
    drawGuesses[i] = choice;
    scheduleAudienceClose(drawValidGuessCount());
    return true;
  }

  public int drawValidGuessCount() {
    int count = 0;
    for (int i = 0; i < drawGuesses.length; i++)
      if (i != active && drawGuesses[i] >= 0 && drawGuesses[i] < 4) count++;
    return count;
  }

  public int drawAnsweredCount() {
    int count = 0;
    for (int i = 0; i < drawGuesses.length; i++)
      if (i != active && drawGuesses[i] >= 0) count++;
    return count;
  }

  public boolean drawGuessComplete() { return drawAnsweredCount() >= players.size() - 1; }

  public int drawCorrectCount() {
    int count = 0;
    for (int i = 0; i < drawGuesses.length; i++)
      if (i != active && drawGuesses[i] == target) count++;
    return count;
  }

  public boolean drawWin() {
    return drawCorrectCount() > 0 && drawCorrectCount() * 2 >= drawValidGuessCount();
  }

  public void finish(boolean won) {
    if (!"GAME".equals(screen)) return;
    audienceClosingAt = 0;
    lastWon = won;
    Player p = current();
    if (p == null) return;
    if (!roundPassed) p.games++;
    roundPoints = 0;
    roundSips = 0;
    if (roundPassed) {
      // A graceful exit is a completed turn, never a drinking challenge.
    } else if (won) {
      p.wins++;
      int speed = (game == 0 || game == 2) && deadline > 0
          ? Math.min(80, (int) Math.max(0, (deadline - clock.getAsLong()) / 1000L) * 5) : 0;
      roundPoints = winPoints() + speed + (game == 9 && bombCutAttempted ? 100 : 0);
      p.score += roundPoints;
    } else {
      if (lossSips() > 0) {
        p.drinks++;
        roundSips = lossSips();
        p.sips += roundSips;
      }
      roundPoints = -Math.min(p.score, 25 * wager);
      p.score += roundPoints;
    }
    for (int i = 0; !roundPassed && i < players.size() && i < predictions.length; i++) {
      if (i == active || predictions[i] < 0 || predictions[i] > 1) continue;
      Player spectator = players.get(i);
      if ((predictions[i] == 1) == won) spectator.score += predictions[i] == 0 ? 50 : 35;
      else if (ruleId != 1) { spectator.drinks++; spectator.sips++; }
    }
    crewPoints = new int[players.size()];
    for (int i = 0; !roundPassed && i < players.size(); i++) {
      if (i == active) continue;
      boolean chose = i < crewChoices.length && crewChoices[i] >= 0;
      boolean guessed = i < drawGuesses.length && drawGuesses[i] >= 0;
      if (!chose && !guessed) continue;
      boolean correct = (game == 0 || game == 2) && crewChoices[i] == target
          || game == 5 && drawGuesses[i] == target;
      crewPoints[i] = correct ? 35 : won ? 20 : 10;
      players.get(i).score += crewPoints[i];
    }
    if (won && trustedFriend >= 0 && trustedFriend < crewPoints.length
        && crewChoices[trustedFriend] == target) {
      roundPoints += 25;
      p.score += 25;
      crewPoints[trustedFriend] += 25;
      players.get(trustedFriend).score += 25;
    }
    if (won && !roundPassed) {
      boolean secret = switch (game) {
        case 0 -> deadline - clock.getAsLong() >= 8000;
        case 1, 8 -> juryPhase && juryComplete() && juryVerdict();
        case 2 -> deadline - clock.getAsLong() >= 4000;
        case 3 -> taps >= reflexGoal() && deadline - clock.getAsLong() >= 4000;
        case 4 -> wager == 3;
        case 5 -> strokes.size() >= 12;
        case 6 -> sequence.length >= 5;
        case 7 -> rhythmHits >= 4;
        case 9 -> taps >= 8;
        default -> false;
      };
      if (secret) unlockSecret(game + 1, p.name);
    }
    screen = "RESULT";
    deadline = 0;
    started = clock.getAsLong();
  }

  public boolean bombTap(String name) {
    if (!"GAME".equals(screen) || game != 9 || players.isEmpty()
        || bombAwaitingPass || taps >= bombGoal()
        || !players.get(bombNext).name.equals(name)) return false;
    taps++;
    if (taps % bombTapsPerHolder() == 0 && taps < bombGoal()) {
      bombVisitedMask |= 1 << bombNext;
      bombAwaitingPass = true;
    }
    return true;
  }

  public boolean canPassBombTo(int targetPlayer) {
    if (targetPlayer < 0 || targetPlayer >= players.size() || targetPlayer == bombNext) return false;
    int all = (1 << players.size()) - 1;
    return (bombVisitedMask & all) == all || (bombVisitedMask & (1 << targetPlayer)) == 0;
  }

  public boolean bombPass(String name, int targetPlayer) {
    if (!"GAME".equals(screen) || game != 9 || !bombAwaitingPass
        || !players.get(bombNext).name.equals(name) || !canPassBombTo(targetPlayer)) return false;
    bombNext = targetPlayer;
    bombRoute = java.util.Arrays.copyOf(bombRoute, bombRoute.length + 1);
    bombRoute[bombRoute.length - 1] = targetPlayer;
    bombAwaitingPass = false;
    return true;
  }

  public boolean canDefuseBomb() {
    return "GAME".equals(screen) && game == 9 && bombAwaitingPass
        && (bombVisitedMask & ((1 << players.size()) - 1)) == (1 << players.size()) - 1;
  }

  /** -1 means invalid, 0 is a failed cut and 1 is a successful cut. */
  public int bombCut(String name, int wire) {
    if (!canDefuseBomb() || bombCutAttempted || wire < 0 || wire > 1
        || !players.get(bombNext).name.equals(name)) return -1;
    bombCutAttempted = true;
    bombCutWire = wire;
    return wire == Math.floorMod(loserCup + taps / bombTapsPerHolder(), 2) ? 1 : 0;
  }

  public int bombGoal() {
    return Math.max(8, bombTapsPerHolder() * players.size());
  }

  public int bombTapsPerHolder() {
    return 1 + Math.floorMod(variant, 3);
  }

  public void advance() {
    if (!"RESULT".equals(screen)) return;
    active = Math.floorMod(active + (ruleId == 2 ? -1 : 1), players.size());
    turn++;
    startSelection();
  }

  public boolean reportRule(String reporter, String targetName) {
    int reporterIndex = indexOf(reporter), targetIndex = indexOf(targetName);
    if (!("VOTE".equals(screen) || "LIBRARY".equals(screen)) || ruleId < 3
        || reporterIndex < 0 || targetIndex < 0 || reporterIndex == targetIndex) return false;
    reportReturn = screen;
    reportBy = reporter;
    reportTarget = targetName;
    reportVotes = new int[players.size()];
    java.util.Arrays.fill(reportVotes, -1);
    if (players.size() > 2) reportVotes[targetIndex] = 2;
    reportVotes[reporterIndex] = 1;
    reportDeadline = clock.getAsLong() + 10000;
    rulePenaltyApplied = false;
    screen = "RULE_VOTE";
    return true;
  }

  public boolean castRuleVote(String name, boolean yes) {
    int i = indexOf(name);
    if (!"RULE_VOTE".equals(screen) || i < 0 || i >= reportVotes.length
        || reportVotes[i] >= 0) return false;
    reportVotes[i] = yes ? 1 : 0;
    if (ruleVoteReady()) resolveRuleVote();
    return true;
  }

  public boolean ruleVoteReady() {
    if (!"RULE_VOTE".equals(screen)) return false;
    for (int value : reportVotes) if (value < 0) return false;
    return true;
  }

  public boolean ruleVoteTimedOut() {
    if (!"RULE_VOTE".equals(screen) || clock.getAsLong() < reportDeadline) return false;
    resolveRuleVote();
    return true;
  }

  private void resolveRuleVote() {
    int yes = 0, no = 0;
    for (int value : reportVotes) { if (value == 1) yes++; else if (value == 0) no++; }
    if (yes > no && ruleId != 1) {
      int i = indexOf(reportTarget);
      if (i >= 0) { players.get(i).sips++; players.get(i).drinks++; rulePenaltyApplied = true; }
    }
    screen = reportReturn;
  }

  public void checkTimeout() {
    if ("GAME".equals(screen) && deadline > 0 && clock.getAsLong() > deadline) {
      if (juryPhase) finish(juryVerdict());
      else if (game == 3) finish(taps >= reflexGoal());
      else if (game == 7) finish(rhythmHits >= 4);
      else finish(game == 5 && drawingReady && drawWin());
    }
  }

  public JSONObject json() {
    return json(true, true);
  }

  public JSONObject networkJson() {
    return json("LOBBY".equals(screen), game != 5 || drawingReady);
  }

  public JSONObject networkJsonFor(String recipient) {
    JSONObject j = networkJson();
    try {
      j.remove("lastVariant");
      j.remove("variantDecks");
      int secretViewer = indexOf(recipient);
      j.put("hiddenProbe", privateValues(hiddenProbe, secretViewer, 0));
      j.put("hiddenKind", privateValues(hiddenKind, secretViewer, -1));
      j.put("hiddenStep", privateValues(hiddenStep, secretViewer, 0));
      j.put("hiddenSeed", privateValues(hiddenSeed, secretViewer, 0));
      j.put("hiddenWonMask", privateValues(hiddenWonMask, secretViewer, 0));
      j.put("hiddenMistakes", privateValues(hiddenMistakes, secretViewer, 0));
      j.put("hiddenSolvedTurn", privateValues(hiddenSolvedTurn, secretViewer, -1));
      if ("JOIN_VOTE".equals(screen)) j.put("joinVotes", masked(joinVotes));
      boolean actor = current() != null && current().name.equals(recipient);
      int guesser = players.isEmpty() ? -1 : (active + 1) % players.size();
      boolean drawGuesser = game == 5 && drawingReady && guesser >= 0
          && players.get(guesser).name.equals(recipient);
      if (!"RESULT".equals(screen)) {
        j.put("loserCup", -1);
        j.put("target", -1);
        if ("VOTE".equals(screen)) j.put("votes", masked(votes));
        if ("PREDICT".equals(screen) || "GAME".equals(screen))
          j.put("predictions", masked(predictions));
        if ("CREW".equals(screen) || ("GAME".equals(screen) && (game == 0 || game == 2))) {
          if (game == 0 || game == 2) j.put("publicCrewLead", crewLead());
          j.put("crewChoices", masked(crewChoices));
        }
        if (game == 5 && drawingReady) j.put("drawGuesses", masked(drawGuesses));
        if (juryPhase) j.put("juryVotes", masked(juryVotes));
        if (!actor && game == 8) j.put("bluffTruth", -1);
        if ("RULE_VOTE".equals(screen)) j.put("reportVotes", masked(reportVotes));
        if (("GAME".equals(screen) || "CREW".equals(screen))
            && (game == 0 || game == 2 || game == 5)) {
          String[] fr = new String[4], en = new String[4];
          if (game == 0) {
            j.put("screenPromptFr", QUIZ_FR[variant][0]);
            j.put("screenPromptEn", QUIZ_EN[variant][0]);
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = QUIZ_FR[variant][option + 1];
              en[i] = QUIZ_EN[variant][option + 1];
            }
          } else if (game == 2) {
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = TUNES[(variant + option) % TUNES.length][0];
              en[i] = TUNES[(variant + option) % TUNES.length][1];
            }
          } else {
            if (actor && !drawingReady) {
              j.put("screenPromptFr", DRAW[variant][0]);
              j.put("screenPromptEn", DRAW[variant][1]);
            }
            for (int i = 0; i < 4; i++) {
              int option = (i - target + 4) % 4;
              fr[i] = DRAW[(variant + option) % DRAW.length][0];
              en[i] = DRAW[(variant + option) % DRAW.length][1];
            }
          }
          j.put("screenChoicesFr", new JSONArray(fr));
          j.put("screenChoicesEn", new JSONArray(en));
          if (game == 0 || game == 5) j.put("variant", -1);
        }
        if (("GAME".equals(screen) || "TRANSITION".equals(screen)
            || "HANDOFF".equals(screen) || "BET".equals(screen)
            || "PREDICT".equals(screen) || "CREW".equals(screen)) && !actor && game != 9) {
          if (!drawGuesser && game != 2) {
            j.put("variant", -1);
          }
          if (game == 6) j.put("sequence", new JSONArray());
        }
        if (game == 5 && !actor && !drawingReady) j.put("variant", -1);
      }
    } catch (Exception ignored) { }
    return j;
  }

  private static JSONArray masked(int[] values) {
    JSONArray result = new JSONArray();
    for (int value : values) result.put(value == -1 ? -1 : 2);
    return result;
  }

  private static JSONArray privateValues(int[] values, int recipient, int hidden) {
    JSONArray result = new JSONArray();
    for (int i = 0; i < values.length; i++) result.put(i == recipient ? values[i] : hidden);
    return result;
  }

  private JSONObject json(boolean includePhotos, boolean includeStrokes) {
    JSONObject j = new JSONObject();
    try {
      j.put("revision", revision);
      j.put("sentAt", clock.getAsLong());
      j.put("screen", screen);
      j.put("active", active);
      j.put("turn", turn);
      j.put("game", game);
      j.put("variant", variant);
      j.put("progress", progress);
      j.put("taps", taps);
      j.put("selected", selected);
      j.put("target", target);
      j.put("rhythmHits", rhythmHits);
      j.put("wager", wager);
      j.put("bombNext", bombNext);
      j.put("bombVisitedMask", bombVisitedMask);
      j.put("bombAwaitingPass", bombAwaitingPass);
      j.put("bombCutAttempted", bombCutAttempted);
      j.put("bombCutWire", bombCutWire);
      j.put("roundPoints", roundPoints);
      j.put("roundSips", roundSips);
      j.put("chosenCup", chosenCup);
      j.put("reflexSeed", reflexSeed);
      j.put("revealUntil", revealUntil);
      j.put("juryPhase", juryPhase);
      j.put("bluffTruth", bluffTruth);
      j.put("juryVotes", new JSONArray(juryVotes));
      j.put("predictions", new JSONArray(predictions));
      j.put("crewChoices", new JSONArray(crewChoices));
      j.put("trustedFriend", trustedFriend);
      j.put("crewPoints", new JSONArray(crewPoints));
      j.put("drawGuesses", new JSONArray(drawGuesses));
      j.put("betPlaced", betPlaced);
      j.put("deadline", deadline);
      j.put("audienceClosingAt", audienceClosingAt);
      j.put("started", started);
      j.put("lastWon", lastWon);
      j.put("roundPassed", roundPassed);
      j.put("drawingReady", drawingReady);
      j.put("note", note);
      j.put("targetX", targetX);
      j.put("targetY", targetY);
      j.put("loserCup", loserCup);
      j.put("bombRoute", new JSONArray(bombRoute));
      j.put("mode", mode);
      j.put("passiveStreak", passiveStreak);
      j.put("offers", new JSONArray(offers));
      j.put("votes", new JSONArray(votes));
      j.put("ruleOwner", ruleOwner);
      j.put("ruleId", ruleId);
      j.put("voteWinner", voteWinner);
      j.put("freePick", freePick);
      j.put("bonusId", bonusId);
      j.put("secretMask", secretMask);
      j.put("secretId", secretId);
      j.put("hiddenProbe", new JSONArray(hiddenProbe));
      j.put("hiddenKind", new JSONArray(hiddenKind));
      j.put("hiddenStep", new JSONArray(hiddenStep));
      j.put("hiddenSeed", new JSONArray(hiddenSeed));
      j.put("hiddenWonMask", new JSONArray(hiddenWonMask));
      j.put("hiddenMistakes", new JSONArray(hiddenMistakes));
      j.put("hiddenSolvedTurn", new JSONArray(hiddenSolvedTurn));
      j.put("hiddenLastOwner", hiddenLastOwner);
      j.put("hiddenLastKind", hiddenLastKind);
      j.put("hiddenLastTurn", hiddenLastTurn);
      j.put("queuedRuleOwner", queuedRuleOwner);
      j.put("queuedSecretId", queuedSecretId);
      j.put("queuedRuleOffers", new JSONArray(queuedRuleOffers));
      j.put("ruleOffers", new JSONArray(ruleOffers));
      j.put("reportTarget", reportTarget);
      j.put("reportBy", reportBy);
      j.put("reportReturn", reportReturn);
      j.put("reportVotes", new JSONArray(reportVotes));
      j.put("reportDeadline", reportDeadline);
      j.put("rulePenaltyApplied", rulePenaltyApplied);
      j.put("pendingJoinName", pendingJoinName);
      j.put("pendingJoinLanguage", pendingJoinLanguage);
      j.put("lastJoinName", lastJoinName);
      j.put("lastJoinAccepted", lastJoinAccepted);
      j.put("lastJoinSips", lastJoinSips);
      j.put("joinVotes", new JSONArray(joinVotes));
      j.put("joinVoteDeadline", joinVoteDeadline);
      j.put("joinDecisionUntil", joinDecisionUntil);
      JSONArray ps = new JSONArray();
      for (Player p : players) ps.put(p.json(includePhotos));
      j.put("players", ps);
      JSONArray d = new JSONArray();
      for (int x : deck) d.put(x);
      j.put("deck", d);
      j.put("lastVariant", new JSONArray(lastVariant));
      JSONArray packs = new JSONArray();
      for (int i = 0; i < TYPES.length; i++)
        packs.put(new JSONArray(i < variantDecks.size() ? variantDecks.get(i) : new ArrayList<>()));
      j.put("variantDecks", packs);
      JSONArray s = new JSONArray();
      for (int x : sequence) s.put(x);
      j.put("sequence", s);
      JSONArray lines = new JSONArray();
      for (float[] v : strokes) {
        if (!includeStrokes) break;
        JSONArray a = new JSONArray();
        for (float f : v) a.put(f);
        lines.put(a);
      }
      j.put("strokes", lines);
    } catch (Exception ignored) {
    }
    return j;
  }

  public void restore(JSONObject j) {
    java.util.HashMap<String, String> knownPhotos = new java.util.HashMap<>();
    for (Player player : players) knownPhotos.put(player.name, player.photo);
    players.clear();
    JSONArray ps = j.optJSONArray("players");
    if (ps != null)
      for (int i = 0; i < ps.length(); i++) {
        JSONObject row = ps.optJSONObject(i);
        if (row == null) continue;
        Player player = Player.from(row);
        if (!row.has("photo")) player.photo = knownPhotos.getOrDefault(player.name, "");
        players.add(player);
      }
    revision = j.optLong("revision", revision);
    screen = j.optString("screen", "HOME");
    active = j.optInt("active");
    turn = j.optInt("turn");
    game = j.optInt("game");
    variant = j.optInt("variant");
    progress = j.optInt("progress");
    taps = j.optInt("taps");
    selected = j.optInt("selected", -1);
    target = j.optInt("target");
    rhythmHits = j.optInt("rhythmHits");
    wager = j.optInt("wager", 1);
    bombNext = j.optInt("bombNext", active);
    bombVisitedMask = j.optInt("bombVisitedMask");
    bombAwaitingPass = j.optBoolean("bombAwaitingPass");
    bombCutAttempted = j.optBoolean("bombCutAttempted");
    bombCutWire = j.optInt("bombCutWire", -1);
    roundPoints = j.optInt("roundPoints");
    roundSips = j.optInt("roundSips");
    chosenCup = j.optInt("chosenCup", -1);
    reflexSeed = j.optInt("reflexSeed");
    revealUntil = j.optLong("revealUntil");
    juryPhase = j.optBoolean("juryPhase");
    bluffTruth = j.optInt("bluffTruth", -1);
    juryVotes = readInts(j.optJSONArray("juryVotes"));
    predictions = readInts(j.optJSONArray("predictions"));
    crewChoices = readInts(j.optJSONArray("crewChoices"));
    trustedFriend = j.optInt("trustedFriend", -1);
    publicCrewLead = j.optInt("publicCrewLead", -2);
    crewPoints = readInts(j.optJSONArray("crewPoints"));
    drawGuesses = readInts(j.optJSONArray("drawGuesses"));
    betPlaced = j.optBoolean("betPlaced");
    deadline = j.optLong("deadline");
    audienceClosingAt = j.optLong("audienceClosingAt");
    started = j.optLong("started");
    lastWon = j.optBoolean("lastWon");
    roundPassed = j.optBoolean("roundPassed");
    drawingReady = j.optBoolean("drawingReady");
    note = j.optString("note");
    targetX = (float) j.optDouble("targetX", 200);
    targetY = (float) j.optDouble("targetY", 390);
    loserCup = j.optInt("loserCup");
    bombRoute = readInts(j.optJSONArray("bombRoute"));
    mode = j.optString("mode", "VOTE");
    passiveStreak = Math.max(0, j.optInt("passiveStreak"));
    offers = readInts(j.optJSONArray("offers"));
    votes = readInts(j.optJSONArray("votes"));
    ruleOwner = j.optString("ruleOwner", "");
    ruleId = j.optInt("ruleId", -1);
    voteWinner = j.optInt("voteWinner", -1);
    freePick = j.optInt("freePick", 0);
    bonusId = j.optInt("bonusId", 0);
    secretMask = j.optInt("secretMask");
    secretId = j.optInt("secretId", -1);
    hiddenProbe = readInts(j.optJSONArray("hiddenProbe"));
    hiddenKind = readInts(j.optJSONArray("hiddenKind"));
    hiddenStep = readInts(j.optJSONArray("hiddenStep"));
    hiddenSeed = readInts(j.optJSONArray("hiddenSeed"));
    hiddenWonMask = readInts(j.optJSONArray("hiddenWonMask"));
    hiddenMistakes = readInts(j.optJSONArray("hiddenMistakes"));
    hiddenSolvedTurn = readInts(j.optJSONArray("hiddenSolvedTurn"));
    hiddenLastOwner = j.optString("hiddenLastOwner", "");
    hiddenLastKind = j.optInt("hiddenLastKind", -1);
    hiddenLastTurn = j.optInt("hiddenLastTurn", -1);
    queuedRuleOwner = j.optString("queuedRuleOwner", "");
    queuedSecretId = j.optInt("queuedSecretId", -1);
    queuedRuleOffers = readInts(j.optJSONArray("queuedRuleOffers"));
    if (hiddenKind.length != players.size() || hiddenProbe.length != players.size()
        || hiddenStep.length != players.size() || hiddenSeed.length != players.size()
        || hiddenWonMask.length != players.size() || hiddenMistakes.length != players.size()
        || hiddenSolvedTurn.length != players.size())
      initHidden();
    ruleOffers = readInts(j.optJSONArray("ruleOffers"));
    if (!ruleOwner.isEmpty() && ruleId < 0 && ruleOffers.length != 3)
      ruleOffers = new int[] {0, 1, 2};
    reportTarget = j.optString("reportTarget", "");
    reportBy = j.optString("reportBy", "");
    reportReturn = j.optString("reportReturn", "VOTE");
    reportVotes = readInts(j.optJSONArray("reportVotes"));
    reportDeadline = j.optLong("reportDeadline");
    rulePenaltyApplied = j.optBoolean("rulePenaltyApplied");
    pendingJoinName = j.optString("pendingJoinName", "");
    pendingJoinLanguage = j.optString("pendingJoinLanguage", "FR");
    lastJoinName = j.optString("lastJoinName", "");
    lastJoinAccepted = j.optBoolean("lastJoinAccepted");
    lastJoinSips = j.optInt("lastJoinSips");
    joinVotes = readInts(j.optJSONArray("joinVotes"));
    joinVoteDeadline = j.optLong("joinVoteDeadline");
    joinDecisionUntil = j.optLong("joinDecisionUntil");
    deck.clear();
    JSONArray d = j.optJSONArray("deck");
    if (d != null) for (int i = 0; i < d.length(); i++) deck.add(d.optInt(i));
    java.util.Arrays.fill(lastVariant, -1);
    JSONArray previous = j.optJSONArray("lastVariant");
    if (previous != null)
      for (int i = 0; i < Math.min(lastVariant.length, previous.length()); i++)
        lastVariant[i] = previous.optInt(i, -1);
    variantDecks.clear();
    JSONArray packs = j.optJSONArray("variantDecks");
    for (int i = 0; i < TYPES.length; i++) {
      ArrayList<Integer> pack = new ArrayList<>();
      JSONArray savedPack = packs == null ? null : packs.optJSONArray(i);
      int limit = variantCount(i);
      if (savedPack != null && savedPack.length() <= limit) {
        for (int k = 0; k < savedPack.length(); k++) {
          int value = savedPack.optInt(k, -1);
          if (value < 0 || value >= limit || pack.contains(value)) { pack.clear(); break; }
          pack.add(value);
        }
      }
      variantDecks.add(pack);
    }
    JSONArray s = j.optJSONArray("sequence");
    sequence = new int[s == null ? 0 : s.length()];
    for (int i = 0; i < sequence.length; i++) sequence[i] = s.optInt(i);
    screenPromptFr = j.optString("screenPromptFr", "");
    screenPromptEn = j.optString("screenPromptEn", "");
    screenChoicesFr = readStrings(j.optJSONArray("screenChoicesFr"));
    screenChoicesEn = readStrings(j.optJSONArray("screenChoicesEn"));
    strokes.clear();
    JSONArray ls = j.optJSONArray("strokes");
    if (ls != null)
      for (int i = 0; i < ls.length(); i++) {
        JSONArray a = ls.optJSONArray(i);
        if (a != null) {
          float[] f = new float[a.length()];
          for (int k = 0; k < f.length; k++) f[k] = (float) a.optDouble(k);
          strokes.add(f);
        }
      }
    if (active >= players.size()) active = 0;
  }

  private static int[] readInts(JSONArray a) {
    if (a == null) return new int[0];
    int[] result = new int[a.length()];
    for (int i = 0; i < result.length; i++) result[i] = a.optInt(i);
    return result;
  }

  private static String[] readStrings(JSONArray a) {
    if (a == null) return new String[0];
    String[] result = new String[a.length()];
    for (int i = 0; i < result.length; i++) result[i] = a.optString(i, "");
    return result;
  }

  public static final String[][] QUIZ_FR = {
    {"Quel animal a trois cœurs ?", "Pieuvre", "Panda", "Canard", "Moustique"},
    {"Combien de côtés a un dé classique ?", "6", "8", "12", "4"},
    {"Quel fruit est une baie botanique ?", "Banane", "Fraise", "Framboise", "Cerise"},
    {
      "Quelle planète pleut des diamants selon les modèles ?", "Neptune", "Mars", "Mercure", "Vénus"
    },
    {"Quel animal dort debout ?", "Cheval", "Poulpe", "Pingouin", "Taupe"},
    {"Combien de bras a une pieuvre ?", "8", "6", "10", "12"},
    {"Quelle est la couleur de la peau de l'ours polaire ?", "Noire", "Blanche", "Rose", "Bleue"},
    {"Quel oiseau sait voler en marche arrière ?", "Colibri", "Corbeau", "Flamant", "Manchot"},
    {"Combien d'os possède généralement un adulte ?", "206", "106", "306", "406"},
    {"Quelle planète est la plus chaude ?", "Vénus", "Mercure", "Mars", "Jupiter"},
    {"Quel est le plus grand désert du monde ?", "Antarctique", "Sahara", "Gobi", "Atacama"},
    {"Quel animal a des empreintes proches des nôtres ?", "Koala", "Pingouin", "Dauphin", "Lama"},
    {"Dans quel pays le papier a-t-il été inventé ?", "Chine", "Italie", "Pérou", "Égypte"},
    {"Combien de faces a un dé de jeu de rôle D20 ?", "20", "12", "16", "24"},
    {"Quel organe humain est le plus grand ?", "Peau", "Foie", "Cœur", "Poumon"},
    {"Quel métal est liquide à température ambiante ?", "Mercure", "Cuivre", "Fer", "Or"},
    {"Quelle planète a les anneaux les plus visibles ?", "Saturne", "Mars", "Terre", "Vénus"}
  };
  public static final String[][] QUIZ_EN = {
    {"Which animal has three hearts?", "Octopus", "Panda", "Duck", "Mosquito"},
    {"How many sides on a standard die?", "6", "8", "12", "4"},
    {"Which fruit is botanically a berry?", "Banana", "Strawberry", "Raspberry", "Cherry"},
    {"Which planet may rain diamonds?", "Neptune", "Mars", "Mercury", "Venus"},
    {"Which animal can sleep standing?", "Horse", "Octopus", "Penguin", "Mole"},
    {"How many arms does an octopus have?", "8", "6", "10", "12"},
    {"What color is a polar bear's skin?", "Black", "White", "Pink", "Blue"},
    {"Which bird can fly backward?", "Hummingbird", "Crow", "Flamingo", "Penguin"},
    {"How many bones does an adult usually have?", "206", "106", "306", "406"},
    {"Which planet is the hottest?", "Venus", "Mercury", "Mars", "Jupiter"},
    {"What is the world's largest desert?", "Antarctica", "Sahara", "Gobi", "Atacama"},
    {"Which animal has fingerprints like ours?", "Koala", "Penguin", "Dolphin", "Llama"},
    {"Where was paper invented?", "China", "Italy", "Peru", "Egypt"},
    {"How many faces does a D20 role-playing die have?", "20", "12", "16", "24"},
    {"What is the human body's largest organ?", "Skin", "Liver", "Heart", "Lung"},
    {"Which metal is liquid at room temperature?", "Mercury", "Copper", "Iron", "Gold"},
    {"Which planet has the most visible rings?", "Saturn", "Mars", "Earth", "Venus"}
  };
  public static final String[][] POSES = {
    {"Entre comme si tu venais d'acheter le bistrot. La table t'invente un titre.", "Enter as if you just bought the bar. Let the table invent your title."},
    {"Pose de pochette d'album après une rupture spectaculaire. Un complice peut jouer l'ex.", "Strike an album-cover pose after a dramatic breakup. A friend may play the ex."},
    {"Tu viens de voir l'addition. Fige la réaction la plus digne possible.", "You have just seen the bill. Freeze your most dignified reaction."},
    {"Un paparazzi invisible arrive. Offre-lui ta pire pose de célébrité.", "An invisible paparazzo arrives. Give them your worst celebrity pose."},
    {"Joue un serveur qui sait un secret et refuse de le dire.", "Play a waiter who knows a secret and refuses to tell."},
    {"Présente un sous-verre comme un bijou hors de prix.", "Present a coaster as if it were priceless jewelry."},
    {"Tu croises ton ancien patron au karaoké. Tout est dans le regard.", "You meet your old boss at karaoke. Let your face do the talking."},
    {"Avec un volontaire, posez pour la photo officielle d'un duo improbable.", "With a willing friend, pose for the official photo of an unlikely duo."},
    {"Mime une entrée de star dans une salle qui n'a rien demandé.", "Make a star entrance into a room that asked for none of it."},
    {"Deviens une statue de musée. La table lui donne un titre scandaleux mais tendre.", "Become a museum statue. The room gives it a scandalous but kind title."},
    {"Joue le DJ dont le vinyle vient de s'arrêter au pire moment.", "Play a DJ whose record stops at the worst possible moment."},
    {"Pose d'agent secret qui a oublié son mot de passe.", "Pose as a secret agent who forgot the password."},
    {"Tu viens de réussir un coup de bluff. Cache ton sourire victorieux.", "You just pulled off a bluff. Hide your victorious grin."},
    {"Accepte un prix imaginaire pour une compétence totalement inutile.", "Accept an imaginary award for a completely useless skill."},
    {"La table te surprend en train de répéter un discours au miroir.", "The room catches you rehearsing a speech in the mirror."},
    {"Prends la pose d'un détective qui a enfin compris… presque.", "Strike the pose of a detective who almost has it figured out."},
    {"Fais une révérence à la personne qui t'a sauvé la soirée.", "Bow to the person who saved your evening."},
    {"Transforme ta chaise en trône de fin de nuit, sans la déplacer.", "Make your chair a late-night throne without moving it."}
  };
  public static final String[][] DRAW = {
    {"Le dernier métro en limousine", "The last train as a limousine"},
    {"Une carte bancaire qui fuit l'addition", "A credit card fleeing the bill"},
    {"Un DJ devant un vinyle cassé", "A DJ with a broken record"},
    {"Un sous-verre avec une couronne", "A coaster wearing a crown"},
    {"Un bouquet qui cache un micro", "A bouquet hiding a microphone"},
    {"Un taxi refusant une célébrité", "A taxi rejecting a celebrity"},
    {"Un miroir donnant des conseils", "A mirror giving advice"},
    {"Des lunettes de soleil à minuit", "Sunglasses at midnight"},
    {"Une clé oubliée sur le comptoir", "A forgotten key on the bar"},
    {"Un photomaton en panne de pose", "A photo booth with no poses left"},
    {"Une boule disco en réunion", "A disco ball in a meeting"},
    {"Un téléphone jaloux d'un vinyle", "A phone jealous of a record"},
    {"Une invitation au mauvais nom", "An invitation with the wrong name"},
    {"Un détective dans une cabine DJ", "A detective in a DJ booth"},
    {"Un chapeau sur un verre vide", "A hat on an empty glass"},
    {"Un billet de concert trempé", "A soaked concert ticket"},
    {"Une chaise réservée à personne", "A chair reserved for nobody"}
  };
  public static final String[][] BLUFF = {
    {"Raconte comment tu as salué quelqu'un dont tu avais oublié le prénom.", "Tell how you greeted someone whose name you had forgotten."},
    {"Décris le plus étrange objet retrouvé au fond d'une poche après une soirée.", "Describe the strangest thing found in a pocket after a night out."},
    {"Raconte une addition que personne ne voulait comprendre.", "Tell the story of a bill nobody wanted to understand."},
    {"Décris un compliment qui a pris une tournure imprévue.", "Describe a compliment that took an unexpected turn."},
    {"Raconte le texto envoyé à la mauvaise personne.", "Tell us about a text sent to the wrong person."},
    {"Décris une excuse trop élégante pour quitter une soirée.", "Describe an overly elegant excuse to leave a party."},
    {"Raconte un rendez-vous arrivé dans le mauvais lieu.", "Tell us about a date that reached the wrong place."},
    {"Décris un débat absurde qui a duré beaucoup trop longtemps.", "Describe an absurd debate that lasted far too long."},
    {"Raconte une rencontre improbable au comptoir.", "Tell us about an unlikely encounter at the bar."},
    {"Décris une petite victoire que tu as célébrée comme un trophée.", "Describe a tiny win you celebrated like a trophy."},
    {"Raconte quand tu as fait semblant de connaître la chanson.", "Tell us when you pretended to know the song."},
    {"Décris une photo de groupe devenue une enquête.", "Describe a group photo that turned into an investigation."},
    {"Raconte une règle de maison annoncée très sérieusement.", "Tell us about a house rule announced with a straight face."},
    {"Décris un objet emprunté qui a vécu sa propre aventure.", "Describe a borrowed object that had its own adventure."},
    {"Raconte un discours improvisé dont tu as regretté le début.", "Tell us about an improvised speech whose opening you regretted."},
    {"Décris une arrivée tardive que tout le monde a remarquée.", "Describe a late entrance nobody could ignore."},
    {"Raconte un moment où la table entière a gardé le même secret.", "Tell us about a moment when the whole table kept the same secret."}
  };
  public static final String[][] TUNES = {
    {"Générique qui grimpe", "Rising opening theme"},
    {"Descente d'escalier", "Staircase descent"},
    {"Ascenseur fou", "Wild elevator leaps"},
    {"Appel à l'écho", "Distant echo call"},
    {"Duo de cloches", "Twin bells"},
    {"Vinyle rayé", "Skipping record"}
  };
}

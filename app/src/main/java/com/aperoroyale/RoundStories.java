package com.aperoroyale;

import java.util.ArrayList;

/** Facts from the host's resolved round, written as a short story for the table. */
public final class RoundStories {
  private RoundStories() { }

  public static String opening(int game, boolean en) {
    String[][] cues = {
        {"Une réponse, plusieurs réputations.", "One answer, several reputations."},
        {"La table devient votre scène.", "The table becomes your stage."},
        {"Un jingle, des certitudes douteuses.", "One jingle, questionable confidence."},
        {"Les amis dessinent le danger.", "Your friends design the danger."},
        {"Sous chaque verre, une alliance.", "Under every cup, an alliance."},
        {"Un dessin peut devenir une légende.", "A drawing can become a legend."},
        {"Souviens-toi de ce qu'ils ont laissé.", "Remember what they left behind."},
        {"Une mesure inventée ensemble.", "A rhythm made together."},
        {"Une histoire, plusieurs versions.", "One story, several versions."},
        {"Choisis la prochaine main.", "Choose the next hand."}
    };
    return cues[Math.floorMod(game, cues.length)][en ? 1 : 0];
  }

  /** Optional conversation fuel. It never blocks the next round or affects scoring. */
  public static boolean hasTableSpark(GameEngine g) {
    int interval = g.players.size() <= 2 || "TURBO".equals(g.mode) ? 3 : 4;
    return (g.turn + 1) % interval == 0;
  }

  public static String tableSpark(GameEngine g, boolean en) {
    String[][] sparks = {
        {"Qui défendrait sa mauvaise réponse avec le plus d'aplomb ?", "Who would defend a wrong answer with the most confidence?"},
        {"Quel titre de tabloïd donnerais-tu à cette performance ?", "What tabloid headline would you give that performance?"},
        {"Quel tube devrait être banni du bar après minuit ?", "Which song should the bar ban after midnight?"},
        {"Qui a le plus souri en préparant ce piège ?", "Who smiled the most while setting that trap?"},
        {"À qui confierais-tu ton dernier sous-verre ?", "Who would you trust with your last coaster?"},
        {"Quel titre absurde mérite ce dessin ?", "What ridiculous title does that drawing deserve?"},
        {"Quel détail de cette soirée personne ne retiendra demain ?", "What detail of tonight will nobody remember tomorrow?"},
        {"Qui mérite le droit de choisir le prochain disque ?", "Who deserves to choose the next record?"},
        {"Quelle excuse serait trop belle pour être vraie ?", "Which excuse would be too good to be true?"},
        {"À qui léguerais-tu l'objet maudit ?", "Who should inherit the cursed object?"}
    };
    return sparks[Math.floorMod(g.game, sparks.length)][en ? 1 : 0];
  }

  public static String handoffSpark(GameEngine g, boolean en) {
    String[][] cues = {
        {"Pendant le passage : chacun parie sur la réponse.", "While passing it: everyone predicts the answer."},
        {"Pendant le passage : inventez le nom de cette pose.", "While passing it: give the pose a name."},
        {"Pendant le passage : qui reconnaîtra le jingle en premier ?", "While passing it: who will name the jingle first?"},
        {"Pendant le passage : annoncez le saboteur de la salle.", "While passing it: name the room's saboteur."},
        {"Pendant le passage : défendez votre verre préféré.", "While passing it: defend your favorite cup."},
        {"Pendant le passage : trouvez un titre à la galerie.", "While passing it: name the gallery."},
        {"Pendant le passage : inventez un aide-mémoire douteux.", "While passing it: invent a dubious memory trick."},
        {"Pendant le passage : battez la mesure sur la table.", "While passing it: tap a beat on the table."},
        {"Pendant le passage : préparez votre meilleur poker face.", "While passing it: prepare your best poker face."},
        {"Pendant le passage : à qui passeriez-vous le dernier fil ?", "While passing it: who gets the last wire?"}
    };
    return cues[Math.floorMod(g.game, cues.length)][en ? 1 : 0];
  }

  public static String[] forRound(GameEngine g, boolean en) {
    String actor = g.current() == null ? (en ? "Someone" : "Quelqu'un") : g.current().name;
    if (g.roundPassed) return new String[] {
        en ? "A graceful exit" : "La porte de sortie",
        en ? actor + " chose to pass." : actor + " a choisi de passer.",
        en ? "No points, no sip. The night goes on." : "Ni point, ni gorgée. La soirée continue."
    };
    return switch (g.game) {
      case 0 -> new String[] {
          en ? "The room had its theories" : "La salle avait ses théories",
          (en ? "Answer: " : "Réponse : ") + quizAnswer(g, en),
          rightNames(g, g.crewChoices, g.target, en)
      };
      case 1 -> new String[] {
          en ? "The jury has spoken" : "Le jury a parlé",
          voteSplit(g, en),
          (en ? "Applause from: " : "Applaudi par : ")
              + names(g, g.juryVotes, 1, en)
      };
      case 2 -> new String[] {
          en ? "The tune unmasked" : "Le jingle démasqué",
          (en ? "It was: " : "C'était : ") + tuneAnswer(g, en),
          rightNames(g, g.crewChoices, g.target, en)
      };
      case 3 -> new String[] {
          en ? "A course built by friends" : "Un parcours signé par les potes",
          (en ? "Targets hit: " : "Cibles touchées : ") + g.taps + " / " + g.reflexGoal(),
          (en ? "Set by: " : "Posées par : ") + contributors(g, en)
      };
      case 4 -> new String[] {
          en ? "The coasters are turned" : "Les sous-verres se retournent",
          g.chosenCup < 0 ? (en ? "No cup was picked." : "Aucun gobelet choisi.")
              : (en ? "Cup " : "Gobelet ") + (g.chosenCup + 1) + " · "
                  + (g.cupIsSafe() ? (en ? "safe" : "sauf") : (en ? "cursed" : "piégé")),
          (en ? "Protected by: " : "Protégé par : ")
              + names(g, g.crewChoices, g.chosenCup, en)
      };
      case 5 -> new String[] {
          en ? "The improvised gallery" : "La galerie improvisée",
          (en ? "Subject: " : "Sujet : ") + drawAnswer(g, en),
          (en ? "Guessed it: " : "Ont trouvé : ")
              + names(g, g.drawGuesses, g.target, en)
              + "  ·  " + g.drawCorrectCount() + "/" + g.drawValidGuessCount()
      };
      case 6 -> new String[] {
          en ? "The friends' chain" : "La chaîne des amis",
          (en ? "Remembered: " : "Retenus : ") + g.progress + " / " + g.sequence.length,
          (en ? "Opened by: " : "Ouverte par : ") + firstContributions(g, en)
      };
      case 7 -> new String[] {
          en ? "The room's rhythm" : "Le rythme de la table",
          (en ? "Beats: " : "Temps : ") + beats(g),
          (en ? "Composed by: " : "Composé par : ") + contributors(g, en)
      };
      case 8 -> new String[] {
          en ? "Who believed the story?" : "Qui a cru l'histoire ?",
          g.bluffTruth < 0 ? (en ? "The story stayed untold." : "L'histoire n'a pas été racontée.")
              : g.bluffTruth == 1 ? (en ? "It was true." : "C'était vrai.")
                  : (en ? "It was made up." : "C'était inventé."),
          (en ? "Believers: " : "Convaincus : ") + names(g, g.juryVotes, 1, en)
      };
      case 9 -> new String[] {
          en ? "The bomb toured the table" : "La bombe a fait le tour",
          route(g, en),
          g.bombCutAttempted
              ? (en ? "Final wire: " : "Dernier fil : ")
                  + (g.bombCutWire == 0 ? (en ? "red" : "rouge") : (en ? "blue" : "bleu"))
              : (en ? "Every hand played its part." : "Chaque main a compté.")
      };
      default -> new String[] {en ? "The round is over" : "Fin de manche", actor, ""};
    };
  }

  private static String quizAnswer(GameEngine g, boolean en) {
    if (g.variant < 0 || g.variant >= GameEngine.QUIZ_FR.length) return "?";
    return (en ? GameEngine.QUIZ_EN : GameEngine.QUIZ_FR)[g.variant][1];
  }

  private static String tuneAnswer(GameEngine g, boolean en) {
    if (g.variant < 0) return "?";
    return GameEngine.TUNES[Math.floorMod(g.variant, GameEngine.TUNES.length)][en ? 1 : 0];
  }

  private static String drawAnswer(GameEngine g, boolean en) {
    if (g.variant < 0 || g.variant >= GameEngine.DRAW.length) return "?";
    return GameEngine.DRAW[g.variant][en ? 1 : 0];
  }

  private static String rightNames(GameEngine g, int[] choices, int target, boolean en) {
    int count = 0;
    for (int i = 0; i < choices.length && i < g.players.size(); i++)
      if (i != g.active && choices[i] == target) count++;
    return (en ? "Got it: " : "Bien vu : ") + names(g, choices, target, en)
        + "  ·  " + count + "/" + g.crewCount();
  }

  private static String voteSplit(GameEngine g, boolean en) {
    int yes = 0, no = 0;
    for (int i = 0; i < g.juryVotes.length && i < g.players.size(); i++) {
      if (i == g.active) continue;
      if (g.juryVotes[i] == 1) yes++;
      if (g.juryVotes[i] == 0) no++;
    }
    return en ? "Accepted " + yes + " · challenged " + no
        : "Validé " + yes + " · contesté " + no;
  }

  private static String names(GameEngine g, int[] choices, int value, boolean en) {
    ArrayList<String> found = new ArrayList<>();
    for (int i = 0; i < choices.length && i < g.players.size(); i++)
      if (i != g.active && choices[i] == value) found.add(g.players.get(i).name);
    return compact(found, en);
  }

  private static String contributors(GameEngine g, boolean en) {
    ArrayList<String> found = new ArrayList<>();
    for (int i = 0; i < g.crewChoices.length && i < g.players.size(); i++)
      if (i != g.active && g.crewChoices[i] >= 0) found.add(g.players.get(i).name);
    return compact(found, en);
  }

  private static String firstContributions(GameEngine g, boolean en) {
    ArrayList<String> found = new ArrayList<>();
    String[] symbols = {"◆", "●", "▲", "■"};
    for (int step = 1; step < g.players.size(); step++) {
      int i = (g.active + step) % g.players.size();
      if (i < g.crewChoices.length && g.crewChoices[i] >= 0)
        found.add(g.players.get(i).name + " " + symbols[Math.floorMod(g.crewChoices[i], 4)]);
    }
    return compact(found, en);
  }

  private static String beats(GameEngine g) {
    int[] pattern = g.rhythmPattern();
    return (pattern[0] + 1) + " · " + (pattern[1] + 1) + " · "
        + (pattern[2] + 1) + " · " + (pattern[3] + 1);
  }

  private static String route(GameEngine g, boolean en) {
    ArrayList<String> found = new ArrayList<>();
    for (int index : g.bombRoute)
      if (index >= 0 && index < g.players.size()) found.add(g.players.get(index).name);
    return compact(found, en).replace(" · ", " → ");
  }

  private static String compact(ArrayList<String> found, boolean en) {
    if (found.isEmpty()) return en ? "nobody" : "personne";
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < Math.min(3, found.size()); i++) {
      if (i > 0) s.append(" · ");
      s.append(found.get(i));
    }
    if (found.size() > 3) s.append(" +").append(found.size() - 3);
    return s.toString();
  }
}

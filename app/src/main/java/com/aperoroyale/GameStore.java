package com.aperoroyale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import org.json.JSONObject;

/** SQLite persistence for resumable sessions, lifetime scores and an auditable turn log. */
public final class GameStore extends SQLiteOpenHelper {
  public GameStore(Context context) {
    super(context, "apero_royale.db", null, 3);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE session(id INTEGER PRIMARY KEY, data TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE stats(name TEXT PRIMARY KEY COLLATE NOCASE, wins INTEGER NOT NULL DEFAULT 0,"
            + " games INTEGER NOT NULL DEFAULT 0, drinks INTEGER NOT NULL DEFAULT 0, sips INTEGER NOT NULL DEFAULT 0,"
            + " predictions INTEGER NOT NULL DEFAULT 0, correct_predictions INTEGER NOT NULL DEFAULT 0, points INTEGER"
            + " NOT NULL DEFAULT 0)");
    db.execSQL(
        "CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, player"
            + " TEXT NOT NULL, game TEXT NOT NULL, won INTEGER NOT NULL, sips INTEGER NOT NULL DEFAULT 0,"
            + " points INTEGER NOT NULL DEFAULT 0, role TEXT NOT NULL DEFAULT 'ACTOR', duration_ms INTEGER NOT NULL DEFAULT 0)");
    db.execSQL("CREATE TABLE game_stats(name TEXT NOT NULL COLLATE NOCASE, game TEXT NOT NULL,"
        + " games INTEGER NOT NULL DEFAULT 0, wins INTEGER NOT NULL DEFAULT 0, sips INTEGER NOT NULL DEFAULT 0,"
        + " points INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(name,game))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE stats ADD COLUMN sips INTEGER NOT NULL DEFAULT 0");
      db.execSQL("UPDATE stats SET sips=drinks");
      db.execSQL("ALTER TABLE history ADD COLUMN sips INTEGER NOT NULL DEFAULT 0");
      db.execSQL("UPDATE history SET sips=CASE WHEN won=0 THEN 1 ELSE 0 END");
    }
    if (oldVersion < 3) {
      db.execSQL("ALTER TABLE stats ADD COLUMN predictions INTEGER NOT NULL DEFAULT 0");
      db.execSQL("ALTER TABLE stats ADD COLUMN correct_predictions INTEGER NOT NULL DEFAULT 0");
      db.execSQL("ALTER TABLE history ADD COLUMN points INTEGER NOT NULL DEFAULT 0");
      db.execSQL("ALTER TABLE history ADD COLUMN role TEXT NOT NULL DEFAULT 'ACTOR'");
      db.execSQL("ALTER TABLE history ADD COLUMN duration_ms INTEGER NOT NULL DEFAULT 0");
      db.execSQL("CREATE TABLE game_stats(name TEXT NOT NULL COLLATE NOCASE, game TEXT NOT NULL,"
          + " games INTEGER NOT NULL DEFAULT 0, wins INTEGER NOT NULL DEFAULT 0, sips INTEGER NOT NULL DEFAULT 0,"
          + " points INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(name,game))");
    }
  }

  public void save(GameEngine engine) {
    ContentValues v = new ContentValues();
    v.put("id", 1);
    v.put("data", engine.json().toString());
    getWritableDatabase().insertWithOnConflict("session", null, v, SQLiteDatabase.CONFLICT_REPLACE);
  }

  public boolean load(GameEngine engine) {
    try (Cursor c = getReadableDatabase().rawQuery("SELECT data FROM session WHERE id=1", null)) {
      if (!c.moveToFirst()) return false;
      engine.restore(new JSONObject(c.getString(0)));
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void record(GameEngine.Player p, int game, boolean won, int points, int drinks, int sips,
      long durationMs) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("ts", System.currentTimeMillis());
      v.put("player", p.name);
      v.put("game", GameEngine.TYPES[game]);
      v.put("won", won ? 1 : 0);
      v.put("sips", sips);
      v.put("points", points);
      v.put("role", "ACTOR");
      v.put("duration_ms", durationMs);
      db.insert("history", null, v);
      db.execSQL("INSERT OR IGNORE INTO stats(name) VALUES(?)", new Object[] {p.name});
      db.execSQL(
          "UPDATE stats SET wins=wins+?, games=games+1, drinks=drinks+?, sips=sips+?, points=MAX(0,points+?)"
              + " WHERE name=?",
          new Object[] {won ? 1 : 0, drinks, sips, points, p.name});
      db.execSQL("INSERT OR IGNORE INTO game_stats(name,game) VALUES(?,?)",
          new Object[] {p.name, GameEngine.TYPES[game]});
      db.execSQL("UPDATE game_stats SET games=games+1,wins=wins+?,sips=sips+?,points=points+?"
          + " WHERE name=? AND game=?",
          new Object[] {won ? 1 : 0, sips, points, p.name, GameEngine.TYPES[game]});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void recordPrediction(GameEngine.Player p, int game, boolean correct, int points, int sips) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("ts", System.currentTimeMillis());
      v.put("player", p.name);
      v.put("game", GameEngine.TYPES[game]);
      v.put("won", correct ? 1 : 0);
      v.put("sips", sips);
      v.put("points", points);
      v.put("role", "PREDICTION");
      db.insert("history", null, v);
      db.execSQL("INSERT OR IGNORE INTO stats(name) VALUES(?)", new Object[] {p.name});
      db.execSQL("UPDATE stats SET predictions=predictions+1,correct_predictions=correct_predictions+?,"
          + "drinks=drinks+?,sips=sips+?,points=points+? WHERE name=?",
          new Object[] {correct ? 1 : 0, sips > 0 ? 1 : 0, sips, points, p.name});
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  public void recordCrew(GameEngine.Player p, int game, boolean correct, int points) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("ts", System.currentTimeMillis());
      v.put("player", p.name);
      v.put("game", GameEngine.TYPES[game]);
      v.put("won", correct ? 1 : 0);
      v.put("sips", 0);
      v.put("points", points);
      v.put("role", "CREW");
      db.insert("history", null, v);
      db.execSQL("INSERT OR IGNORE INTO stats(name) VALUES(?)", new Object[] {p.name});
      db.execSQL("UPDATE stats SET points=points+? WHERE name=?", new Object[] {points, p.name});
      db.execSQL("INSERT OR IGNORE INTO game_stats(name,game) VALUES(?,?)",
          new Object[] {p.name, GameEngine.TYPES[game]});
      db.execSQL("UPDATE game_stats SET points=points+? WHERE name=? AND game=?",
          new Object[] {points, p.name, GameEngine.TYPES[game]});
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  public void recordRulePenalty(String name) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("ts", System.currentTimeMillis());
      v.put("player", name);
      v.put("game", "ROOM_RULE");
      v.put("won", 0);
      v.put("sips", 1);
      v.put("points", 0);
      v.put("role", "RULE");
      db.insert("history", null, v);
      db.execSQL("INSERT OR IGNORE INTO stats(name) VALUES(?)", new Object[] {name});
      db.execSQL("UPDATE stats SET drinks=drinks+1,sips=sips+1 WHERE name=?", new Object[] {name});
      db.setTransactionSuccessful();
    } finally { db.endTransaction(); }
  }

  public ArrayList<String[]> gameBreakdown() {
    ArrayList<String[]> rows = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(
        "SELECT game,SUM(games),SUM(wins),SUM(sips),SUM(points) FROM game_stats"
            + " GROUP BY game ORDER BY SUM(games) DESC,game LIMIT 10", null)) {
      while (c.moveToNext()) rows.add(new String[] {
          c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)});
    }
    return rows;
  }

  public ArrayList<String[]> leaderboard() {
    ArrayList<String[]> rows = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT name,wins,games,drinks,points,sips FROM stats ORDER BY points DESC,wins DESC"
                    + " LIMIT 20",
                null)) {
      while (c.moveToNext())
        rows.add(
            new String[] {
              c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)
            });
    }
    return rows;
  }
}

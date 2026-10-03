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
    super(context, "apero_royale.db", null, 2);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE session(id INTEGER PRIMARY KEY, data TEXT NOT NULL)");
    db.execSQL(
        "CREATE TABLE stats(name TEXT PRIMARY KEY COLLATE NOCASE, wins INTEGER NOT NULL DEFAULT 0,"
            + " games INTEGER NOT NULL DEFAULT 0, drinks INTEGER NOT NULL DEFAULT 0, sips INTEGER NOT NULL DEFAULT 0, points INTEGER"
            + " NOT NULL DEFAULT 0)");
    db.execSQL(
        "CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, player"
            + " TEXT NOT NULL, game TEXT NOT NULL, won INTEGER NOT NULL, sips INTEGER NOT NULL DEFAULT 0)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE stats ADD COLUMN sips INTEGER NOT NULL DEFAULT 0");
      db.execSQL("UPDATE stats SET sips=drinks");
      db.execSQL("ALTER TABLE history ADD COLUMN sips INTEGER NOT NULL DEFAULT 0");
      db.execSQL("UPDATE history SET sips=CASE WHEN won=0 THEN 1 ELSE 0 END");
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

  public void record(GameEngine.Player p, int game, boolean won, int points, int drinks, int sips) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      ContentValues v = new ContentValues();
      v.put("ts", System.currentTimeMillis());
      v.put("player", p.name);
      v.put("game", GameEngine.TYPES[game]);
      v.put("won", won ? 1 : 0);
      v.put("sips", sips);
      db.insert("history", null, v);
      db.execSQL("INSERT OR IGNORE INTO stats(name) VALUES(?)", new Object[] {p.name});
      db.execSQL(
          "UPDATE stats SET wins=wins+?, games=games+1, drinks=drinks+?, sips=sips+?, points=MAX(0,points+?)"
              + " WHERE name=?",
          new Object[] {won ? 1 : 0, drinks, sips, points, p.name});
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
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

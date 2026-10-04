package com.aperoroyale;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import org.json.JSONObject;

/** A small LAN room: the host owns the state, clients send touches and receive snapshots. */
public class PartyNetwork {
  public interface Events {
    void joined(String name, String language);

    void action(String name, float x, float y, int kind, float height);

    void command(String name, String command, int value);

    void profile(String name, String photo);

    void snapshot(JSONObject state);

    void status(String message);
  }

  private final Events events;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final java.util.concurrent.ExecutorService writes =
      java.util.concurrent.Executors.newSingleThreadExecutor();
  private ServerSocket server;
  private Socket client;
  private BufferedWriter hostWriter;
  private final ArrayList<Socket> peers = new ArrayList<>();
  private final HashMap<Socket, String> names = new HashMap<>();
  private String roomCode = "";
  public boolean hosting = false, connected = false;
  public String localName = "";

  public PartyNetwork(Events events) {
    this.events = events;
  }

  public String ip() {
    try {
      Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
      while (all.hasMoreElements()) {
        NetworkInterface n = all.nextElement();
        if (n.isLoopback() || !n.isUp()) continue;
        Enumeration<InetAddress> addrs = n.getInetAddresses();
        while (addrs.hasMoreElements()) {
          InetAddress a = addrs.nextElement();
          String s = a.getHostAddress();
          if (s != null && s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !s.startsWith("169.254."))
            return s;
        }
      }
    } catch (Exception ignored) {
    }
    return "?";
  }

  public String roomCode() {
    return roomCode;
  }

  public void host() {
    close();
    hosting = true;
    roomCode =
        String.format(
            java.util.Locale.ROOT, "%06d", new java.security.SecureRandom().nextInt(1000000));
    new Thread(
            () -> {
              try {
                server = new ServerSocket(43867);
                ui.post(() -> events.status("HOST " + ip() + ":43867 • PIN " + roomCode));
                while (!server.isClosed()) {
                  Socket s = server.accept();
                  s.setSoTimeout(0);
                  synchronized (peers) {
                    if (peers.size() >= 6) {
                      sendError(s, "Room full");
                      s.close();
                      continue;
                    }
                    peers.add(s);
                  }
                  new Thread(() -> readPeer(s), "party-peer").start();
                }
              } catch (Exception e) {
                if (hosting) ui.post(() -> events.status("LAN: " + e.getMessage()));
              }
            },
            "party-host")
        .start();
  }

  private void readPeer(Socket s) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
      String line;
      while ((line = in.readLine()) != null) {
        JSONObject j = new JSONObject(line);
        String type = j.optString("type");
        if ("JOIN".equals(type)) {
          String name = j.optString("name");
          String lang = j.optString("language", "FR");
          if (name.length() > 16 || name.trim().isEmpty() || !roomCode.equals(j.optString("pin"))) {
            sendError(s, "Invalid room PIN or name");
            return;
          }
          synchronized (names) {
            names.put(s, name);
          }
          ui.post(() -> events.joined(name, lang));
        } else if ("ACTION".equals(type) || "COMMAND".equals(type) || "PROFILE".equals(type)) {
          String name;
          synchronized (names) {
            name = names.get(s);
          }
          if (name != null) {
            if ("PROFILE".equals(type)) {
              String photo = j.optString("photo", "");
              if (photo.length() <= 60000) ui.post(() -> events.profile(name, photo));
            } else if ("COMMAND".equals(type)) {
              String command = j.optString("command");
              int value = j.optInt("value", -1);
              ui.post(() -> events.command(name, command, value));
            } else {
              float x = (float) j.optDouble("x");
              float y = (float) j.optDouble("y");
              int kind = j.optInt("kind");
              float height = (float) j.optDouble("height", 800);
              ui.post(() -> events.action(name, x, y, kind, height));
            }
          }
        }
      }
    } catch (Exception ignored) {
    } finally {
      synchronized (peers) {
        peers.remove(s);
      }
      synchronized (names) {
        names.remove(s);
      }
      try {
        s.close();
      } catch (Exception ignored) {
      }
    }
  }

  public void join(String ip, String name, String language, String pin) {
    close();
    localName = name;
    new Thread(
            () -> {
              try {
                client = new Socket(ip.trim(), 43867);
                hostWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                connected = true;
                JSONObject j = new JSONObject();
                j.put("type", "JOIN");
                j.put("name", name);
                j.put("language", language);
                j.put("pin", pin.trim());
                send(j);
                ui.post(() -> events.status("CONNECTED"));
                try (BufferedReader in =
                    new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                  String line;
                  while ((line = in.readLine()) != null) {
                    JSONObject state = new JSONObject(line);
                    if ("ERROR".equals(state.optString("type"))) {
                      ui.post(() -> events.status(state.optString("message")));
                      break;
                    }
                    ui.post(() -> events.snapshot(state));
                  }
                }
              } catch (Exception e) {
                ui.post(() -> events.status("LAN: " + e.getMessage()));
              } finally {
                connected = false;
              }
            },
            "party-client")
        .start();
  }

  private void sendError(Socket s, String message) {
    try {
      BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      JSONObject j = new JSONObject();
      j.put("type", "ERROR");
      j.put("message", message);
      w.write(j.toString());
      w.newLine();
      w.flush();
    } catch (Exception ignored) {
    }
  }

  public void reject(String name) {
    writes.execute(
        () -> {
          synchronized (peers) {
            for (Socket s : new ArrayList<>(peers))
              if (name.equals(names.get(s))) {
                sendError(s, "Room full or duplicate name");
                try {
                  s.close();
                } catch (Exception ignored) {
                }
              }
          }
        });
  }

  public void action(float x, float y, int kind, float height) {
    if (!connected) return;
    try {
      JSONObject j = new JSONObject();
      j.put("type", "ACTION");
      j.put("x", x);
      j.put("y", y);
      j.put("kind", kind);
      j.put("height", height);
      send(j);
    } catch (Exception ignored) {
    }
  }

  public void command(String name, int value) {
    if (!connected) return;
    try {
      JSONObject j = new JSONObject();
      j.put("type", "COMMAND");
      j.put("command", name);
      j.put("value", value);
      send(j);
    } catch (Exception ignored) {
    }
  }

  public void profile(String photo) {
    if (!connected || photo.length() > 60000) return;
    try {
      JSONObject j = new JSONObject();
      j.put("type", "PROFILE");
      j.put("photo", photo);
      send(j);
    } catch (Exception ignored) {
    }
  }

  public boolean isRemote(String name) {
    synchronized (names) { return names.containsValue(name); }
  }

  private void send(JSONObject j) {
    writes.execute(
        () -> {
          try {
            if (hostWriter != null) {
              hostWriter.write(j.toString());
              hostWriter.newLine();
              hostWriter.flush();
            }
          } catch (Exception ignored) {
          }
        });
  }

  public void broadcast(JSONObject j) {
    if (!hosting) return;
    String line = j.toString() + "\n";
    writes.execute(
        () -> {
          synchronized (peers) {
            for (Socket s : new ArrayList<>(peers))
              try {
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                w.write(line);
                w.flush();
              } catch (Exception ignored) {
              }
          }
        });
  }

  /** Send each peer only the state needed for its role in this round. */
  public void broadcast(GameEngine game) {
    if (!hosting) return;
    HashMap<Socket, JSONObject> states = new HashMap<>();
    synchronized (peers) {
      synchronized (names) {
        for (Socket s : peers) {
          String name = names.get(s);
          if (name != null) states.put(s, game.networkJsonFor(name));
        }
      }
    }
    writes.execute(() -> {
      for (java.util.Map.Entry<Socket, JSONObject> entry : states.entrySet())
        try {
          BufferedWriter w = new BufferedWriter(new OutputStreamWriter(entry.getKey().getOutputStream()));
          w.write(entry.getValue().toString());
          w.newLine();
          w.flush();
        } catch (Exception ignored) { }
    });
  }

  public void close() {
    hosting = connected = false;
    try {
      if (server != null) server.close();
    } catch (Exception ignored) {
    }
    try {
      if (client != null) client.close();
    } catch (Exception ignored) {
    }
    synchronized (peers) {
      for (Socket s : peers)
        try {
          s.close();
        } catch (Exception ignored) {
        }
      peers.clear();
    }
    hostWriter = null;
  }
}

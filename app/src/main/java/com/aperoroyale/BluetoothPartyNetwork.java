package com.aperoroyale;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** RFCOMM room for paired nearby phones. Uses the same host-authoritative JSON protocol as Wi-Fi. */
public final class BluetoothPartyNetwork extends PartyNetwork {
  private static final UUID SERVICE_UUID = UUID.fromString("9c2f9a40-5199-4e0d-b385-87796378b92e");
  private final Events events;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final ExecutorService writes = Executors.newSingleThreadExecutor();
  private final ArrayList<Peer> peers = new ArrayList<>();
  private BluetoothServerSocket server;
  private Peer upstream;
  private String pin = "";
  private volatile int generation = 0;

  private static final class Peer {
    final BluetoothSocket socket;
    final BufferedReader reader;
    final BufferedWriter writer;
    String name = "";

    Peer(BluetoothSocket socket) throws Exception {
      this.socket = socket;
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
    }

    void write(JSONObject message) throws Exception {
      writer.write(message.toString());
      writer.newLine();
      writer.flush();
    }

    void close() { try { socket.close(); } catch (Exception ignored) { } }
  }

  public BluetoothPartyNetwork(Events events) {
    super(events);
    this.events = events;
  }

  @Override public String ip() { return "Bluetooth"; }
  @Override public String roomCode() { return pin; }

  public void hostBluetooth(BluetoothAdapter adapter) {
    close();
    hosting = true;
    pin = String.format(Locale.ROOT, "%06d", new java.security.SecureRandom().nextInt(1000000));
    int epoch = generation;
    new Thread(() -> {
      try {
        BluetoothServerSocket listener = adapter.listenUsingRfcommWithServiceRecord("Apero Royale", SERVICE_UUID);
        server = listener;
        ui.post(() -> events.status("BLUETOOTH • PIN " + pin));
        while (hosting && epoch == generation) {
          BluetoothSocket socket = listener.accept();
          if (epoch != generation) { socket.close(); break; }
          Peer peer = new Peer(socket);
          synchronized (peers) {
            if (peers.size() >= 6) {
              error(peer, "Room full");
              peer.close();
              continue;
            }
            peers.add(peer);
          }
          new Thread(() -> readPeer(peer), "party-bt-peer").start();
        }
      } catch (Exception e) {
        if (hosting && epoch == generation) ui.post(() -> events.status("Bluetooth: " + e.getMessage()));
      }
    }, "party-bt-host").start();
  }

  private void readPeer(Peer peer) {
    try {
      String line;
      while ((line = peer.reader.readLine()) != null) {
        if (line.length() > 1000000) break;
        JSONObject message = new JSONObject(line);
        String type = message.optString("type");
        if ("JOIN".equals(type)) {
          if (!peer.name.isEmpty()) continue;
          String name = message.optString("name").trim();
          if (name.isEmpty() || name.length() > 16 || !pin.equals(message.optString("pin"))) {
            error(peer, "Invalid PIN or name");
            break;
          }
          peer.name = name;
          String language = message.optString("language", "FR");
          ui.post(() -> events.joined(name, language));
        } else if (!peer.name.isEmpty()) {
          String name = peer.name;
          if ("ACTION".equals(type)) {
            float x = (float) message.optDouble("x");
            float y = (float) message.optDouble("y");
            int kind = message.optInt("kind");
            float height = (float) message.optDouble("height", 800);
            ui.post(() -> events.action(name, x, y, kind, height));
          } else if ("COMMAND".equals(type)) {
            String command = message.optString("command");
            int value = message.optInt("value", -1);
            ui.post(() -> events.command(name, command, value));
          } else if ("PROFILE".equals(type)) {
            String photo = message.optString("photo", "");
            if (photo.length() <= 60000) ui.post(() -> events.profile(name, photo));
          }
        }
      }
    } catch (Exception ignored) {
    } finally {
      synchronized (peers) { peers.remove(peer); }
      peer.close();
    }
  }

  public void joinBluetooth(BluetoothDevice device, String name, String language, String roomPin) {
    close();
    localName = name;
    localLanguage = language;
    int epoch = generation;
    new Thread(() -> {
      try {
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
        socket.connect();
        Peer peer = new Peer(socket);
        if (epoch != generation) { peer.close(); return; }
        upstream = peer;
        connected = true;
        JSONObject join = new JSONObject();
        join.put("type", "JOIN");
        join.put("name", name);
        join.put("language", language);
        join.put("pin", roomPin);
        writes.execute(() -> { try { peer.write(join); } catch (Exception ignored) { } });
        ui.post(() -> events.status("BLUETOOTH CONNECTED"));
        String line;
        while (epoch == generation && (line = peer.reader.readLine()) != null) {
          if (line.length() > 1000000) break;
          JSONObject state = new JSONObject(line);
          if ("ERROR".equals(state.optString("type"))) {
            ui.post(() -> events.status(state.optString("message")));
            break;
          }
          ui.post(() -> events.snapshot(state));
        }
      } catch (Exception e) {
        if (epoch == generation) ui.post(() -> events.status("Bluetooth: " + e.getMessage()));
      } finally {
        if (epoch == generation) connected = false;
        if (upstream != null) upstream.close();
      }
    }, "party-bt-client").start();
  }

  private void error(Peer peer, String reason) {
    try {
      JSONObject message = new JSONObject();
      message.put("type", "ERROR");
      message.put("message", reason);
      peer.write(message);
    } catch (Exception ignored) { }
  }

  @Override public void reject(String name) {
    writes.execute(() -> {
      synchronized (peers) {
        for (Peer peer : new ArrayList<>(peers))
          if (name.equals(peer.name)) { error(peer, "Room full or duplicate name"); peer.close(); }
      }
    });
  }

  @Override public boolean isRemote(String name) {
    synchronized (peers) { for (Peer peer : peers) if (name.equals(peer.name)) return true; }
    return false;
  }

  @Override public void action(float x, float y, int kind, float height) {
    try {
      JSONObject message = new JSONObject();
      message.put("type", "ACTION");
      message.put("x", x);
      message.put("y", y);
      message.put("kind", kind);
      message.put("height", height);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void command(String command, int value) {
    try {
      JSONObject message = new JSONObject();
      message.put("type", "COMMAND");
      message.put("command", command);
      message.put("value", value);
      send(message);
    } catch (Exception ignored) { }
  }

  @Override public void profile(String photo) {
    if (photo.length() > 60000) return;
    try {
      JSONObject message = new JSONObject();
      message.put("type", "PROFILE");
      message.put("photo", photo);
      send(message);
    } catch (Exception ignored) { }
  }

  private void send(JSONObject message) {
    if (!connected) return;
    writes.execute(() -> { try { if (upstream != null) upstream.write(message); } catch (Exception ignored) { } });
  }

  @Override public void broadcast(JSONObject state) {
    if (!hosting) return;
    writes.execute(() -> {
      synchronized (peers) {
        for (Peer peer : new ArrayList<>(peers))
          try { peer.write(state); } catch (Exception ignored) { peer.close(); }
      }
    });
  }

  @Override public void broadcast(GameEngine game) {
    if (!hosting) return;
    ArrayList<Peer> current;
    synchronized (peers) { current = new ArrayList<>(peers); }
    ArrayList<JSONObject> states = new ArrayList<>();
    for (Peer peer : current) states.add(game.networkJsonFor(peer.name));
    writes.execute(() -> {
      for (int i = 0; i < current.size(); i++)
        try { current.get(i).write(states.get(i)); }
        catch (Exception ignored) { current.get(i).close(); }
    });
  }

  @Override public void close() {
    generation++;
    hosting = connected = false;
    try { if (server != null) server.close(); } catch (Exception ignored) { }
    if (upstream != null) upstream.close();
    synchronized (peers) {
      for (Peer peer : peers) peer.close();
      peers.clear();
    }
    server = null;
    upstream = null;
    super.close();
  }
}

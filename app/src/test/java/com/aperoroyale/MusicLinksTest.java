package com.aperoroyale;

import static org.junit.Assert.*;
import org.junit.Test;

public final class MusicLinksTest {
  @Test public void acceptsOnlyHttpsLinksForSelectedProvider() {
    assertTrue(MusicLinks.allowed(MusicLinks.SPOTIFY,
        "https://open.spotify.com/playlist/abc"));
    assertTrue(MusicLinks.allowed(MusicLinks.DEEZER,
        "https://www.deezer.com/playlist/123"));
    assertTrue(MusicLinks.allowed(MusicLinks.APPLE,
        "https://music.apple.com/fr/playlist/example"));
    assertTrue(MusicLinks.allowed(MusicLinks.AMAZON,
        "https://music.amazon.fr/playlists/example"));
    assertFalse(MusicLinks.allowed(MusicLinks.SPOTIFY,
        "https://open.spotify.com.evil.example/playlist/abc"));
    assertFalse(MusicLinks.allowed(MusicLinks.SPOTIFY,
        "http://open.spotify.com/playlist/abc"));
    assertFalse(MusicLinks.allowed(MusicLinks.DEEZER,
        "https://open.spotify.com/playlist/abc"));
  }

  @Test public void invalidCustomLinkFallsBackToProviderHome() {
    assertEquals("https://music.apple.com/",
        MusicLinks.destination(MusicLinks.APPLE, "https://wrong.example/"));
  }
}

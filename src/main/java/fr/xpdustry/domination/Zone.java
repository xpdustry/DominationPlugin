package fr.xpdustry.domination;

import arc.math.geom.*;
import arc.struct.*;
import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import org.checkerframework.checker.nullness.qual.*;
import org.jetbrains.annotations.Nullable;

public final class Zone implements Position {

  private final int x;
  private final int y;

  private transient Team team = Team.derelict;
  private transient float percent = 100F;

  public Zone(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void update(@NonNull DominationMapConfig map) {
    // Reset the team if the team got beaten
    if (!team.active()) team = Team.derelict;

    // Count the number of players in the zone, per team
    ObjectIntMap<Team> players = new ObjectIntMap<>();
    Groups.player.each(p -> {
      if (p.within(this, map.getZoneRadius())) players.increment(p.team());
    });

    // Search for the team with the most players
    var maxPlayers = 0;
    var winner = Team.derelict;

    for (final var entry : players) {
      if (entry.value > maxPlayers) {
        winner = entry.key;
        maxPlayers = entry.value;
      } else if (entry.value == maxPlayers) {
        // If 2 teams have the same number of players, don't update so set back to derelict.
        winner = Team.derelict;
      }
    }

    // Updates the zone values
    if (winner != Team.derelict) {
      if (team == winner) {
        percent = Math.min(percent + map.getCaptureRate(), 100F);
      } else {
        percent = Math.max(percent - map.getCaptureRate(), 0F);
        if (percent == 0) team = winner;
      }
    }
  }

  public @NonNull Team getTeam() {
    return team;
  }

  public float getPercent() {
    return percent;
  }

  @Override
  public float getX() {
    return x * Vars.tilesize;
  }

  @Override
  public float getY() {
    return y * Vars.tilesize;
  }

  public int getTileX() {
    return x;
  }

  public int getTileY() {
    return y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final var other = (Zone) o;
    return x == other.x && y == other.y;
  }

  public static class ZoneAdapter extends TypeAdapter<Zone> {

    @Override
    public void write(JsonWriter writer, Zone value) throws IOException {
      if (value == null) {
        writer.nullValue();
      } else {
        writer.value(value.getTileX() + ", " + value.getTileY());
      }
    }

    @Override
    public @Nullable Zone read(JsonReader reader) throws IOException {
      if (reader.peek() == JsonToken.NULL) {
        reader.nextNull();
        return null;
      }

      String text = reader.nextString();
      String[] coords = text.split(",", 2);

        if (coords.length != 2) {
            throw new IOException(text + " is not a coordinate.");
        }

      for (int i = 0; i < coords.length; i++) {
        coords[i] = coords[i].trim();
      }

      return new Zone(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
    }
  }
}

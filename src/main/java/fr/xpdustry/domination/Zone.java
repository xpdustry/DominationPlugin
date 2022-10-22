/*
 * DominationPlugin, a "capture the zone" like gamemode plugin.
 *
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

  public Zone(final int x, final int y) {
    this.x = x;
    this.y = y;
  }

  public void update(final @NonNull DominationMapConfig map) {
    // Reset the team if the team got beaten
    if (!team.active()) team = Team.derelict;

    // Count the number of players in the zone, per team
    ObjectIntMap<Team> players = new ObjectIntMap<>();
    Groups.player.each(p -> {
      if (p.within(this, map.getZoneRadius() * Vars.tilesize)) players.increment(p.team());
    });

    // Search for the team with the most players
    var winner = Team.derelict;
    var maxPlayers = 0;

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
  public boolean equals(final @Nullable Object o) {
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

      final var text = reader.nextString();
      final var coords = text.split(",", 2);

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

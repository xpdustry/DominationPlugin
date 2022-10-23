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

import arc.struct.*;
import com.google.gson.*;
import com.google.gson.stream.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DominationZone {

  private float x;
  private float y;
  private float radius;

  private Team team = Team.derelict;
  private float percent = 100F;

  public DominationZone(final float x, final float y, final float radius, final Team team, final float percent) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    this.team = team;
    this.percent = percent;
  }

  public void update(final float captureRate) {
    // Reset the team if the team got beaten
    if (!team.active()) {
      team = Team.derelict;
    }

    // Count the number of players in the zone, per team
    final var players = new ObjectIntMap<Team>();
    Groups.player.each(player -> {
      if (player.within(getWorldX(), getWorldY(), this.radius * Vars.tilesize)) {
        players.increment(player.team());
      }
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
        percent = Math.min(percent + captureRate, 100F);
      } else {
        percent = Math.max(percent - captureRate, 0F);
        if (percent == 0) {
          team = winner;
        }
      }
    }
  }

  public float getX() {
    return x;
  }

  public void setX(final float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(final float y) {
    this.y = y;
  }

  public float getRadius() {
    return radius;
  }

  public void setRadius(final float radius) {
    this.radius = radius;
  }

  public Team getTeam() {
    return team;
  }

  public void setTeam(final Team team) {
    this.team = team;
  }

  public float getPercent() {
    return percent;
  }

  public void setPercent(final float percent) {
    this.percent = percent;
  }

  public float getWorldX() {
    return x * Vars.tilesize;
  }

  public float getWorldY() {
    return y * Vars.tilesize;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public boolean equals(Object o) {
    return  this == o || (
      o instanceof DominationZone zone
        && Float.compare(zone.x, x) == 0
        && Float.compare(zone.y, y) == 0
        && Float.compare(zone.radius, radius) == 0
        && team.id == zone.team.id
        && Float.compare(zone.percent, percent) == 0
      );
  }

  static final class Adapter extends TypeAdapter<DominationZone> {

    @Override
    public void write(final JsonWriter writer, final DominationZone value) throws IOException {
      if (value == null) {
        writer.nullValue();
      } else {
        writer.value(value.x + ',' + value.y + ',' + value.radius + ',' + value.team.id + ',' + value.percent);
      }
    }

    @Override
    public @Nullable DominationZone read(JsonReader reader) throws IOException {
      if (reader.peek() == JsonToken.NULL) {
        reader.nextNull();
        return null;
      }

      final var text = reader.nextString();
      final var info = text.split(",", 5);

      if (info.length != 5) {
        throw new IOException(text + " is not a coordinate.");
      }

      for (int i = 0; i < info.length; i++) {
        info[i] = info[i].trim();
      }

      return new DominationZone(
        Float.parseFloat(info[0]),
        Float.parseFloat(info[1]),
        Float.parseFloat(info[2]),
        Team.get(Integer.parseInt(info[3])),
        Float.parseFloat(info[4])
      );
    }
  }
}

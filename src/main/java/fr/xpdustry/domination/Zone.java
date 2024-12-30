/*
 * Domination, a "capture the zone" like gamemode plugin.
 *
 * Copyright (C) 2024  Xpdustry
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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Objects;
import mindustry.Vars;
import mindustry.game.Team;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO add name to zone
public final class Zone {

    private int x;
    private int y;
    private int radius;

    private transient Team team = Team.derelict;
    private transient int capture = 100;

    public Zone(final int x, final int y, final int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public Zone(final int x, final int y) {
        this(x, y, 5 * Vars.tilesize);
    }

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public float getRadius() {
        return radius * Vars.tilesize;
    }

    public void setRadius(final int radius) {
        this.radius = radius;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(final Team team) {
        this.team = team;
    }

    public int getCapture() {
        return capture;
    }

    public void setCapture(final int capture) {
        this.capture = capture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof Zone zone && zone.x == x && zone.y == y && zone.radius == radius);
    }

    static final class Adapter extends TypeAdapter<Zone> {

        @Override
        public void write(final JsonWriter writer, final @Nullable Zone value) throws IOException {
            if (value == null) {
                writer.nullValue();
            } else {
                writer.value(value.x + ", " + value.y + ", " + value.radius);
            }
        }

        @Override
        public @Nullable Zone read(final JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }

            final var text = reader.nextString();
            final var info = text.split(",", 3);

            if (info.length != 3) {
                throw new IOException(text + " is not valid zone data (x,y,radius).");
            }

            for (int i = 0; i < info.length; i++) {
                info[i] = info[i].trim();
            }

            return new Zone(Integer.parseInt(info[0]), Integer.parseInt(info[1]), Integer.parseInt(info[2]));
        }
    }
}

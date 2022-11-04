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

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import org.checkerframework.checker.nullness.qual.*;

final class DurationAdapter extends TypeAdapter<Duration> {

  @Override
  public void write(final JsonWriter writer, final Duration value) throws IOException {
    if (value == null) {
      writer.nullValue();
    } else {
      writer.value(value.toString());
    }
  }

  @Override
  public @Nullable Duration read(final JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull();
      return null;
    }
    try {
      return Duration.parse(reader.nextString());
    } catch (final DateTimeParseException e) {
      throw new IOException("Failed to parse the duration.", e);
    }
  }
}

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

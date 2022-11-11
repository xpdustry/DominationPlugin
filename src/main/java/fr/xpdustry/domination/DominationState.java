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

import java.time.*;
import java.util.*;
import java.util.stream.*;
import mindustry.game.*;
import net.mindustry_ddns.filestore.*;

public final class DominationState {

  // TODO Separate zone data and zone state with a map
  private static final Duration ONE_HOUR = Duration.ofHours(1L);
  private final Store<List<Zone>> zones;
  private final Instant start = Instant.now(Clock.systemUTC());

  public DominationState(final Store<List<Zone>> zones) {
    this.zones = zones;
  }

  public Collection<Zone> getZones() {
    return zones.get();
  }

  public Instant getStart() {
    return start;
  }

  public Duration getRemainingTime() {
    final var now = Instant.now(Clock.systemUTC());
    return start.plus(ONE_HOUR).isBefore(now)
      ? Duration.ZERO
      : ONE_HOUR.minus(Duration.between(start, now));
  }

  public Map<Team, Integer> getLeaderboard() {
    return getZones().stream().collect(Collectors.toUnmodifiableMap(Zone::getTeam, Zone::getCapture, Integer::sum));
  }

  public void save() {
    zones.save();
  }
}

package fr.xpdustry.domination;

import java.time.*;
import java.util.*;
import java.util.stream.*;
import mindustry.game.*;
import net.mindustry_ddns.filestore.*;

public final class DominationState {

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

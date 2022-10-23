package fr.xpdustry.domination;

import java.time.*;
import java.util.*;

public final class DominationMap {

  private final Set<DominationZone> zones = new HashSet<>();
  private Duration gameDuration = Duration.ofMinutes(30L);
  private Duration showdownDuration = Duration.ofMinutes(5L);
  private float captureRate = 5F;

  public Collection<DominationZone> getZones() {
    return Collections.unmodifiableCollection(zones);
  }

  public void addZone(final DominationZone zone) {
    zones.add(zone);
  }

  public void removeZone(final DominationZone zone) {
    zones.remove(zone);
  }

  public Duration getGameDuration() {
    return gameDuration;
  }

  public void setGameDuration(final Duration gameDuration) {
    this.gameDuration = gameDuration;
  }

  public Duration getShowdownDuration() {
    return showdownDuration;
  }

  public void setShowdownDuration(final Duration showdownDuration) {
    this.showdownDuration = showdownDuration;
  }

  public float getCaptureRate() {
    return captureRate;
  }

  public void setCaptureRate(final float captureRate) {
    this.captureRate = captureRate;
  }
}

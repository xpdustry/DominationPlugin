package fr.xpdustry.domination;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import java.util.*;
import java.util.function.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.net.*;
import org.checkerframework.checker.nullness.qual.*;
import org.jetbrains.annotations.*;

public final class DominationMapConfig implements Iterable<Zone> {

  private static final Seq<Effect> EFFECTS = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

  private int zoneRadius = 5;
  private float captureRate = 5F;
  private float gameDuration = 30F;
  private float showdownDuration = 5F;
  private final Set<Zone> zones = new HashSet<>();

  public void addZone(final @NonNull Zone zone) {
    zones.add(zone);
  }

  public void removeZone(final @NonNull Zone zone) {
    zones.remove(zone);
  }

  public boolean hasZone(final @NonNull Zone zone) {
    return zones.contains(zone);
  }

  @Override
  public @NonNull Iterator<Zone> iterator() {
    return zones.iterator();
  }

  @Override
  public void forEach(final @NonNull Consumer<? super Zone> action) {
    zones.forEach(action);
  }

  @Override
  public @NotNull Spliterator<Zone> spliterator() {
    return zones.spliterator();
  }

  public void drawZoneCenters(final @NonNull NetConnection con) {
    zones.forEach(z -> Call.effect(con, Fx.unitLand, z.getX(), z.getY(), 0, z.getTeam().color));
  }

  public void drawZoneCircles() {
    zones.forEach(z -> {
      Geometry.iteratePolygon((px, py) -> {
        Call.effect(EFFECTS.random(), px + z.getX(), py + z.getY(), 0, z.getTeam().color);
      }, createZoneCircle());
    });
  }

  public void drawZoneCircles(final @NonNull NetConnection con) {
    zones.forEach(z -> {
      Geometry.iteratePolygon((px, py) -> {
        Call.effect(con, EFFECTS.random(), px + z.getX(), py + z.getY(), 0, z.getTeam().color);
      }, createZoneCircle());
    });
  }

  public @NotNull Collection<Zone> getZones() {
    return Collections.unmodifiableSet(zones);
  }

  public int getZoneRadius() {
    return zoneRadius;
  }

  public void setZoneRadius(final int zoneRadius) {
    this.zoneRadius = zoneRadius;
  }

  public float getCaptureRate() {
    return captureRate;
  }

  public void setCaptureRate(final float captureRate) {
    this.captureRate = captureRate;
  }

  public float getGameDuration() {
    return gameDuration;
  }

  public void setGameDuration(final float gameDuration) {
    this.gameDuration = gameDuration;
  }

  public float getShowdownDuration() {
    return showdownDuration;
  }

  public void setShowdownDuration(final float showdownDuration) {
    this.showdownDuration = showdownDuration;
  }

  private float[] createZoneCircle() {
    return Geometry.regPoly((int) (Mathf.pi * zoneRadius), zoneRadius * Vars.tilesize);
  }
}

package fr.xpdustry.domination;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.net.*;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.*;


public final class DominationMapConfig implements Iterable<Zone>{
    private static final Seq<Effect> EFFECTS =  Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

    private final int zoneRadius;
    private final float captureRate;
    private final float gameDuration;
    private final float showdownDuration;
    private final Set<Zone> zones = new HashSet<>();

    public DominationMapConfig(
        final int zoneRadius,
        final float captureRate,
        final float gameDuration,
        final float showdownDuration
    ){
        this.zoneRadius = zoneRadius;
        this.captureRate = captureRate;
        this.gameDuration = gameDuration;
        this.showdownDuration = showdownDuration;
    }

    public DominationMapConfig(){
        this(5, 5F, 30F, 5F);
    }

    public void addZone(final @NonNull Zone zone){
        zones.add(zone);
    }

    public void removeZone(final @NonNull Zone zone){
        zones.remove(zone);
    }

    public boolean hasZone(final @NonNull Zone zone){
        return zones.contains(zone);
    }

    @Override public @NonNull Iterator<Zone> iterator(){
        return zones.iterator();
    }

    @Override public void forEach(final @NonNull Consumer<? super Zone> action){
        zones.forEach(action);
    }

    @Override public Spliterator<Zone> spliterator(){
        return zones.spliterator();
    }

    public void drawZoneCenters(final @NonNull NetConnection con){
        zones.forEach(z -> Call.effect(con, Fx.unitLand, z.getX(), z.getY(), 0, z.getTeam().color));
    }

    public void drawZoneCircles(){
        zones.forEach(z -> {
            Geometry.iteratePolygon((px, py) -> {
                Call.effect(EFFECTS.random(), px + z.getX(), py + z.getY() , 0, z.getTeam().color);
            }, createZoneCircle());
        });
    }

    public void drawZoneCircles(final @NonNull NetConnection con){
        zones.forEach(z -> {
            Geometry.iteratePolygon((px, py) -> {
                Call.effect(con, EFFECTS.random(), px + z.getX(), py + z.getY(), 0, z.getTeam().color);
            }, createZoneCircle());
        });
    }

    public Set<Zone> getZones(){
        return Collections.unmodifiableSet(zones);
    }

    public float getZoneRadius(){
        return zoneRadius * Vars.tilesize;
    }

    public float getCaptureRate(){
        return captureRate;
    }

    public float getGameDuration(){
        return gameDuration * Time.toMinutes;
    }

    public float getShowdownDuration(){
        return showdownDuration * Time.toMinutes;
    }

    private float[] createZoneCircle(){
        return Geometry.regPoly((int)(Mathf.pi * zoneRadius), zoneRadius * Vars.tilesize);
    }
}

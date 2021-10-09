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

import java.util.*;
import java.util.function.*;


public class DominationMap implements Iterable<Zone>{
    private int zoneRadius = 5;
    private float captureRate = 10F;
    private float updateTicks = Time.toSeconds;
    private float gameDuration = 10F;
    private float showdownDuration = 3F;
    private final ArrayList<Zone> zones = new ArrayList<>();

    private static final Seq<Effect> effects = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

    public int getZoneRadius(){
        return zoneRadius;
    }

    public void setZoneRadius(int zoneRadius){
        if(zoneRadius < 0) throw new IllegalArgumentException("The zone radius is negative: " + zoneRadius);
        this.zoneRadius = zoneRadius;
    }

    public float getCaptureRate(){
        return captureRate;
    }

    public void setCaptureRate(float captureRate){
        if(captureRate < 0) throw new IllegalArgumentException("The capture rate is negative: " + captureRate);
        this.captureRate = captureRate;
    }

    public float getUpdateTicks(){
        return updateTicks;
    }

    public void setUpdateTicks(float updateTicks){
        if(updateTicks < 0) throw new IllegalArgumentException("The update ticks is negative: " + updateTicks);
        this.updateTicks = updateTicks;
    }

    public float getGameDuration(){
        return gameDuration;
    }

    public void setGameDuration(float gameDuration){
        if(gameDuration < 0) throw new IllegalArgumentException("The game duration is negative: " + gameDuration);
        this.gameDuration = gameDuration;
    }

    public ArrayList<Zone> getZones(){
        return new ArrayList<>(zones);
    }

    public void addZone(Zone zone){
        zones.add(zone);
    }

    public boolean hasZone(Zone zone){
        return zones.contains(zone);
    }

    public void removeZone(Zone zone){
        zones.remove(zone);
    }

    @Nullable
    public Zone getZone(int x, int y){
        for(Zone zone : zones){
            if(zone.getX() == x && zone.getY() == y){
                return zone;
            }
        }

        return null;
    }

    @Override
    public Iterator<Zone> iterator(){
        return zones.listIterator();
    }

    @Override
    public void forEach(Consumer<? super Zone> action){
        Objects.requireNonNull(action);
        for(Zone zone : zones){
            action.accept(zone);
        }
    }

    @Override
    public Spliterator<Zone> spliterator(){
        return zones.spliterator();
    }

    public float getShowdownDuration(){
        return showdownDuration;
    }

    public void setShowdownDuration(float showdownDuration){
        if(showdownDuration < 0) throw new IllegalArgumentException("The showdown duration is negative: " + showdownDuration);
        this.showdownDuration = showdownDuration;
    }

    public void drawZoneCircles(){
        float[] circle = createZoneCircle();

        zones.forEach(z -> {
            Geometry.iteratePolygon((cx, cy) -> {
                Call.effect(effects.random(), (cx + z.getX()) * Vars.tilesize, (cy + z.getY()) * Vars.tilesize, 0, z.getTeam().color);
            }, circle);
        });
    }

    public void drawZoneCircles(NetConnection con){
        float[] circle = createZoneCircle();

        zones.forEach(z -> {
            Geometry.iteratePolygon((cx, cy) -> {
                Call.effect(con, effects.random(), (cx + z.getX()) * Vars.tilesize, (cy + z.getY()) * Vars.tilesize, 0, z.getTeam().color);
            }, circle);
        });
    }

    public void drawZoneCenters(){
        zones.forEach(z -> {
            Call.effect(Fx.unitLand, z.getX() * Vars.tilesize, z.getY() * Vars.tilesize, 0, z.getTeam().color);
        });
    }

    public void drawZoneCenters(NetConnection con){
        zones.forEach(z -> {
            Call.effect(con, Fx.unitLand, z.getX() * Vars.tilesize, z.getY() * Vars.tilesize, 0, z.getTeam().color);
        });
    }

    public void drawZoneTexts(){
        zones.forEach(z -> {
            String percent = Strings.format("[#@]@%", z.getTeam().color, Strings.fixed(z.getPercent(), 0));
            Call.label(percent, 1.0F / 6, z.getX() * Vars.tilesize, z.getY() * Vars.tilesize);
        });
    }

    public void drawZoneTexts(NetConnection con){
        zones.forEach(z -> {
            String percent = Strings.format("[#@]@%", z.getTeam().color, Strings.fixed(z.getPercent(), 0));
            Call.label(con, percent, 1.0F / 6, z.getX() * Vars.tilesize, z.getY() * Vars.tilesize);
        });
    }

    public float[] createZoneCircle(){
        return Geometry.regPoly((int)(Mathf.pi * zoneRadius), zoneRadius);
    }
}

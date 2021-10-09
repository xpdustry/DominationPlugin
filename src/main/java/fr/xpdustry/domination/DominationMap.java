package fr.xpdustry.domination;

import arc.util.*;

import java.util.*;
import java.util.function.*;


public class DominationMap implements Iterable<Zone>{
    private int zoneRadius = 5;
    private float captureRate = 10F;
    private float updateTicks = Time.toSeconds;
    private float gameDuration = 5;
    private final ArrayList<Zone> zones = new ArrayList<>();

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
}

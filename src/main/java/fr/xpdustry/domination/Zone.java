package fr.xpdustry.domination;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.IntIntMap.*;
import arc.util.*;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;


public class Zone{
    public final int x;
    public final int y;

    private transient int radius;
    private transient float[] area;

    private transient Team team;
    private transient float percent;
    private transient final IntIntMap map;

    private static final Seq<Effect> effects = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

    public Zone(int x, int y, int radius){
        this.x = x;
        this.y = y;
        // Generates a circle for rendering
        setRadius(radius);
        this.map = new IntIntMap(state.teams.getActive().size);
        // Set the default values for a new game
        reset();
    }

    public void update(float captureRate){
        // Reset the team if the team got beaten
        if(!team.active()) team = Team.derelict;
        // Clears the map of the previous results
        map.clear(state.teams.getActive().size);

        // Count the number of players in the zone, per team
        Groups.player.each(p -> {
            if(p.within(x * tilesize, y * tilesize, radius * tilesize)){
                map.increment(p.team().id);
            }
        });

        // Search for the team with the most players
        Entry winner = new Entry();
        boolean freeze = false; // If 2 teams have the same number of players, don't update.
        for(Entry entry : map){
            if(entry.value > winner.value){
                winner = entry;
                freeze = false;
            }else if(entry.value == winner.value){
                freeze = true;
            }
        }

        // Updates the zone values
        if(winner.key != 0 && !freeze){ // winner.key != 0 -> 0 is the id of derelict
            if(team.id == winner.key){
                percent = Math.min(percent + captureRate, 100F);
            }else{
                percent = Math.max(percent - captureRate, 0F);
                if(percent == 0) team = Team.get(winner.key);
            }
        }
    }

    public void render(float updateTicks){
        Geometry.iteratePolygon((cx, cy) -> {
            Call.effect(effects.random(), (cx + x) * tilesize, (cy + y) * tilesize, 0, team.color);
        }, area);

        Call.label(Strings.format("[#@]@%", team.color, Strings.fixed(percent, 0)), updateTicks / Time.toSeconds, x * tilesize, y * tilesize);
    }

    public void setRadius(int radius){
        this.radius = radius;
        this.area = Geometry.regPoly((int)(this.radius * Mathf.pi), this.radius);
    }

    public Team getTeam(){
        return team;
    }

    public float getPercent(){
        return percent;
    }

    public void reset(){
        team = Team.derelict;
        percent = 100F;
        map.clear();
    }

    @Override
    public String toString(){
        return "Zone{" + "x=" + x +
        ", y=" + y +
        ", radius=" + radius +
        ", team=" + team +
        ", percent=" + percent +
        '}';
    }
}

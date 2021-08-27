package fr.xpdustry.domination;

import arc.struct.*;
import arc.struct.IntIntMap.*;

import mindustry.game.*;
import mindustry.gen.*;

import fr.xpdustry.domination.DominationPlugin.*;

import static mindustry.Vars.*;


public class Zone{
    public final int x;
    public final int y;

    private transient Team team;
    private transient float percent;
    private transient final IntIntMap map;

    public Zone(int x, int y){
        this.x = x;
        this.y = y;
        this.map = new IntIntMap();
        // Set the default values for a new game
        reset();
    }

    public void update(DominationSettings settings){
        // Reset the team if the team got beaten
        if(!team.active()) team = Team.derelict;
        // Clears the map of the previous results
        map.clear(state.teams.getActive().size);

        // Count the number of players in the zone, per team
        Groups.player.each(p -> {
            if(p.within(x * tilesize, y * tilesize, settings.zoneRadius * tilesize)){
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
                percent = Math.min(percent + settings.captureRate, 100F);
            }else{
                percent = Math.max(percent - settings.captureRate, 0F);
                if(percent == 0) team = Team.get(winner.key);
            }
        }
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
        ", team=" + team +
        ", percent=" + percent +
        '}';
    }
}

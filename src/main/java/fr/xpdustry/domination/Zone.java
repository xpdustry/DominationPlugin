package fr.xpdustry.domination;

import arc.struct.*;
import arc.struct.ObjectIntMap.*;

import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import java.io.*;


public class Zone{
    private final int x;
    private final int y;

    private transient Team team;
    private transient float percent;

    public Zone(int x, int y){
        this.x = x;
        this.y = y;
        reset(); // Set the default values for a new game
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public void update(DominationMap map){
        // Reset the team if the team got beaten
        if(!team.active()) team = Team.derelict;

        // Count the number of players in the zone, per team
        ObjectIntMap<Team> players = new ObjectIntMap<>();
        Groups.player.each(p -> {
            if(p.within(x * Vars.tilesize, y * Vars.tilesize, map.getZoneRadius() * Vars.tilesize)){
                players.increment(p.team());
            }
        });

        // Search for the team with the most players
        int maxPlayers = 0;
        Team winner = Team.derelict;
        for(Entry<Team> entry : players){
            if(entry.value > maxPlayers){
                winner = entry.key;
                maxPlayers = entry.value;
            }else if(entry.value == maxPlayers){
                // If 2 teams have the same number of players, don't update so set back to derelict.
                winner = Team.derelict;
            }
        }

        // Updates the zone values
        if(winner != Team.derelict){
            if(team == winner){
                percent = Math.min(percent + map.getCaptureRate(), 100F);
            }else{
                percent = Math.max(percent - map.getCaptureRate(), 0F);
                if(percent == 0) team = winner;
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
    }

    public static class ZoneAdapter extends TypeAdapter<Zone>{
        @Override
        public void write(JsonWriter writer, Zone value) throws IOException{
            if(value == null){
                writer.nullValue();
            }else{
                writer.value(value.getX() + ", " + value.getY());
            }
        }

        @Override
        public Zone read(JsonReader reader) throws IOException{
            if(reader.peek() == JsonToken.NULL){
                reader.nextNull();
                return null;
            }

            String text = reader.nextString();
            String[] coords = text.split(",");

            for(int i = 0; i < coords.length; i++){
                coords[i] = coords[i].trim();
            }

            return new Zone(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
        }
    }
}

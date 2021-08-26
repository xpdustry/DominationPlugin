package fr.xpdustry.domination;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;

import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;

import fr.xpdustry.domination.DominationSettings.*;

import java.util.*;
import java.util.Map.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

@SuppressWarnings("unused")
public class DominationPlugin extends Plugin{
    private static DominationSettings settings;

    private static final Json json = new Json();
    private static final Fi config = new Fi(Core.files.external("domination-config.json").absolutePath());

    private static final Interval interval = new Interval(3);
    private static final ObjectSet<Playerc> editors = new ObjectSet<>();

    static {
        json.setOutputType(OutputType.json);
        json.setSerializer(DominationSettings.class, new DominationIO());
    }

    /** The zones of the current map, might throw a NPE if used when not playing */
    private Seq<Zone> currentZones(){
        return settings.maps.get(state.map.name(), Seq::new);
    }

    @Override
    public void init(){
        // Settings
        if(config.exists()){
            settings = json.fromJson(DominationSettings.class, config);
        }else{
            settings = new DominationSettings();
            config.writeString(json.prettyPrint(settings));
        }

        Events.run(WorldLoadEvent.class, () -> {
            // updates the radius if it has been changed
            currentZones().each(z -> z.setRadius(settings.zoneRadius));
            interval.reset(2, 0); // Resets the timer when a new game begin
        });

        Events.run(Trigger.update, () -> {
            if(state.rules.pvp && !state.gameOver){
                if(interval.get(0, settings.updateTicks)){
                    currentZones().forEach(z -> z.update(settings.captureRate));
                    // Time remaining in seconds
                    int time = (int) ((settings.gameDuration - interval.getTime(2)) / Time.toSeconds);
                    Call.setHudText(Strings.format("Time remaining > @:@", time / 60, time % 60));
                }if(interval.get(1, settings.renderTicks)){
                    currentZones().forEach(z -> z.render(settings.updateTicks));
                    // Shows the zone center to the editors
                    editors.each(p -> {
                        currentZones().each(z -> Call.effect(p.con(), Fx.heal, z.x * tilesize, z.y * tilesize, 0, z.getTeam().color));
                    });
                }

                // Ugly way to determine the winner :^(
                if(interval.get(2, settings.gameDuration)){
                    Map<Team,Float> teams = new HashMap<>(state.teams.getActive().size); // using map for the compute method...
                    currentZones().each(zone -> teams.compute(zone.getTeam(), (t, i) -> zone.getPercent() + (i != null ? i : 0)));

                    // Gets the highest captured percent
                    float max = 0F;
                    Team winner = Team.derelict;
                    for(Entry<Team,Float> entry : teams.entrySet()){
                        if(entry.getValue() > max){
                            max = entry.getValue();
                            winner = entry.getKey();
                        }
                    }

                    // Wee
                    Events.fire(new GameOverEvent(winner));
                    Call.sendMessage(Strings.format("Congrats, @ team win!", winner));
                }
            }
        });

        // Reset the zones to their original states and save the settings
        Events.run(GameOverEvent.class, () -> {
            currentZones().each(Zone::reset);
            config.writeString(json.prettyPrint(settings));
        });


        Events.on(PlayerLeave.class, event -> {
            if(editors.contains(event.player)){
                editors.remove(event.player);
            }
        });

        Events.on(TapEvent.class, event -> {
            if(editors.contains(event.player)){
                Zone zone = currentZones().find(z -> z.x == event.tile.x && z.y == event.tile.y);
                if(zone == null){
                    currentZones().add(new Zone(event.tile.x, event.tile.y, settings.zoneRadius));
                }else{
                    currentZones().remove(zone);
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("domination-settings", "<save/load>", "Settings for the Domination plugin...", args -> {
            switch(args[0].toLowerCase()){
                case "save" -> {
                    config.writeString(json.prettyPrint(settings));
                    info("Settings have been successfully saved.");
                }
                case "load" -> {
                    if(state.isGame()){
                        info("You can't just modify the settings in the middle of the game...");
                        return;
                    }

                    settings = json.fromJson(DominationSettings.class, config);
                    info("Settings have been successfully loaded.");
                }
                default -> info("The option '@' is invalid.", args[0].toLowerCase());
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Playerc>register("domination-edit", "<on/off>", "edit the zones.", (args, player) -> {
            if(!player.admin()){
                player.sendMessage("[red]You need to be a chosen one to use this command.");
                return;
            }

            switch(args[0].toLowerCase()){
                case "on" -> {
                    editors.add(player);
                    player.sendMessage("You enabled editor mode, now every click will create/delete a Zone.");
                }
                case "off" -> {
                    editors.remove(player);
                    player.sendMessage("You disabled editor mode, how unfortunate...");
                }
                default -> player.sendMessage(Strings.format("'@' is not a valid option.", args[0]));
            }
        });
    }
}

package fr.xpdustry.domination;

import arc.*;
import arc.files.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.struct.IntFloatMap.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;

import fr.xpdustry.domination.Zone.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;


@SuppressWarnings("unused")
public class DominationPlugin extends Plugin{
    private static DominationSettings settings;
    private static final Json json = new Json();
    private static final Fi config = new Fi(Core.files.external("domination-config.json").absolutePath());

    private static final Interval interval = new Interval(3);
    private static final ObjectSet<Playerc> editors = new ObjectSet<>();

    private static final IntFloatMap leaderboard = new IntFloatMap();
    private static final Seq<Effect> effects = Seq.with(Fx.mine, Fx.mineBig, Fx.mineHuge);

    static {
        json.setOutputType(OutputType.json);
        json.setSerializer(DominationSettings.class, new DominationIO());
    }

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

        Events.run(PlayEvent.class, () -> {
            state.rules.modeName = "[red]Domination";
        });

        Events.run(WorldLoadEvent.class, () -> {
            interval.reset(2, 0); // Reset the timer
        });

        // Main
        Events.run(Trigger.update, () -> {
            if(isActive() && interval.get(0, settings.updateTicks)){
                // Updates the zone internal data
                currentZones().each(z -> z.update(settings));

                // Updates the leaderboard [team -> percent_captured]
                leaderboard.clear(state.teams.active.size + 1);
                currentZones().each(z -> leaderboard.increment(z.getTeam().id, 0, z.getPercent()));
            }

            if(isActive() && interval.get(1, settings.gameDuration)){
                Entry winner = new Entry();
                for(Entry entry : leaderboard.entries()){
                    if(entry.value > winner.value){
                        winner = entry;
                    }
                }

                Events.fire(new GameOverEvent(Team.get(winner.key)));
            }

            if(interval.get(2, Time.toSeconds / 6)){
                // HUD text
                StringBuilder builder = new StringBuilder(100);
                // Generate a circle for the zone rendering
                float[] circle = Geometry.regPoly((int)(Mathf.pi * settings.zoneRadius), settings.zoneRadius);

                if(isActive()){
                    currentZones().each(z -> {
                        // Render the circle
                        Geometry.iteratePolygon((cx, cy) -> {
                            Call.effect(effects.random(), (cx + z.x) * tilesize, (cy + z.y) * tilesize, 0, z.getTeam().color);
                        }, circle);

                        // Display the percent in the circles
                        String percent = Strings.format("[#@]@%", z.getTeam().color, Strings.fixed(z.getPercent(), 0));
                        Call.label(percent, 1.0F / 6, z.x * tilesize, z.y * tilesize);
                    });

                    // Time remaining
                    int time = (int) ((settings.gameDuration - interval.getTime(1)) / Time.toSeconds);
                    builder.append(Strings.format("Time remaining > @\n", Strings.formatMillis(time * 1000L)));

                    // Leaderboard
                    var iterator = leaderboard.entries().iterator();
                    while(iterator.hasNext()){
                        var entry = iterator.next();
                        var team = Team.get(entry.key);
                        if(team != Team.derelict){
                            builder.append(Strings.format("[#@]@[] > @%", team.color, team.name, (int)entry.value));
                            if(iterator.hasNext()) builder.append('\n');
                        }
                    }

                    // Unclaimed zones
                    if(leaderboard.containsKey(Team.derelict.id)){
                        builder.append(Strings.format("\n[#@]Unclaimed[] > @%", Team.derelict.color, (int)leaderboard.get(Team.derelict.id)));
                    }

                    Call.setHudText(builder.toString());
                }

                // Rendering for editors
                editors.each(p -> {
                    currentZones().each(z -> {
                        Call.effect(p.con(), Fx.unitLand, z.x * tilesize, z.y * tilesize, 0, z.getTeam().color);

                        if(!state.rules.pvp && state.isGame()){
                            // Render the circle
                            Geometry.iteratePolygon((cx, cy) -> {
                                Call.effect(p.con(), effects.random(), (cx + z.x) * tilesize, (cy + z.y) * tilesize, 0, z.getTeam().color);
                            }, circle);
                        }
                    });
                });
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
                    currentZones().add(new Zone(event.tile.x, event.tile.y));
                }else{
                    currentZones().remove(zone);
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("domination-config", "<save/load>", "Settings for the Domination plugin...", args -> {
            switch(args[0].toLowerCase()){
                case "save" -> {
                    config.writeString(json.prettyPrint(settings));
                    info("Settings have been successfully saved.");
                }
                case "load" -> {
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

    public static boolean isActive(){
        return state.rules.pvp && !state.isMenu();
    }

    static class DominationSettings{
        public int zoneRadius = 5;
        public float captureRate = 10F;
        public float updateTicks = Time.toSeconds;
        public float gameDuration = Time.toMinutes * 5;
        public final ObjectMap<String, Seq<Zone>> maps = new ObjectMap<>();
    }
}
